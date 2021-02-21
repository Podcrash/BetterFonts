/*
 * Minecraft OpenType Font Support Mod
 *
 * Copyright (C) 2012 Wojciech Stryjewski <thvortex@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>..
 */

package betterfonts;

import java.lang.ref.WeakReference;
import java.text.Bidi;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * The StringCache is the public interface for rendering of all Unicode strings using OpenType fonts. It caches the glyph layout
 * of individual strings, and it uses a GlyphCache instance to cache the pre-rendered images for individual glyphs. Once a string
 * and its glyph images are cached, the critical path in renderString() will draw the glyphs as fast as if using a bitmap font.
 * Strings are cached using weak references through a two layer string cache. Strings that are no longer in use by Minecraft will
 * be evicted from the cache, while the pre-rendered images of individual glyphs remains cached forever. The following diagram
 * illustrates how this works:
 *
 * <pre>
 * String passed to            Key object considers      Entry object holds       Each Glyph object      GlyphCache.Entry stores
 * renderString();             all ASCII digits equal    an array of Glyph        belongs to only one    the texture ID, image
 * mapped by weak              to zero ('0');            objects which may        Entry object; it has   width/height and
 * weakRefCache                mapped by weak            not directly             the glyph x/y pos      normalized texture
 * to Key object               stringCache to Entry      correspond to Unicode    within the string      coordinates.
 *                                                       chars in string
 * String("Fi1") ------------\                                               ---> Glyph("F") ----------> GlyphCache.Entry("F")
 *                    N:1     \                1:1                     1:N  /                    N:1
 * String("Fi4") ------------> Key("Fi0") -------------> Entry("Fi0") -----+----> Glyph("i") ----------> GlyphCache.Entry("i")
 *                                                                          \                    N:1
 *                                                                           ---> Glyph("0") -----\
 *                                                                                                 ----> GlyphCache.Entry("0")
 *                                                                           ---> Glyph("0") -----/
 *                    N:1                      1:1                     1:N  /                    N:1
 * String("Be1") ------------> Key("Be0") -------------> Entry("Be0") -----+----> Glyph("e") ----------> GlyphCache.Entry("e")
 *                                                                          \                    N:1
 *                                                                           ---> Glyph("B") ----------> GlyphCache.Entry("B")
 * </pre>
 */
class StringCache
{
    /** Service used to make OpenGL calls */
    private final OglService oglService;

    /** Service used to make OpenGL calls */
    private final FontCache fontCache;

    /**
     * A cache of recently seen strings to their fully layed-out state, complete with color changes and texture coordinates of
     * all pre-rendered glyph images needed to display this string. The weakRefCache holds strong references to the Key
     * objects used in this map.
     */
    private final Map<Key, Entry> stringCache = new WeakHashMap<>();

    /**
     * Every String passed to the public renderString() function is added to this WeakHashMap. As long as As long as Minecraft
     * continues to hold a strong reference to the String object (i.e. from TileEntitySign and ChatLine) passed here, the
     * weakRefCache map will continue to hold a strong reference to the Key object that said strings all map to (multiple strings
     * in weakRefCache can map to a single Key if those strings only differ by their ASCII digits).
     */
    private final Map<String, Key> weakRefCache = new WeakHashMap<>();

    /**
     * Temporary Key object re-used for lookups with stringCache.get(). Using a temporary object like this avoids the overhead
     * of allocating new objects in the critical rendering path. Of course, new Key objects are always created when adding
     * a mapping to stringCache.
     */
    private final Key lookupKey = new Key();

    /**
     * Pre-cached glyphs for the ASCII digits 0-9 (in that order). Used by renderString() to substitute digit glyphs on the fly
     * as a performance boost. The speed up is most noticeable on the F3 screen which rapidly displays lots of changing numbers.
     * The 4 element array is index by the font style (combination of Font.PLAIN, Font.BOLD, and Font.ITALIC), and each of the
     * nested elements is index by the digit value 0-9.
     */
    public final Glyph[][] digitGlyphs = new Glyph[4][];

