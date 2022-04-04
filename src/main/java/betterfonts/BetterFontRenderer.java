/*
 * Minecraft OpenType Font Support Mod
 *
 * Copyright (C) 2021 Podcrash Ltd
 * Copyright (C) 2018 Jittapan Pluemsumran <https://github.com/secretdataz>
 * Copyright (C) 2017 cubex2 <https://github.com/cubex2>
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package betterfonts;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

import java.awt.geom.Rectangle2D;
import java.util.*;

class BetterFontRenderer implements Constants, IBetterFontRenderer {
    /** Offset from the string's baseline as which to draw the underline (in pixels) */
    private static final float UNDERLINE_OFFSET = 1 * MINECRAFT_SCALE_FACTOR;

    /** Thickness of the underline (in pixels) */
    private static final float UNDERLINE_THICKNESS = 2 * MINECRAFT_SCALE_FACTOR;

    /** Offset from the string's baseline as which to draw the strikethrough line (in pixels) */
    private static final float STRIKETHROUGH_OFFSET = -6 * MINECRAFT_SCALE_FACTOR;

    /** Thickness of the strikethrough line (in pixels) */
    private static final float STRIKETHROUGH_THICKNESS = 2 * MINECRAFT_SCALE_FACTOR;

    /* Characters that are supported by the RANDOM render style. This is pre-sorted to allow being binarySearched */
    private static final char[] RANDOM_STYLE_CHARS;
    static
    {
        final char[] unsorted = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000".toCharArray();
        Arrays.sort(unsorted); // This is probably already sorted, but just to make 100% sure, I'm gonna manually sort it
        RANDOM_STYLE_CHARS = unsorted;
    }

    /** Service used to make OpenGL calls */
    private final OglService oglService;

    /** Cache used to lookup which fonts to use for rendering */
    private final FontCache fontCache;

    /** Cache used to save layed out glyphs to be subsequently reused */
    private final StringCache stringCache;

    /** Caches needed for creating GlyphVectors and retrieving glyph texture coordinates. */
    private final GlyphCaches glyphCaches;

    /**
     * Color codes from original FontRender class. First 16 entries are the primary chat colors; second 16 are darker versions
     * used for drop shadows.
     */
    private final int[] colorTable;

    /** Random used for the random style in {@link #renderString(String, float, float, int, boolean)} */
    private final Random fontRandom = new Random();

    private int cachedTexMinFilter, cachedTexMagFilter;
    private float cachedTexMaxLevel, cachedTexLodBias;

    /** If true, then enable GL_BLEND in renderString() so anti-aliasing font glyphs show up properly. */
    private boolean antiAliasEnabled;

    /**
     * A single BetterFontRenderer object is allocated by Minecraft's FontRenderer which forwards all string drawing and requests for
     * string width to this class.
     *
     * @param colors 32 element array of RGBA colors corresponding to the 16 text color codes followed by 16 darker version of the
     * color codes for use as drop shadows
     */
    BetterFontRenderer(OglService oglService, int[] colors, List<FontInternal> fonts, boolean antiAlias)
    {
        this.oglService = oglService;
        this.colorTable = colors;
        this.glyphCaches = new GlyphCaches(oglService,
                fonts.stream().anyMatch(OpenTypeFont.class::isInstance),
                fonts.stream().anyMatch(BitmapAsciiFont.class::isInstance),
                fonts.stream().anyMatch(BitmapUnifont.class::isInstance));
        this.fontCache = new FontCache(fonts);
        this.stringCache = new StringCache(oglService, fontCache, glyphCaches);

        setAntiAlias(antiAlias);
    }

    @Override
    public void setAntiAlias(boolean antiAlias)
    {
        this.antiAliasEnabled = antiAlias;
        glyphCaches.setAntiAlias(antiAlias);
        /* Antialiasing changed, invalidate caches */
        invalidate();
    }

    public void invalidate()
    {
        glyphCaches.invalidate();
        /* Make sure to invalidate it after the GlyphCache */
        stringCache.invalidate();
    }

    public List<Font> getFonts() {
        return fontCache.getFonts();
    }

    @Override
    public float drawString(String text, float startX, float startY, int initialColor, boolean dropShadow)
    {
        oglService.glEnable(GL11.GL_ALPHA);

        if(dropShadow)
        {
            float newX;
            newX = renderString(text, startX + 1.0F, startY + 1.0F, adjustColor(initialColor, true), true);
            newX = Math.max(newX, renderString(text, startX, startY, adjustColor(initialColor, false), false));
            return newX;
        }

        return renderString(text, startX, startY, adjustColor(initialColor, false), false);
    }

    /**
     * Applies the same adjustments as Minecraft does at the given color
     *
     * @param initialColor color
     * @param shadowFlag whether the color is used to render the string shadow
     * @return adjusted color
     */
    private int adjustColor(int initialColor, boolean shadowFlag) {
        /* If the alpha is below a certain threshold (<= 3), use the maximum (255) */
        if((initialColor & 0xFC000000) == 0)
            initialColor |= 0xFF000000;
        /* Fix the color to be used for shadows */
        if(shadowFlag)
            initialColor = (initialColor & 0xFCFCFC) >> 2 | initialColor & 0xFF000000;
        return initialColor;
    }

    @Override
    public float renderString(String str, float startX, float startY, int initialColor, boolean shadowFlag)
    {
        /* Check for invalid arguments */
        if(str == null || str.isEmpty())
        {
            return 0;
        }

        /* Fix for vanilla mipmapping
         * Easiest fix to not implement mip-mapping
         * For some reasons if I put it somewhere else it disables mipmapping */
        saveMipmapping();
        disableMipmapping();

        /* Fix for what RenderLivingBase#setBrightness does */
        oglService.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);

        /* Make sure the entire string is cached before rendering and return its glyph representation */
        StringCache.Entry entry = stringCache.cacheString(str);

        /* Translate to the right coords */
        oglService.glTranslatef(startX, startY, 0);

        /* Color currently selected by color code; reapplied to Tessellator instance after glBindTexture() */
        int color = initialColor;

        /* Track which texture is currently bound to minimize the number of glBindTexture() and Tessellator.draw() calls needed */
        int boundTextureName = 0;

        /*
         * This color change will have no effect on the actual text (since colors are included in the Tessellator vertex
         * array), however GuiEditSign of all things depends on having the current color set to white when it renders its
         * "Edit sign message:" text. Otherwise, the sign which is rendered underneath would look too dark.
         */
        oglService.glColor3f((color >> 16 & 0xff) / 255f, (color >> 8 & 0xff) / 255f, (color & 0xff) / 255f);

        /* Save the blending state */
        boolean wasBlendEnabled = oglService.glIsEnabled(GL11.GL_BLEND);
        /* If the GL_BLEND is not enabled, ignore the alpha value. This fixes the problem with the scoreboard */
        if(!wasBlendEnabled)
            initialColor = 0xFF000000 | initialColor;

        /*
         * Enable GL_BLEND in case the font is drawn anti-aliased because Minecraft itself only enables blending for chat text
         * (so it can fade out), but not GUI text or signs. Minecraft uses multiple blend functions so it has to be specified here
         * as well for consistent blending.
         */
        if(antiAliasEnabled)
        {
            oglService.glEnable(GL11.GL_BLEND);
            oglService.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        /* Using the Tessellator to queue up data in a vertex array and then draw all at once should be faster than immediate mode */
        OglService.Tessellator tessellator = oglService.tessellator();
        tessellator.startDrawingQuadsWithUV();
        tessellator.setColorRGBA(color);

        for(int glyphIndex = 0, colorIndex = 0; glyphIndex < entry.sortedGlyphs.length; glyphIndex++)
        {
            /* Select the current glyph's texture information and horizontal layout position within this string */
            Glyph glyph = entry.sortedGlyphs[glyphIndex];
            GlyphTexture texture = glyph.texture;
            float glyphX = glyph.x;

            /*
             * Apply the latest color code found before this glyph.
             * Note that only the RGB component of the color is replaced by a color code; the alpha component
             * of the original color passed into this function will remain.
             */
            color = applyColorCode(tessellator, glyph.colorCode, initialColor, shadowFlag);
            /* The currently active font style is needed to select the proper ASCII digit style for fast replacement */
            int fontStyle = glyph.fontStyle;
            /* The currently active render style is needed to replace the character with a random one if randomStyle is active */
            int renderStyle = glyph.renderStyle;

            /*
             * Replace ASCII digits in the string with their respective glyphs; strings differing by digits are only cached once.
             * If the new replacement glyph has a different width than the original placeholder glyph (e.g. the '1' glyph is often
             * narrower than other digits), re-center the new glyph over the placeholder's position to minimize the visual impact
             * of the width mismatch.
             */
            char c = str.charAt(glyph.stringIndex);
            if(c >= '0' && c <= '9')
            {
                int oldWidth = Math.round(texture.width * glyph.textureScale);
                texture = stringCache.digitGlyphs[fontStyle][(c - '0')].texture;

                /* In case it failed the first time */
                if(texture == null)
                {
                    stringCache.cacheDigitGlyphs();
                    texture = stringCache.digitGlyphs[fontStyle][(c - '0')].texture;
                }

                int newWidth = Math.round(texture.width * glyph.textureScale);
                glyphX += (oldWidth - newWidth) >> 1;
            }

            /* Replace character with random one */
            if((renderStyle & StringCache.ColorCode.RANDOM) != 0 && Arrays.binarySearch(RANDOM_STYLE_CHARS, c) >= 0)
            {
                // TODO: probably shouldn't cache calls to getStringWidth() and the subsequent cacheString()
                int oldCharWidth = Math.round(getStringWidth(String.valueOf(c)));

                char newC;
                do {
                    int charPos = fontRandom.nextInt(RANDOM_STYLE_CHARS.length);
                    newC = RANDOM_STYLE_CHARS[charPos];
                }
                while(oldCharWidth == Math.round(getStringWidth(String.valueOf(newC))));

                texture = stringCache.cacheString(String.valueOf(newC)).sortedGlyphs[0].texture;
            }

            /*
             * Make sure the OpenGL texture storing this glyph's image is bound (if not already bound). All pending glyphs in the
             * Tessellator vertex array must be drawn before switching textures, otherwise they would erroneously use the new
             * texture as well.
             */
            if(boundTextureName != texture.textureName)
            {
                tessellator.draw();
                tessellator.startDrawingQuadsWithUV();
                tessellator.setColorRGBA(color >> 16 & 0xff, color >> 8 & 0xff, color & 0xff, color >> 24 & 0xff);

                restoreMipmapping(); // If I don't do this mipmapping gets completely disabled

                oglService.glBindTexture(GL11.GL_TEXTURE_2D, texture.textureName);
                boundTextureName = texture.textureName;

                disableMipmapping(); // Re-disable it
            }

            float x1 = glyphX;
            float x2 = glyphX + texture.width * glyph.textureScale;
            /* Adjust the baseline of the string because the startY coordinate in Minecraft is for the top of the string */
            float y1 = glyph.y + glyph.ascent;
            float y2 = glyph.y + glyph.ascent + texture.height * glyph.textureScale;

            tessellator.addVertexWithUV(x1, y1, 0, texture.u1, texture.v1);
            tessellator.addVertexWithUV(x1, y2, 0, texture.u1, texture.v2);
            tessellator.addVertexWithUV(x2, y2, 0, texture.u2, texture.v2);
            tessellator.addVertexWithUV(x2, y1, 0, texture.u2, texture.v1);
        }

        /* Draw any remaining glyphs in the Tessellator vertex array (there should be at least one glyph pending) */
        tessellator.draw();

        /* Draw strikethrough and underlines if the string uses them anywhere */
        if(entry.specialRender)
        {
            int renderStyle = 0;

            /* Use initial color passed to renderString(); disable texturing to draw solid color lines */
            color = initialColor;
            oglService.glDisable(GL11.GL_TEXTURE_2D);
            tessellator.startDrawingQuads();
            tessellator.setColorRGBA(color >> 16 & 0xff, color >> 8 & 0xff, color & 0xff, color >> 24 & 0xff);

            boolean isUnderlining = false, isStrikeThrough = false;
            for(int glyphIndex = 0, colorIndex = 0; glyphIndex < entry.glyphs.length; glyphIndex++)
            {
                /*
                 * If the original string had a color code at this glyph's position, then change the current GL color that gets added
                 * to the vertex array. The while loop handles multiple consecutive color codes, in which case only the last such
                 * color code takes effect.
                 */
                while(colorIndex < entry.colors.length && entry.glyphs[glyphIndex].stringIndex >= entry.colors[colorIndex].stringIndex)
                {
                    if(!isUnderlining)
                        color = applyColorCode(tessellator, entry.colors[colorIndex].colorCode, initialColor, shadowFlag);
                    renderStyle = entry.colors[colorIndex].renderStyle;
                    colorIndex++;
                }

                /* Select the current glyph within this string for its layout position */
                Glyph glyph = entry.glyphs[glyphIndex];
                boolean isLastGlyph = glyphIndex == entry.glyphs.length - 1;

                /*
                 * Draw underline/strikethrough on glyph if the style is enabled
                 * The baseline of the string needs to be adjusted because the startY coordinate in Minecraft is for the top of the string
                 */
                isUnderlining = drawLineOverGlyphs(tessellator, glyph, isLastGlyph,
                        (renderStyle & StringCache.ColorCode.UNDERLINE) != 0, isUnderlining,
                        entry.ascent + UNDERLINE_OFFSET, UNDERLINE_THICKNESS);
                isStrikeThrough = drawLineOverGlyphs(tessellator, glyph, isLastGlyph,
                        (renderStyle & StringCache.ColorCode.STRIKETHROUGH) != 0, isStrikeThrough,
                        entry.ascent + STRIKETHROUGH_OFFSET, STRIKETHROUGH_THICKNESS);
            }

            /* Finish drawing the last strikethrough/underline segments */
            tessellator.draw();
            oglService.glEnable(GL11.GL_TEXTURE_2D);
        }

        if(antiAliasEnabled && !wasBlendEnabled)
            oglService.glDisable(GL11.GL_BLEND);

        oglService.glTranslatef(-startX, -startY, 0);

        restoreMipmapping();

        /* Return total horizontal advance (slightly wider than the bounding box, but close enough for centering strings) */
        return entry.advance;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private boolean drawLineOverGlyphs(OglService.Tessellator tessellator,
                                       Glyph glyph,
                                       boolean isLastGlyph,
                                       boolean shouldDrawLine,
                                       boolean isAlreadyDrawingLine,
                                       float offset, float thickness) {
        /* The strike/underlines are drawn beyond the glyph's width to include the extra space between glyphs */
        float glyphSpace = glyph.advance - glyph.texture.width * glyph.textureScale;

        if(shouldDrawLine)
        {
            float x1 = glyph.x - glyphSpace;
            float x2 = glyph.x + glyph.advance;
            float y1 = offset;
            float y2 = offset + thickness;

            if(!isAlreadyDrawingLine)
            {
                tessellator.addVertex(x1, y1, 0);
                tessellator.addVertex(x1, y2, 0);
                return true;
            }
            else if(isLastGlyph)
            {
                tessellator.addVertex(x2, y2, 0);
                tessellator.addVertex(x2, y1, 0);
                return false;
            }
        }
        else if(isAlreadyDrawingLine)
        {
            float x1 = glyph.x;
            float y1 = offset;
            float y2 = offset + thickness;

            tessellator.addVertex(x1, y2, 0);
            tessellator.addVertex(x1, y1, 0);
            return false;
        }

        return isAlreadyDrawingLine;
    }

    @Override
    public float getStringWidth(String str)
    {
        /* Check for invalid arguments */
        if(str == null || str.isEmpty())
        {
            return 0;
        }

        /* Make sure the entire string is cached and rendered since it will probably be used again in a renderString() call */
        StringCache.Entry entry = stringCache.cacheString(str);

        /* Return total horizontal advance (slightly wider than the bounding box, but close enough for centering strings) */
        return entry.advance;
    }

    @Override
    public float getCharWidth(char character)
    {
        return getStringWidth(String.valueOf(character));
    }

    @Override
    public float getStringHeight(String str)
    {
        /* Check for invalid arguments */
        if(str == null || str.isEmpty())
        {
            return 0;
        }

        /* Make sure the entire string is cached and rendered since it will probably be used again in a renderString() call */
        StringCache.Entry entry = stringCache.cacheString(str);

        /* Return total horizontal advance (slightly wider than the bounding box, but close enough for centering strings) */
        return entry.height;
    }

    @Override
    public float getStringBaseline(String str)
    {
        /* Check for invalid arguments */
        if(str == null || str.isEmpty())
        {
            return 0;
        }

        /* Make sure the entire string is cached and rendered since it will probably be used again in a renderString() call */
        StringCache.Entry entry = stringCache.cacheString(str);

        /* Return total horizontal advance (slightly wider than the bounding box, but close enough for centering strings) */
        return entry.ascent;
    }

    @Override
    public Rectangle2D.Float getStringVisualBounds(String str)
    {
        return getStringVisualBounds(str, new Rectangle2D.Float());
    }

    @Override
    public Rectangle2D.Float getStringVisualBounds(String str, Rectangle2D.Float rectangle)
    {
        /* Check for invalid arguments */
        if(str == null || str.isEmpty())
        {
            rectangle.x = rectangle.y = rectangle.width = rectangle.height = 0;
            return rectangle;
        }

        /* Make sure the entire string is cached and rendered since it will probably be used again in a renderString() call */
        StringCache.Entry entry = stringCache.cacheString(str);

        float minX = 0, maxX = 0;
        float minY = 0, maxY = 0;

        for(int glyphIndex = 0; glyphIndex < entry.glyphs.length; glyphIndex++)
        {
            /* Select the current glyph's horizontal layout position within this string */
            Glyph glyph = entry.glyphs[glyphIndex];

            final float x1 = glyph.x;
            final float x2 = glyph.x + glyph.texture.width * glyph.textureScale;
            final float y1 = glyph.y + glyph.ascent;
            final float y2 = glyph.y + glyph.ascent + glyph.texture.height * glyph.textureScale;

            /* Find the minimum and maximum coordinates */
            minX = glyphIndex == 0 ? x1 : Math.min(minX, x1);
            maxX = glyphIndex == 0 ? x2 : Math.max(maxX, x2);
            minY = glyphIndex == 0 ? y1 : Math.min(minY, y1);
            maxY = glyphIndex == 0 ? y2 : Math.max(maxY, y2);
        }

        rectangle.x = minX;
        rectangle.y = minY;
        rectangle.width = maxX - minX;
        rectangle.height = maxY - minY;
        return rectangle;
    }

    @Override
    public int sizeString(String str, float width, boolean breakAtSpaces)
    {
        /* Check for invalid arguments */
        if(str == null || str.isEmpty())
        {
            return 0;
        }

        /* The glyph array for a string is sorted by the string's logical character position */
        Glyph[] glyphs = stringCache.cacheString(str).glyphs;

        /* Index of the last whitespace found in the string; used if breakAtSpaces is true */
        int wsIndex = -1;

        /* Add up the individual advance of each glyph until it exceeds the specified width */
        float advance = 0;
        int index = 0;
        while(index < glyphs.length && advance <= width)
        {
            /* Keep track of spaces if breakAtSpaces it set */
            if(breakAtSpaces)
            {
                char c = str.charAt(glyphs[index].stringIndex);
                if(c == ' ')
                {
                    wsIndex = index;
                }
                else if(c == '\n')
                {
                    wsIndex = index;
                    break;
                }
            }

            float nextAdvance = advance + glyphs[index].advance;
            if(nextAdvance > width) // Prevents returning an additional char
                break;

            advance = nextAdvance;
            index++;
        }

        /* Avoid splitting individual words if breakAtSpaces set; same test condition as in Minecraft's FontRenderer */
        if(index < glyphs.length && wsIndex != -1 && wsIndex < index)
        {
            index = wsIndex;
        }

        /* The string index of the last glyph that wouldn't fit gives the total desired length of the string in characters */
        return index < glyphs.length ? glyphs[index].stringIndex : str.length();
    }

    @Override
    public int sizeStringToWidth(String str, float width)
    {
        return sizeString(str, width, true);
    }

    @Override
    public String trimStringToWidth(String str, float width)
    {
        return trimStringToWidth(str, width, false);
    }

    @Override
    public String trimStringToWidth(String str, float width, boolean reverse)
    {
        if (reverse)
            str = new StringBuilder(str).reverse().toString();

        int length = sizeString(str, width, false);
        str = str.substring(0, length);

        if(reverse)
        {
            str = (new StringBuilder(str)).reverse().toString();
        }

        return str;
    }

    @Override
    public List<String> listFormattedStringToWidth(String str, float wrapWidth)
    {
        final List<String> lines = new ArrayList<>();

        String remaining = str;
        do
        {
            final int lineEnd = sizeStringToWidth(remaining, wrapWidth);

            final String line = remaining.substring(0, lineEnd);
            lines.add(line);

            final boolean isWhitespace = lineEnd < remaining.length() &&
                    (remaining.charAt(lineEnd) == ' ' || remaining.charAt(lineEnd) == '\n');
            remaining = getFormatFromString(remaining) + remaining.substring(lineEnd + (isWhitespace ? 1 : 0));
        }
        while(!remaining.isEmpty());

        return lines;
    }

    private String getFormatFromString(String str)
    {
        final StringBuilder sb = new StringBuilder();
        int start = 0, next;

        /* Search for section mark characters indicating the start of a color code (but only if followed by at least one character) */
        while((next = str.indexOf('\u00A7', start)) != -1 && next + 1 < str.length())
        {
            char ch = str.charAt(next + 1);
            int code = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(ch));

            if(code != -1) // isColor || isFormatSpecial
            {
                if(code <= 15) // isColor
                    sb.setLength(0); // This may be a bug in Minecraft's original FontRenderer
                sb.append('\u00A7').append(ch);
            }

            /* Resume search for section marks after skipping this one */
            start = next + 2;
        }

        return sb.toString();
    }

    /**
     * Apply a new vertex color to the Tessellator instance based on the numeric chat color code. Only the RGB component of the
     * color is replaced by a color code; the alpha component of the original default color will remain.
     *
     * @param colorCode the chat color code as a number 0-15 or -1 to reset the default color
     * @param color the default color used when the colorCode is -1
     * @param shadowFlag ir true, the color code will select a darker version of the color suitable for drop shadows
     * @return the new RGBA color set by this function
     */
    private int applyColorCode(OglService.Tessellator tessellator, int colorCode, int color, boolean shadowFlag)
    {
        /* A -1 color code indicates a reset to the initial color passed into renderString() */
        if(colorCode != -1)
        {
            colorCode = shadowFlag ? colorCode + 16 : colorCode;
            color = colorTable[colorCode] & 0xffffff | color & 0xff000000;
        }

        tessellator.setColorRGBA(color);
        return color;
    }

    /** Save current mipmapping values */
    private void saveMipmapping()
    {
        cachedTexMinFilter = oglService.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
        cachedTexMagFilter = oglService.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
        cachedTexMaxLevel = oglService.glGetTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL);
        cachedTexLodBias = oglService.glGetTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS);
    }

    /** Disables mipmapping to render strings cause it's not properly implemented */
    private void disableMipmapping()
    {
        oglService.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        oglService.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        oglService.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        oglService.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0);
    }

    /** Reset mipmapping to the saved values */
    private void restoreMipmapping()
    {
        oglService.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, cachedTexMinFilter);
        oglService.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, cachedTexMagFilter);
        oglService.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, cachedTexMaxLevel);
        oglService.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, cachedTexLodBias);
    }
}
