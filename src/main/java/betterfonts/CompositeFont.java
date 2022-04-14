/*
 * Minecraft OpenType Font Support Mod
 *
 * Copyright (C) 2022 Podcrash Ltd
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

import java.util.*;

class CompositeFont extends BaseFontDescriptor implements BetterFontDescriptor
{

    private final List<BetterFontInternal> fonts;
    private final List<BetterFontInternal> unmodifiableFonts;

    public CompositeFont()
    {
        this.fonts = new ArrayList<>();
        this.unmodifiableFonts = Collections.unmodifiableList(fonts);
    }

    public CompositeFont(BetterFont font)
    {
        this.fonts = new ArrayList<>();
        addFont(font);
        this.unmodifiableFonts = Collections.unmodifiableList(fonts);
    }

    public CompositeFont(BetterFont... fonts)
    {
        this.fonts = new ArrayList<>();
        addFonts(fonts);
        this.unmodifiableFonts = Collections.unmodifiableList(this.fonts);
    }

    public CompositeFont(Collection<? extends BetterFont> fonts)
    {
        this.fonts = new ArrayList<>();
        addFonts(fonts);
        this.unmodifiableFonts = Collections.unmodifiableList(this.fonts);
    }

    @Override
    public List<BetterFontInternal> getFonts()
    {
        return unmodifiableFonts;
    }

    void addFont(BetterFontDescriptor font)
    {
        if(font instanceof BetterFont)
        {
            fonts.add(BetterFontInternal.cast((BetterFont) font));
            return;
        }

        addFonts(font.getFonts());
    }

    void addFonts(BetterFont... fonts)
    {
        Arrays.stream(fonts).forEach(this::addFont);
    }

    void addFonts(Collection<? extends BetterFont> fonts)
    {
        fonts.forEach(this::addFont);
    }
}
