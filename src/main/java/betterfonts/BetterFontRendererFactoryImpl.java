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

import betterfonts.FontFactory.AwtBuilder;
import betterfonts.FontFactory.AwtBuilderEnd;

import java.io.InputStream;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

class BetterFontRendererFactoryImpl implements BetterFontRendererFactory
{
    private final FontFactory fontFactory;
    private final OglService oglService;
    private final int[] colors;

    private CompositeFont font = new CompositeFont();

    public BetterFontRendererFactoryImpl(OglService oglService, int[] colors)
    {
        this(FontFactoryImpl.INSTANCE, oglService, colors);
    }

    BetterFontRendererFactoryImpl(FontFactory fontFactory, OglService oglService, int[] colors)
    {
        this.fontFactory = fontFactory;
        this.oglService = oglService;
        this.colors = colors;
    }

    @Override
    public BetterFontRendererFactory when(boolean condition,
                                          Function<BetterFontRendererFactory, BetterFontRendererFactory> action,
                                          Function<BetterFontRendererFactory, BetterFontRendererFactory> or)
    {
        return condition ? action.apply(this) : or.apply(this);
    }

    @Override
    public BetterFontRendererFactory when(boolean condition, Function<BetterFontRendererFactory, BetterFontRendererFactory> action)
    {
        return condition ? action.apply(this) : this;
    }

    @Override
    public BetterFontRendererFactory withFont(FontDescriptor font)
    {
        this.font.addFont(font);
        return this;
    }

    @Override
    public BetterFontRendererFactory withFont(Font font)
    {
        this.font.addFont(font);
        return this;
    }

    @Override
    public BetterFontRendererFactory withFonts(Font... fonts)
    {
        this.font.addFonts(fonts);
        return this;
    }

    @Override
    public BetterFontRendererFactory withFonts(Collection<Font> fonts)
    {
        this.font.addFonts(fonts);
        return this;
    }

    @Override
    public BetterFontRendererFactory useSystemFonts(Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFonts)
    {
        withFonts(fontFactory.createSystemFonts(openTypeFonts));
        return this;
    }

    @Override
    public BetterFontRendererFactory withOpenTypeFont(String name, Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont)
    {
        withFont(fontFactory.createOpenTypeFont(name, openTypeFont));
        return this;
    }

    @Override
    public BetterFontRendererFactory withOpenTypeFont(Supplier<InputStream> is, Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont)
    {
        withAwtFont(is, java.awt.Font.TRUETYPE_FONT, openTypeFont);
        return this;
    }

    @Override
    public BetterFontRendererFactory withAwtFont(Supplier<InputStream> is, int fontFormat, Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont)
    {
        withFont(fontFactory.createAwtFont(is, fontFormat, openTypeFont));
        return this;
    }

    @Override
    public BetterFontRendererFactory withBitmapAsciiFont(String name, Supplier<InputStream> bitmap, int size)
    {
        withFont(fontFactory.createBitmapAsciiFont(name, bitmap, size));
        return this;
    }

    @Override
    public BetterFontRendererFactory withBitmapUnifont(String name, Supplier<InputStream> glyphSizes, IntFunction<InputStream> pageSupplier, int size)
    {
        withFont(fontFactory.createBitmapUnifont(name, glyphSizes, pageSupplier, size));
        return this;
    }

    @Override
    public BetterFontRenderer build()
    {
        final BetterFontRenderer fontRenderer = new BetterFontRenderer(oglService, colors, font.getFonts(), true);
        font = new CompositeFont();
        return fontRenderer;
    }
}
