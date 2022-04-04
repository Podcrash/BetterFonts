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
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public interface Font extends FontDescriptor
{
    /** The plain style constant */
    int PLAIN = java.awt.Font.PLAIN;
    /**
     * The bold style constant.  This can be combined with the other style
     * constants (except PLAIN) for mixed styles.
     */
    int BOLD = java.awt.Font.BOLD;
    /**
     * The italicized style constant.  This can be combined with the other
     * style constants (except PLAIN) for mixed styles.
     */
    int ITALIC = java.awt.Font.ITALIC;

    enum Baseline
    {
        /** Uses the default Minecraft baseline (7 units from the top) */
        MINECRAFT,
        /** Uses the baseline calculated by awt */
        AWT,
        /** Calculates the baseline by laying out the most common characters */
        COMMON_CHARS
    }

    static List<Font> createSystemFonts(Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFonts)
    {
        return FontFactoryImpl.INSTANCE.createSystemFonts(openTypeFonts);
    }

    static Font createOpenTypeFont(String name, Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont)
    {
        return FontFactoryImpl.INSTANCE.createOpenTypeFont(name, openTypeFont);
    }

    static Font createOpenTypeFont(Supplier<InputStream> is, Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont)
    {
        return FontFactoryImpl.INSTANCE.createOpenTypeFont(is, openTypeFont);
    }

    static Font createAwtFont(Supplier<InputStream> is,
                              int fontFormat,
                              Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont) {
        return FontFactoryImpl.INSTANCE.createAwtFont(is, fontFormat, openTypeFont);
    }

    static Font createBitmapAsciiFont(String name, Supplier<InputStream> bitmap, int size)
    {
        return FontFactoryImpl.INSTANCE.createBitmapAsciiFont(name, bitmap, size);
    }

    static Font createBitmapUnifont(String name, Supplier<InputStream> glyphSizes, IntFunction<InputStream> pageSupplier, int size)
    {
        return FontFactoryImpl.INSTANCE.createBitmapUnifont(name, glyphSizes, pageSupplier, size);
    }

    @Override
    default List<? extends Font> getFonts()
    {
        return Collections.singletonList(this);
    }

    /** @return the font face name of this Font */
    String getName();

    /**
     * Returns the size of this Font.
     *
     * This is implementation dependant, as it might return either the point size (see {@link java.awt.Font#getSize()}
     * or the actual max height of the font. Therefore, it should only be used as a reference to derive fonts, not
     * for actual rendering.
     *
     * @return the size of this Font
     */
    int getSize();

    /**
     * Returns the style of this Font. The style can be PLAIN, BOLD, ITALIC, or BOLD+ITALIC.
     *
     * @return the style of this Font
     */
    int getStyle();

    /**
     * Creates a new Font object by replicating this Font object and applying a new style and size.
     *
     * @param style the style for the new Font
     * @return a new Font object.
     */
    default Font deriveFont(int style) {
        return deriveFont(style, getSize());
    }

    /**
     * Creates a new Font object by replicating this Font object and applying a new style and size.
     *
     * @param style the style for the new Font
     * @param size the size for the new Font
     * @return a new Font object.
     */
    Font deriveFont(int style, float size);
}
