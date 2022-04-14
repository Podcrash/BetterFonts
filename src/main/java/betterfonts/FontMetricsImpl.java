/*
 * Minecraft OpenType Font Support Mod
 *
 * Copyright (C) 2021-2022 Podcrash Ltd
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

import java.util.ArrayList;
import java.util.List;

class FontMetricsImpl implements FontMetrics
{
    /** Cache used to lookup which fonts to use for rendering */
    protected final FontCache fontCache;

    /** Cache used to save layed out glyphs to be subsequently reused */
    protected final StringCache stringCache;

    /** Caches needed for creating GlyphVectors and retrieving glyph texture coordinates. */
    protected final GlyphCaches glyphCaches;

    public FontMetricsImpl(List<? extends Font> fonts)
    {
        this(new NoContextOglService(), fonts);
    }

    protected FontMetricsImpl(FontRenderContext fontRenderContext, List<? extends Font> fonts)
    {
        this.glyphCaches = new GlyphCaches(
                fontRenderContext,
                fonts.stream().anyMatch(OpenTypeFont.class::isInstance),
                fonts.stream().anyMatch(BitmapAsciiFont.class::isInstance),
                fonts.stream().anyMatch(BitmapUnifont.class::isInstance));
        this.fontCache = new FontCache(fonts);
        this.stringCache = new StringCache(fontRenderContext, fontCache, glyphCaches);
    }

    protected void invalidate()
    {
        glyphCaches.invalidate();
        /* Make sure to invalidate it after the GlyphCache */
        stringCache.invalidate();
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
}