    /** True if digitGlyphs[] has been assigned and cacheString() can begin replacing all digits with '0' in the string. */
    private boolean digitGlyphsReady = false;

    /**
     * Wraps a String and acts as the key into stringCache. The hashCode() and equals() methods consider all ASCII digits
     * to be equal when hashing and comparing Key objects together. Therefore, Strings which only differ in their digits will
     * be all hashed together into the same entry. The renderString() method will then substitute the correct digit glyph on
     * the fly. This special digit handling gives a significant speedup on the F3 debug screen.
     */
    static private class Key
    {
        /**
         * A copy of the String which this Key is indexing. A copy is used to avoid creating a strong reference to the original
         * passed into renderString(). When the original String is no longer needed by Minecraft, it will be garbage collected
         * and the WeakHashMaps in StringCache will allow this Key object and its associated Entry object to be garbage
         * collected as well.
         */
        public String str;

        /**
         * Computes a hash code on str in the same manner as the String class, except all ASCII digits hash as '0'
         *
         * @return the augmented hash code on str
         */
        @Override
        public int hashCode()
        {
            int code = 0, length = str.length();

            /*
             * True if a section mark character was last seen. In this case, if the next character is a digit, it must
             * not be considered equal to any other digit. This forces any string that differs in color codes only to
             * have a separate entry in the StringCache.
             */
            boolean colorCode = false;

            for (int index = 0; index < length; index++)
            {
                char c = str.charAt(index);
                if(c >= '0' && c <= '9' && !colorCode)
                {
                    c = '0';
                }
                code = (code * 31) + c;
                colorCode = (c == '\u00A7');
            }

            return code;
        }

        /**
         * Compare str against another object (specifically, the object's string representation as returned by toString).
         * All ASCII digits are considered equal by this method, as long as they are at the same index within the string.
         *
         * @return true if the strings are the identical, or only differ in their ASCII digits
         */
        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object o)
        {
            /*
             * There seems to be a timing window inside WeakHashMap itself where a null object can be passed to this
             * equals() method. Presumably it happens between computing a hash code for the weakly referenced Key object
             * while it still exists and calling its equals() method after it was garbage collected.
             */
            if(o == null)
            {
                return false;
            }

            /* Calling toString on a String object simply returns itself so no new object allocation is performed */
            String other = o.toString();
            int length = str.length();

            if(length != other.length())
            {
                return false;
            }

            /*
             * True if a section mark character was last seen. In this case, if the next character is a digit, it must
             * not be considered equal to any other digit. This forces any string that differs in color codes only to
             * have a separate entry in the StringCache.
             */
            boolean colorCode = false;

            for(int index = 0; index < length; index++)
            {
                char c1 = str.charAt(index);
                char c2 = other.charAt(index);

                if(c1 != c2 && (c1 < '0' || c1 > '9' || c2 < '0' || c2 > '9' || colorCode))
                {
                    return false;
                }
                colorCode = (c1 == '\u00A7');
            }

            return true;
        }

