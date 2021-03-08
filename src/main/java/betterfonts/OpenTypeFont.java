/*
 * Minecraft OpenType Font Support Mod
 *
 * Copyright (C) 2021 Podcrash Ltd
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

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

class OpenTypeFont implements FontInternal, Constants
{
    /** Cache needed for creating GlyphVectors and retrieving glyph texture coordinates. */
    private OpenTypeGlyphCache glyphCache;

    /** Service used to make OpenGL calls */
    private final OglService oglService;

    /** Actual underlying java Font */
    private final java.awt.Font font;

    private final BetterFontRendererFactory.Baseline baseline;
    private final float customBaseline;
    private Float commonCharsBaseline;

    public OpenTypeFont(OglService oglService,
                        java.awt.Font font,
                        BetterFontRendererFactory.Baseline baseline)
    {
        this(oglService, null, font, Objects.requireNonNull(baseline, "Baseline can't be null"), -1);
    }

    public OpenTypeFont(OglService oglService,
                        java.awt.Font font,
                        float baseline)
    {
        this(oglService, null, font, null, baseline);
    }

    private OpenTypeFont(OglService oglService,
                         OpenTypeGlyphCache glyphCache,
                         java.awt.Font font,
                         BetterFontRendererFactory.Baseline baseline,
                         float customBaseline)
    {
        this.oglService = oglService;
        this.glyphCache = glyphCache;
        this.font = font;
        this.baseline = baseline;
        this.customBaseline = customBaseline;
    }

    private float getCommonCharsBaseline()
    {
        if(commonCharsBaseline == null)
        {
            /* Lay down all the common characters to calculate their positioning in a string */
            final GlyphVector vector = glyphCache.layoutGlyphVector(font, COMMON_CHARS.toCharArray(), 0, COMMON_CHARS.length(), FontInternal.LAYOUT_LEFT_TO_RIGHT);
            /*
             * Find the minimum y value of the laid out string.
             * Since AWT lays out from the baseline, that will be the distance from the baseline, so we just need to invert it
             */
            commonCharsBaseline = (float) -IntStream.range(0, vector.getNumGlyphs())
                    .mapToObj(index -> vector.getGlyphPixelBounds(index, null, 0, 0).getLocation())
                    .mapToDouble(Point::getY)
                    .min()
                    .orElse(0);
        }
        return commonCharsBaseline;
    }

    void setGlyphCache(OpenTypeGlyphCache glyphCache)
    {
        this.glyphCache = glyphCache;
    }

    private void ensureGlyphCache()
    {
        if(glyphCache == null)
            throw new AssertionError("GlyphCache hasn't been set yet");
    }

    @Override
    public String getName()
    {
        return font.getFontName();
    }

    @Override
    public int getSize()
    {
        return font.getSize();
    }

    @Override
    public int getStyle()
    {
        return font.getStyle();
    }

    @Override
    public int canDisplayUpTo(char[] text, int start, int limit)
    {
        return font.canDisplayUpTo(text, start, limit);
    }

    @Override
    public int canDisplayFrom(char[] text, int start, int limit)
    {
        for(int i = start; i < limit; i++)
        {
            char c = text[i];
            if(font.canDisplay(c))
                return i;
            if(Character.isHighSurrogate(c) && font.canDisplay(Character.codePointAt(text, i, limit)))
                return i;
            i++;
        }
        return -1;
    }

    @Override
    public float layoutFont(List<Glyph> glyphList, char[] text, int start, int limit, int layoutFlags, float advance)
    {
        ensureGlyphCache();

        /*
         * Ensure that all glyphs used by the string are pre-rendered and cached in the texture. Only safe to do so from the
         * main thread because cacheGlyphs() can crash LWJGL if it makes OpenGL calls from any other thread. In this case,
         * cacheString() will also not insert the entry into the stringCache since it may be incomplete if lookupGlyph()
         * returns null for any glyphs not yet stored in the glyph cache.
         */
        if(oglService.isContextCurrent())
        {
            glyphCache.cacheGlyphs(font, text, start, limit, layoutFlags);
        }

        /* Creating a GlyphVector takes care of all language specific OpenType glyph substitutions and positioning */
        GlyphVector vector = glyphCache.layoutGlyphVector(font, text, start, limit, layoutFlags);
        LineMetrics lineMetrics = glyphCache.getLineMetrics(font, text, start, limit);

        /*
         * Extract all needed information for each glyph from the GlyphVector so it won't be needed for actual rendering.
         * Note that initially, glyph.start holds the character index into the stripped text array. But after the entire
         * string is layed out, this field will be adjusted on every Glyph object to correctly index the original unstripped
         * string.
         */
        Glyph glyph = null;
        int numGlyphs = vector.getNumGlyphs();
        for(int index = 0; index < numGlyphs; index++) {
            Point position = vector.getGlyphPixelBounds(index, null, advance / MINECRAFT_SCALE_FACTOR, 0).getLocation();

            /* Compute horizontal advance for the previous glyph based on this glyph's position */
            if (glyph != null) {
                glyph.advance = (position.x * MINECRAFT_SCALE_FACTOR) - glyph.x;
            }

            /*
             * Allocate a new glyph object and add to the glyphList. The glyph.stringIndex here is really like stripIndex but
             * it will be corrected later to account for the color codes that have been stripped out.
             */
            glyph = new Glyph();
            glyph.stringIndex = start + vector.getGlyphCharIndex(index);
            glyph.texture = glyphCache.lookupGlyph(font, vector.getGlyphCode(index));
            /* The MINECRAFT_SCALE_FACTOR is needed to align with the scaled GUI coordinate system */
            glyph.textureScale = MINECRAFT_SCALE_FACTOR;
            glyph.x = position.x * MINECRAFT_SCALE_FACTOR;
            glyph.y = position.y * MINECRAFT_SCALE_FACTOR;
            // TODO: both the height and the ascent are actually calculated for the specific substring,
            //       not for the glyph, so they shouldn't be saved in the glyph object
            glyph.height = (lineMetrics.getAscent() + lineMetrics.getDescent()) * MINECRAFT_SCALE_FACTOR;

            if(baseline == null)
            {
                glyph.ascent = customBaseline;
            }
            else
            {
                switch (baseline)
                {
                    // @formatter:off
                    case MINECRAFT: glyph.ascent = MINECRAFT_BASELINE_OFFSET; break;
                    case AWT: glyph.ascent = lineMetrics.getAscent() * MINECRAFT_SCALE_FACTOR; break;
                    case COMMON_CHARS: glyph.ascent = getCommonCharsBaseline() * MINECRAFT_SCALE_FACTOR; break;
                    // @formatter:on
                    default:
                        throw new AssertionError("Unsupported baseline type " + baseline);
                }
            }

            glyphList.add(glyph);
        }

        /* Compute the advance position of the last glyph (or only glyph) since it can't be done by the above loop */
        advance += (vector.getGlyphPosition(numGlyphs).getX() * MINECRAFT_SCALE_FACTOR);
        if(glyph != null)
        {
            glyph.advance = advance - glyph.x;
        }

        /* Return the overall horizontal advance in pixels from the start of string */
        return advance;
    }

    @Override
    public FontInternal deriveFont(int style, float size)
    {
        final java.awt.Font derived = font.getAttributes().get(TextAttribute.WEIGHT) == null && font.getAttributes().get(TextAttribute.POSTURE) == null ?
                font.deriveFont(style, size) :
                // Need to preserve the weight and posture, cause deriveFont(style) overrides it
                font.deriveFont(style, size).deriveFont(font.getAttributes());
        return new OpenTypeFont(oglService, glyphCache, derived, baseline, customBaseline);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpenTypeFont that = (OpenTypeFont) o;
        return font.equals(that.font);
    }

    @Override
    public int hashCode() {
        return Objects.hash(font);
    }

    @Override
    public String toString() {
        return "OpenTypeFont{" +
                "font=" + font +
                '}';
    }
}
