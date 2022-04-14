/*
 * Minecraft OpenType Font Support Mod
 *
 * Copyright (C) 2021-2022 Podcrash Ltd
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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class FontCache
{
    /**
     * A list of all fonts that can be returned by lookupFont(), and that will always be searched for a usable font before.
     * This list will only have plain variation of a font, unlike fontCache which could have multiple entries
     * for the various styles (i.e. bold, italic, etc.) of a font.
     */
    private final List<BetterFontInternal> fonts;
    private final List<BetterFont> unmodifiableFonts;

    public FontCache(List<? extends BetterFont> fonts)
    {
        if(fonts.isEmpty())
            throw new UnsupportedOperationException("The FontRenderer needs at least 1 font");
        this.fonts = fonts.stream()
                .sequential()
                .map(BetterFontInternal::cast)
                .collect(Collectors.toList());
        this.unmodifiableFonts = Collections.unmodifiableList(this.fonts);
    }

    /**
     * Find the first font in the system able to render at least one character from a given string. The function always tries searching first
     * in the fontCache (based on the request style). Failing that, it searches the usedFonts list followed by the allFonts[] array.
     *
     * @param text the string to check against the font
     * @param start the offset into text at which to start checking characters for being supported by a font
     * @param limitPtr the (offset + length) at which to stop checking characters.
     *                 This will also be set with the latest character this Font should render.
     * @param style a combination of the Font.PLAIN, Font.BOLD, and Font.ITALIC to request a particular font style
     * @return an OpenType font capable of displaying at least the first character at the start position in text
     */
    public BetterFontInternal lookupFont(char[] text, int start, AtomicInteger limitPtr, int style)
    {
        for(BetterFontInternal font : fonts)
        {
            final int newLimit = font.canDisplayUpTo(text, start, limitPtr.get());
            /* Only use the font if it can layout alla characters (-1) or at least the first character of the requested string range */
            if(newLimit == -1 || newLimit != start)
            {
                /* Set the last characters this font can render */
                limitPtr.getAndUpdate(l -> newLimit != -1 ? newLimit : l);
                /* Return a font instance of the proper style; usedFonts has only plain style fonts */
                return font.deriveFont(style);
            }
            /*
             * This font can't display the first character, but maybe it can display some subsequent ones
             * We want to respect the user defined order, so set that as the new limit to use for subsequent fonts.
             */
            limitPtr.getAndUpdate(l -> {
                final int newL = font.canDisplayFrom(text, start, l);
                /* canDisplayFrom returns -1 if the font cannot display any character */
                return newL == -1 ? l : Math.min(l, newL);
            });
        }

        /* If no supported fonts found, use the default one (first in usedFonts) so it can draw its unknown character glyphs */
        final BetterFontInternal font = fonts.get(0);
        /* Return a font instance of the proper point size and style; usedFonts only 1pt sized plain style fonts */
        return font.deriveFont(style);
    }

    public List<BetterFont> getFonts()
    {
        return unmodifiableFonts;
    }
}