        /**
         * Returns the contained String object within this Key.
         *
         * @return the str object
         */
        @Override
        public String toString()
        {
            return str;
        }
    }

    /** This entry holds the layed out glyph positions for the cached string along with some relevant metadata. */
    public static class Entry
    {
        /** A weak reference back to the Key object in stringCache that maps to this Entry. */
        public WeakReference<Key> keyRef;

        /** The total horizontal advance (i.e. width) for this string in pixels. */
        public float advance;

        /** The distance from the baseline to the ascender line for this string in pixels. */
        public float ascent;

        /** The height for this string in pixels. */
        public float height;

        /** Array of fully layed out glyphs for the string. Sorted by logical order of characters (i.e. glyph.stringIndex) */
        public Glyph[] glyphs;

        /** Array of fully layed out glyphs for the string. Sorted by texture */
        public Glyph[] sortedGlyphs;

        /** Array of color code locations from the original string */
        public ColorCode[] colors;

        /** True if the string uses strikethrough or underlines anywhere and needs an extra pass in renderString() */
        public boolean specialRender;
    }

    /** Identifies the location and value of a single color code in the original string */
    public static class ColorCode implements Comparable<Integer>
    {
        /** Bit flag used with renderStyle to request the underline style */
        public static final byte UNDERLINE = 1;

        /** Bit flag used with renderStyle to request the strikethrough style */
        public static final byte STRIKETHROUGH = 2;

        /** Bit flag used with renderStyle to request the random style */
        public static final byte RANDOM = 4;

        /** The index into the original string (i.e. with color codes) for the location of this color code. */
        public int stringIndex;

        /** The index into the stripped string (i.e. with no color codes) of where this color code would have appeared */
        public int stripIndex;

        /** The numeric color code (i.e. index into the colorCode[] array); -1 to reset default color */
        public byte colorCode;

        /** Combination of Font.PLAIN, Font.BOLD, and Font.ITALIC specifying font specific styles */
        public byte fontStyle;

        /** Combination of UNDERLINE, STRIKETHROUGH and RANDOM  flags specifying effects performed by renderString() */
        public byte renderStyle;

        /**
         * Performs numeric comparison on stripIndex. Allows binary search on ColorCode arrays in layoutStyle.
         *
         * @param i the Integer object being compared
         * @return either -1, 0, or 1 if this < other, this == other, or this > other
         */
        @Override
        public int compareTo(Integer i)
        {
            return Integer.compare(stringIndex, i);
        }
    }

    public StringCache(OglService oglService, FontCache fontCache)
    {
        this.oglService = oglService;
        this.fontCache = fontCache;

        /* Pre-cache the ASCII digits to allow for fast glyph substitution */
        cacheDigitGlyphs();
    }

    /**
     * Change the default font used to pre-render glyph images. If this method is called at runtime, the string cache is flushed so that
     * all visible strings will be immediately re-layed out using the new font selection.
     */
    public void invalidate()
    {
        /* Clear the string cache so all strings have to be re-layed out and re-rendered */
        weakRefCache.clear();
        stringCache.clear();

        /* Pre-cache the ASCII digits to allow for fast glyph substitution */
        cacheDigitGlyphs();
    }

    /**
     * Pre-cache the ASCII digits to allow for fast glyph substitution. Called once from the constructor and called any time the font selection
     * changes at runtime via setDefaultFont().
     */
    public void cacheDigitGlyphs()
    {
        /* Need to cache each font style combination; the digitGlyphsReady = false disabled the normal glyph substitution mechanism */
        digitGlyphsReady = false;
        digitGlyphs[Font.PLAIN] = cacheString("0123456789").glyphs;
        digitGlyphs[Font.BOLD] = cacheString("\u00A7l0123456789").glyphs; // TODO
        digitGlyphs[Font.ITALIC] = cacheString("\u00A7o0123456789").glyphs;
        digitGlyphs[Font.BOLD | Font.ITALIC] = cacheString("\u00A7l\u00A7o0123456789").glyphs;
        digitGlyphsReady = true;
    }

    /**
     * Add a string to the string cache by perform full layout on it, remembering its glyph positions, and making sure that
     * every font glyph used by the string is pre-rendering. If this string has already been cached, then simply return its
     * existing Entry from the cache. Note that for caching purposes, this method considers two strings to be identical if they
     * only differ in their ASCII digits; the renderString() method performs fast glyph substitution based on the actual digits
     * in the string at the time.
     *
     * @param str this String will be layed out and added to the cache (or looked up, if already cached)
     * @return the string's cache entry containing all the glyph positions
     */
    public Entry cacheString(String str)
    {
        /*
         * New Key object allocated only if the string was not found in the StringCache using lookupKey. This variable must
         * be outside the (entry == null) code block to have a temporary strong reference between the time when the Key is
         * added to stringCache and added to weakRefCache.
         */
        Key key;

        /* Either a newly created Entry object for the string, or the cached Entry if the string is already in the cache */
        Entry entry = null;

        /* Don't perform a cache lookup from other threads because the stringCache is not synchronized */
        if(oglService.isContextCurrent())
        {
            /* Re-use existing lookupKey to avoid allocation overhead on the critical rendering path */
            lookupKey.str = str;

            /* If this string is already in the cache, simply return the cached Entry object */
            entry = stringCache.get(lookupKey);
        }

        /* If string is not cached (or not on main thread) then layout the string */
        if(entry == null)
        {
            /* layoutGlyphVector() requires a char[] so create it here and pass it around to avoid duplication later on */
            char[] text = str.toCharArray();

            /* Strip all color codes from the string */
            entry = new Entry();
            int length = stripColorCodes(entry, str, text);

            /* Layout the entire string, splitting it up by color codes and the Unicode bidirectional algorithm */
            List<Glyph> glyphList = new ArrayList<>();
            entry.advance = layoutBidiString(glyphList, text, 0, length, entry.colors);

            /* Convert the accumulated Glyph list to an array for efficient storage */
            entry.glyphs = new Glyph[glyphList.size()];
            entry.glyphs = glyphList.toArray(entry.glyphs);

            /*
             * Sort Glyph array by stringIndex so it can be compared during rendering to the already sorted ColorCode array.
             * This will apply color codes in the string's logical character order and not the visual order on screen.
             */
            Arrays.sort(entry.glyphs);

            /*
             * Do not actually sort by texture when called from other threads because GlyphCache.cacheGlyphs()
             * will not have been called and the cache entry does not contain any texture data needed for rendering.
             */
            if (oglService.isContextCurrent()) {
                entry.sortedGlyphs = Arrays.copyOf(entry.glyphs, entry.glyphs.length);
                Arrays.sort(entry.sortedGlyphs, Comparator.comparingInt(o -> o.texture.textureName));
            }

            /* Do some post-processing on each Glyph object */
            int colorIndex = 0, shift = 0;
            byte colorCode = -1, fontStyle = Font.PLAIN, renderStyle = 0;
            for(int glyphIndex = 0; glyphIndex < entry.glyphs.length; glyphIndex++)
            {
                Glyph glyph = entry.glyphs[glyphIndex];

                /*
                 * Adjust the string index for each glyph to point into the original string with unstripped color codes. The while
                 * loop is necessary to handle multiple consecutive color codes with no visible glyphs between them. These new adjusted
                 * stringIndex can now be compared against the color stringIndex during rendering. It also allows lookups of ASCII
                 * digits in the original string for fast glyph replacement during rendering.
                 */
                while(colorIndex < entry.colors.length && glyph.stringIndex + shift >= entry.colors[colorIndex].stringIndex)
                {
                    colorCode = entry.colors[colorIndex].colorCode;
                    fontStyle = entry.colors[colorIndex].fontStyle;
                    renderStyle = entry.colors[colorIndex].renderStyle;
                    shift += 2;
                    colorIndex++;
                }

                glyph.colorCode = colorCode;
                glyph.fontStyle = fontStyle;
                glyph.renderStyle = renderStyle;
                glyph.stringIndex += shift;
            }

            /* Find the ascent used by the majority of glyphs */
            entry.ascent = glyphList.stream()
                    .collect(Collectors.toMap(item -> item.ascent, item -> 1, Integer::sum))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse((float) 0);

            /* Find the maximum height of the glyphs */
            entry.height = (float) glyphList.stream()
                    .mapToDouble(glyph -> glyph.height)
                    .max()
                    .orElse(0);

            /*
             * Do not actually cache the string when called from other threads because GlyphCache.cacheGlyphs() will not have been called
             * and the cache entry does not contain any texture data needed for rendering.
             */
            if(oglService.isContextCurrent())
            {
                /* Wrap the string in a Key object (to change how ASCII digits are compared) and cache it along with the newly generated Entry */
                key = new Key();

                /* Make a copy of the original String to avoid creating a strong reference to it */
                //noinspection StringOperationCanBeSimplified
                key.str = new String(str);
                entry.keyRef = new WeakReference<>(key);
                stringCache.put(key, entry);
            }
        }

        /* Do not access weakRefCache from other threads since it is un-synchronized, and for a newly created entry, the keyRef is null */
        if(oglService.isContextCurrent())
        {
            /*
             * Add the String passed into this method to the stringWeakMap so it keeps the Key reference live as long as the String is in use.
             * If an existing Entry was already found in the stringCache, it's possible that its Key has already been garbage collected. The
             * code below checks for this to avoid adding (str, null) entries into weakRefCache. Note that if a new Key object was created, it
             * will still be live because of the strong reference created by the "key" variable.
             */
            Key oldKey = entry.keyRef.get();
            if(oldKey != null)
            {
                weakRefCache.put(str, oldKey);
            }
            lookupKey.str = null;
        }

        /* Return either the existing or the newly created entry so it can be accessed immediately */
        return entry;
    }

    /**
     * Remove all color codes from the string by shifting data in the text[] array over so it overwrites them. The value of each
     * color code and its position (relative to the new stripped text[]) is also recorded in a separate array. The color codes must
     * be removed for a font's context sensitive glyph substitution to work (like Arabic letter middle form).
     *
     * @param str the string from which color codes will be stripped
     * @param text on input it should be an identical copy of str; on output it will be string with all color codes removed
     * @return the length of the new stripped string in text[]; actual text.length will not change because the array is not reallocated
     */
    private int stripColorCodes(Entry cacheEntry, String str, char[] text)
    {
        List<ColorCode> colorList = new ArrayList<>();
        int start = 0, shift = 0, next;

        byte fontStyle = Font.PLAIN;
        byte renderStyle = 0;
        byte colorCode = -1;

        /* Search for section mark characters indicating the start of a color code (but only if followed by at least one character) */
        while((next = str.indexOf('\u00A7', start)) != -1 && next + 1 < str.length())
        {
            /*
             * Remove the two char color code from text[] by shifting the remaining data in the array over on top of it.
             * The "start" and "next" variables all contain offsets into the original unmodified "str" string. The "shift"
             * variable keeps track of how many characters have been stripped so far, and it's used to compute offsets into
             * the text[] array based on the start/next offsets in the original string.
             */
            System.arraycopy(text, next - shift + 2, text, next - shift, text.length - next - 2);

            /* Decode escape code used in the string and change current font style / color based on it */
            int code = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(str.charAt(next + 1)));
            switch(code)
            {
                case 16:
                    renderStyle |= ColorCode.RANDOM;
                    break;

                /* Bold style */
                case 17:
                    fontStyle |= Font.BOLD;
                    break;

                /* Strikethrough style */
                case 18:
                    renderStyle |= ColorCode.STRIKETHROUGH;
                    cacheEntry.specialRender = true;
                    break;

                /* Underline style */
                case 19:
                    renderStyle |= ColorCode.UNDERLINE;
                    cacheEntry.specialRender = true;
                    break;

                /* Italic style */
                case 20:
                    fontStyle |= Font.ITALIC;
                    break;

                /* Plain style */
                case 21:
                    fontStyle = Font.PLAIN;
                    renderStyle = 0;
                    colorCode = -1; // This may be a bug in Minecraft's original FontRenderer
                    break;

                /* Otherwise, must be a color code or some other unsupported code */
                default:
                    if(code >= 0 /* && code <= 15 */)
                    {
                        colorCode = (byte) code;
                        fontStyle = Font.PLAIN; // This may be a bug in Minecraft's original FontRenderer
                        renderStyle = 0;        // This may be a bug in Minecraft's original FontRenderer
                    }
                    break;
            }

            /* Create a new ColorCode object that tracks the position of the code in the original string */
            ColorCode entry = new ColorCode();
            entry.stringIndex = next;
            entry.stripIndex = next - shift;
            entry.colorCode = colorCode;
            entry.fontStyle = fontStyle;
            entry.renderStyle = renderStyle;
            colorList.add(entry);

            /* Resume search for section marks after skipping this one */
            start = next + 2;
            shift += 2;
        }

        /* Convert the accumulated ColorCode list to an array for efficient storage */
        cacheEntry.colors = new ColorCode[colorList.size()];
        cacheEntry.colors = colorList.toArray(cacheEntry.colors);

        /* Return the new length of the string after all color codes were removed */
        return text.length - shift;
    }

    /**
     * Split a string into contiguous LTR or RTL sections by applying the Unicode Bidirectional Algorithm. Calls layoutString()
     * for each contiguous run to perform further analysis.
     *
     * @param glyphList will hold all new Glyph objects allocated by layoutFont()
     * @param text the string to lay out
     * @param start the offset into text at which to start the layout
     * @param limit the (offset + length) at which to stop performing the layout
     * @return the total advance (horizontal distance) of this string
     */
    private float layoutBidiString(List<Glyph> glyphList, char[] text, int start, int limit, ColorCode[] colors)
    {
        float advance = 0;

        /* Avoid performing full bidirectional analysis if text has no "strong" right-to-left characters */
        if(Bidi.requiresBidi(text, start, limit))
        {
            /* Note that while requiresBidi() uses start/limit the Bidi constructor uses start/length */
            Bidi bidi = new Bidi(text, start, null, 0, limit - start, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);

            /* If text is entirely right-to-left, then insert an EntryText node for the entire string */
            if(bidi.isRightToLeft())
            {
                return layoutStyle(glyphList, text, start, limit, FontInternal.LAYOUT_RIGHT_TO_LEFT, advance, colors);
            }

            /* Otherwise text has a mixture of LTR and RLT, and it requires full bidirectional analysis */
            else
            {
                int runCount = bidi.getRunCount();
                byte[] levels = new byte[runCount];
                Integer[] ranges = new Integer[runCount];

                /* Reorder contiguous runs of text into their display order from left to right */
                for(int index = 0; index < runCount; index++)
                {
                    levels[index] = (byte) bidi.getRunLevel(index);
                    ranges[index] = index;
                }
                Bidi.reorderVisually(levels, 0, ranges, 0, runCount);

                /*
                 * Every GlyphVector must be created on a contiguous run of left-to-right or right-to-left text. Keep track of
                 * the horizontal advance between each run of text, so that the glyphs in each run can be assigned a position relative
                 * to the start of the entire string and not just relative to that run.
                 */
                for(int visualIndex = 0; visualIndex < runCount; visualIndex++)
                {
                    int logicalIndex = ranges[visualIndex];

                    /* An odd numbered level indicates right-to-left ordering */
                    int layoutFlag = (bidi.getRunLevel(logicalIndex) & 1) == 1 ? FontInternal.LAYOUT_RIGHT_TO_LEFT : FontInternal.LAYOUT_LEFT_TO_RIGHT;
                    advance = layoutStyle(glyphList, text, start + bidi.getRunStart(logicalIndex), start + bidi.getRunLimit(logicalIndex),
                        layoutFlag, advance, colors);
                }
            }

            return advance;
        }

        /* If text is entirely left-to-right, then insert an EntryText node for the entire string */
        else
        {
            return layoutStyle(glyphList, text, start, limit, FontInternal.LAYOUT_LEFT_TO_RIGHT, advance, colors);
        }
    }

    private float layoutStyle(List<Glyph> glyphList, char[] text, int start, int limit, int layoutFlags, float advance, ColorCode[] colors)
    {
        int currentFontStyle = Font.PLAIN;

        /* Find ColorCode object with stripIndex <= start; that will have the font style in effect at the beginning of this text run */
        int colorIndex = Arrays.binarySearch(colors, start);

        /*
         * If no exact match is found, Arrays.binarySearch() returns (-(insertion point) - 1) where the insertion point is the index
         * of the first ColorCode with a stripIndex > start. In that case, colorIndex is adjusted to select the immediately preceding
         * ColorCode whose stripIndex < start.
         */
        if(colorIndex < 0)
        {
            colorIndex = -colorIndex - 2;
        }

        /* Break up the string into segments, where each segment has the same font style in use */
        while(start < limit)
        {
            int next = limit;

            /* In case of multiple consecutive color codes with the same stripIndex, select the last one which will have active font style */
            while(colorIndex >= 0 && colorIndex < (colors.length - 1) && colors[colorIndex].stripIndex == colors[colorIndex + 1].stripIndex)
            {
                colorIndex++;
            }

            /* If an actual ColorCode object was found (colorIndex within the array), use its fontStyle for layout and render */
            if(colorIndex >= 0 && colorIndex < colors.length)
            {
                currentFontStyle = colors[colorIndex].fontStyle;
            }

            /*
             * Search for the next ColorCode that uses a different fontStyle than the current one. If found, the stripIndex of that
             * new code is the split point where the string must be split into a separately styled segment.
             */
            while(++colorIndex < colors.length)
            {
                if(colors[colorIndex].fontStyle != currentFontStyle)
                {
                    next = colors[colorIndex].stripIndex;
                    break;
                }
            }

            /* Layout the string segment with the style currently selected by the last color code */
            advance = layoutString(glyphList, text, start, next, layoutFlags, advance, currentFontStyle);
            start = next;
        }

        return advance;
    }

    /**
     * Given a string that runs contiguously LTR or RTL, break it up into individual segments based on which fonts can render
     * which characters in the string. Calls layoutFont() for each portion of the string that can be layed out with a single
     * font.
     *
     * @param glyphList will hold all new Glyph objects allocated by layoutFont()
     * @param text the string to lay out
     * @param start the offset into text at which to start the layout
     * @param limit the (offset + length) at which to stop performing the layout
     * @param layoutFlags either Font.LAYOUT_RIGHT_TO_LEFT or Font.LAYOUT_LEFT_TO_RIGHT
     * @param advance the horizontal advance (i.e. X position) returned by previous call to layoutString()
     * @param style combination of Font.PLAIN, Font.BOLD, and Font.ITALIC to select a fonts with some specific style
     * @return the advance (horizontal distance) of this string plus the advance passed in as an argument
     *
     * @todo Correctly handling RTL font selection requires scanning the string from RTL as well.
     */
    private float layoutString(List<Glyph> glyphList, char[] text, int start, int limit, int layoutFlags, float advance, int style)
    {
        /*
         * Convert all digits in the string to a '0' before layout to ensure that any glyphs replaced on the fly will all have
         * the same positions. Under Windows, Java's "SansSerif" logical font uses the "Arial" font for digits, in which the "1"
         * digit is slightly narrower than all other digits. Checking the digitGlyphsReady flag prevents a chicken-and-egg
         * problem where the digit glyphs have to be initially cached and the digitGlyphs[] array initialized without replacing
         * every digit with '0'.
         */
        if(digitGlyphsReady)
        {
            for(int index = start; index < limit; index++)
            {
                if(text[index] >= '0' && text[index] <= '9')
                {
                    text[index] = '0';
                }
            }
        }

        /* Break the string up into segments, where each segment can be displayed using a single font */
        while(start < limit)
        {
            final AtomicInteger limitPtr = new AtomicInteger(limit);
            FontInternal font = fontCache.lookupFont(text, start, limitPtr, style);
            /* limitPtr is updated with the limit at which this Font should stop rendering */
            advance = font.layoutFont(glyphList, text, start, limitPtr.get(), layoutFlags, advance);
            start = limitPtr.get();
        }

        return advance;
    }
}
