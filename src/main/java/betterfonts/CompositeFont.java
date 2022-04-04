/*
 * Minecraft OpenType Font Support Mod
 *
 * Copyright (C) 2021 Podcrash Ltd
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

class CompositeFont implements FontDescriptor
{

    private final List<FontInternal> fonts;
    private final List<FontInternal> unmodifiableFonts;

    public CompositeFont()
    {
        this.fonts = new ArrayList<>();
        this.unmodifiableFonts = Collections.unmodifiableList(fonts);
    }

    public CompositeFont(Font font)
    {
        this.fonts = new ArrayList<>();
        addFont(font);
        this.unmodifiableFonts = Collections.unmodifiableList(fonts);
    }

    public CompositeFont(Font... fonts)
    {
        this.fonts = new ArrayList<>();
        addFonts(fonts);
        this.unmodifiableFonts = Collections.unmodifiableList(this.fonts);
    }

    public CompositeFont(Collection<Font> fonts)
    {
        this.fonts = new ArrayList<>();
        addFonts(fonts);
        this.unmodifiableFonts = Collections.unmodifiableList(this.fonts);
    }

    @Override
    public List<FontInternal> getFonts()
    {
        return unmodifiableFonts;
    }

    void addFont(FontDescriptor font)
    {
        if(font instanceof Font)
        {
            fonts.add(FontInternal.cast((Font) font));
            return;
        }

        addFonts(font.getFonts());
    }

    void addFonts(Font... fonts)
    {
        Arrays.stream(fonts).forEach(this::addFont);
    }

    void addFonts(Collection<? extends Font> fonts)
    {
        fonts.forEach(this::addFont);
    }
}
