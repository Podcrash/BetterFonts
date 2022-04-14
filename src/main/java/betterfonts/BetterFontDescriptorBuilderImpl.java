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

import java.io.InputStream;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

class BetterFontDescriptorBuilderImpl implements BetterFontDescriptorBuilder
{
    private final FontFactory fontFactory;
    private CompositeFont font = new CompositeFont();

    public BetterFontDescriptorBuilderImpl(FontFactory fontFactory)
    {
        this.fontFactory = fontFactory;
    }

    @Override
    public BetterFontDescriptorBuilder withFont(BetterFontDescriptor font)
    {
        this.font.addFont(font);
        return this;
    }

    @Override
    public BetterFontDescriptorBuilder withFonts(BetterFont... fonts)
    {
        this.font.addFonts(fonts);
        return this;
    }

    @Override
    public BetterFontDescriptorBuilder withFonts(Collection<BetterFont> fonts)
    {
        this.font.addFonts(fonts);
        return this;
    }

    @Override
    public BetterFontDescriptorBuilder useSystemFonts(Function<FontFactory.AwtBuilder<?, ?>, FontFactory.AwtBuilderEnd<?>> openTypeFonts)
    {
        withFonts(fontFactory.createSystemFonts(openTypeFonts));
        return this;
    }

    @Override
    public BetterFontDescriptorBuilder withOpenTypeFont(String name, Function<FontFactory.AwtBuilder<?, ?>, FontFactory.AwtBuilderEnd<?>> openTypeFont)
    {
        withFont(fontFactory.createOpenTypeFont(name, openTypeFont));
        return this;
    }

    @Override
    public BetterFontDescriptorBuilder withOpenTypeFont(Supplier<InputStream> is, Function<FontFactory.AwtBuilder<?, ?>, FontFactory.AwtBuilderEnd<?>> openTypeFont)
    {
        withAwtFont(is, java.awt.Font.TRUETYPE_FONT, openTypeFont);
        return this;
    }

    @Override
    public BetterFontDescriptorBuilder withAwtFont(Supplier<InputStream> is, int fontFormat, Function<FontFactory.AwtBuilder<?, ?>, FontFactory.AwtBuilderEnd<?>> openTypeFont)
    {
        withFont(fontFactory.createAwtFont(is, fontFormat, openTypeFont));
        return this;
    }

    @Override
    public BetterFontDescriptorBuilder withBitmapAsciiFont(String name, Supplier<InputStream> bitmap, int size)
    {
        withFont(fontFactory.createBitmapAsciiFont(name, bitmap, size));
        return this;
    }

    @Override
    public BetterFontDescriptorBuilder withBitmapUnifont(String name, Supplier<InputStream> glyphSizes, IntFunction<InputStream> pageSupplier, int size)
    {
        withFont(fontFactory.createBitmapUnifont(name, glyphSizes, pageSupplier, size));
        return this;
    }

    @Override
    public BetterFontDescriptor build()
    {
        final BetterFontDescriptor built = font;
        this.font = new CompositeFont(built.getFonts());
        return built;
    }
}
