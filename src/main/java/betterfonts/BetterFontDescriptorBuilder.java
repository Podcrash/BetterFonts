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

import betterfonts.FontFactory.AwtBuilder;
import betterfonts.FontFactory.AwtBuilderEnd;

import java.io.InputStream;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public interface BetterFontDescriptorBuilder
{

    BetterFontDescriptorBuilder withFont(BetterFontDescriptor font);

    BetterFontDescriptorBuilder withFonts(BetterFont... fonts);

    BetterFontDescriptorBuilder withFonts(Collection<BetterFont> fonts);

    BetterFontDescriptorBuilder useSystemFonts(Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFonts);

    BetterFontDescriptorBuilder withOpenTypeFont(String name, Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont);

    BetterFontDescriptorBuilder withOpenTypeFont(Supplier<InputStream> is, Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont);

    BetterFontDescriptorBuilder withAwtFont(Supplier<InputStream> is, int fontFormat, Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont);

    BetterFontDescriptorBuilder withBitmapAsciiFont(String name, Supplier<InputStream> bitmap, int size);

    BetterFontDescriptorBuilder withBitmapUnifont(String name, Supplier<InputStream> glyphSizes, IntFunction<InputStream> pageSupplier, int size);

    BetterFontDescriptor build();
}
