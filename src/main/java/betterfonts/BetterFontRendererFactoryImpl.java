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

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class BetterFontRendererFactoryImpl implements BetterFontRendererFactory
{
    private final OglService oglService;
    private final int[] colors;
    private final List<FontInternal> fonts = new ArrayList<>();

    public BetterFontRendererFactoryImpl(OglService oglService, int[] colors)
    {
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
    public BetterFontRendererFactory withFont(Font font)
    {
        if(!(font instanceof FontInternal))
            throw new AssertionError("" +
                    "All types need to implement FontInternal " +
                    "(instance: " + font + ", type: " + font.getClass() + ")");
        fonts.add((FontInternal) font);
        return this;
    }

    @Override
    public BetterFontRendererFactory withFonts(Font... fonts)
    {
        Arrays.stream(fonts).forEach(this::withFont);
        return this;
    }

    @Override
    public BetterFontRendererFactory withFonts(Collection<Font> fonts)
    {
        fonts.forEach(this::withFont);
        return this;
    }

    @Override
    public BetterFontRendererFactory useSystemFonts(Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFonts)
    {
        final GraphicsEnvironment graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        graphicsEnv.preferLocaleFonts();
        /* Use Java's logical font as the default initial font if user does not override it */
        final java.awt.Font javaLogicalFont = new java.awt.Font(java.awt.Font.SANS_SERIF, Font.PLAIN, 1);
        fonts.addAll(Stream
                .concat(Stream.of(javaLogicalFont),
                        Arrays.stream(graphicsEnv.getAllFonts()).filter(font -> !font.equals(javaLogicalFont)))
                .map(font -> ((AwtBuilderImpl) openTypeFonts.apply(new AwtBuilderImpl(font))).getFont())
                .collect(Collectors.toList()));
        return this;
    }

    @Override
    public BetterFontRendererFactory withOpenTypeFont(String name, Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont)
    {
        fonts.add(((AwtBuilderImpl) openTypeFont.apply(new AwtBuilderImpl(name))).getFont());
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
        fonts.add(((AwtBuilderImpl) openTypeFont.apply(new AwtBuilderImpl(is, fontFormat))).getFont());
        return this;
    }

    @Override
    public BetterFontRendererFactory withBitmapAsciiFont(String name, Supplier<InputStream> bitmap, int size)
    {
        fonts.add(new BitmapAsciiFont(oglService, bitmap, name, Font.PLAIN, size));
        return this;
    }

    @Override
    public BetterFontRendererFactory withBitmapUnifont(String name, Supplier<InputStream> glyphSizes, IntFunction<InputStream> pageSupplier, int size)
    {
        fonts.add(new BitmapUnifont(oglService, glyphSizes, pageSupplier, name, Font.PLAIN, size));
        return this;
    }

    @Override
    public BetterFontRenderer build()
    {
        final BetterFontRenderer fontRenderer = new BetterFontRenderer(oglService, colors, fonts, true);
        fonts.clear();
        return fontRenderer;
    }

    private class AwtBuilderImpl implements AwtBuilder<AwtBuilderImpl, AwtBuilderImpl>, AwtBuilderEnd<AwtBuilderImpl>
    {
        private final java.awt.Font font;
        private final String name;
        private final Supplier<InputStream> inputStream;
        private final Integer inputStreamFormat;

        private int size;
        private Baseline baseline = Baseline.MINECRAFT;
        private float customBaseline;

        private Float weight;
        private Float posture;
        private Integer kerning;
        private Integer ligatures;

        public AwtBuilderImpl(java.awt.Font font)
        {
            this.font = font;
            this.name = null;
            this.inputStream = null;
            this.inputStreamFormat = null;
        }

        public AwtBuilderImpl(String name)
        {
            this.name = name;
            this.font = null;
            this.inputStream = null;
            this.inputStreamFormat = null;
        }

        public AwtBuilderImpl(Supplier<InputStream> inputStream, int inputStreamFormat)
        {
            this.inputStream = inputStream;
            this.inputStreamFormat = inputStreamFormat;
            this.font = null;
            this.name = null;
        }

        @Override
        public AwtBuilderImpl when(boolean condition, Function<AwtBuilderImpl, AwtBuilderImpl> action)
        {
            return condition ? action.apply(this) : this;
        }

        @Override
        public AwtBuilderImpl when(boolean condition,
                                   Function<AwtBuilderImpl, AwtBuilderImpl> action,
                                   Function<AwtBuilderImpl, AwtBuilderImpl> or)
        {
            return condition ? action.apply(this) : or.apply(this);
        }

        @Override
        public AwtBuilderImpl fromPointSize(int pointSize)
        {
            this.size = pointSize;
            return this;
        }

        @Override
        public AwtBuilderImpl fromHeight(float height)
        {
            final FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
            java.awt.Font font = getAwtFont((int) height);
            Rectangle2D rectangle = null;

            int pointSize = (int) height;
            int tries = 0;
            do {
                if(rectangle != null)
                    pointSize += (int) rectangle.getHeight() < height ? 1 : -1;

                font = font.deriveFont(java.awt.Font.PLAIN, pointSize);
                rectangle = font.createGlyphVector(frc, Constants.COMMON_CHARS).getVisualBounds();
            } while(Math.abs(rectangle.getHeight() - height) <= 0.01f && ++tries < 10);

            this.size = pointSize;
            return this;
        }

        @Override
        public AwtBuilderImpl withBaseline(float baseline)
        {
            this.baseline = null;
            this.customBaseline = baseline;
            return this;
        }

        @Override
        public AwtBuilderImpl withBaseline(Baseline baseline)
        {
            this.baseline = baseline;
            return this;
        }

        @Override
        public AwtBuilderImpl withWeight(float weight) {
            this.weight = weight;
            return this;
        }

        @Override
        public AwtBuilderImpl withPosture(float posture) {
            this.posture = posture;
            return this;
        }

        @Override
        public AwtBuilderImpl withKerning(boolean kerning) {
            this.kerning = kerning ? TextAttribute.KERNING_ON : 0;
            return this;
        }

        @Override
        public AwtBuilderImpl withLigatures(boolean ligatures) {
            this.ligatures = ligatures ? TextAttribute.LIGATURES_ON : 0;
            return this;
        }

        private java.awt.Font getAwtFont(int size)
        {
            final java.awt.Font font;
            if(this.font != null)
            {
                font = this.font.deriveFont(Font.PLAIN, size);
            }
            else if(name != null)
            {
                font = new java.awt.Font(name, Font.PLAIN, size);
            }
            else if(inputStream != null && inputStreamFormat != null)
            {
                try(InputStream is = this.inputStream.get())
                {
                    font = java.awt.Font.createFont(inputStreamFormat, is).deriveFont(Font.PLAIN, size);
                }
                catch(FontFormatException ex)
                {
                    throw new RuntimeException("Invalid font format for the provided InputStream", ex);
                }
                catch(IOException ex)
                {
                    throw new UncheckedIOException("Couldn't read the provided font InputStream", ex);
                }
            }
            else
            {
                throw new AssertionError("Invalid OpenTypeBuilderImpl state");
            }

            final Map<TextAttribute, Object> attributes = new HashMap<>();
            if(weight != null)
                attributes.put(TextAttribute.WEIGHT, weight);
            if(posture != null)
                attributes.put(TextAttribute.POSTURE, posture);
            if(kerning != null)
                attributes.put(TextAttribute.KERNING, kerning);
            if(ligatures != null)
                attributes.put(TextAttribute.LIGATURES, ligatures);

            return attributes.isEmpty() ?
                    font :
                    font.deriveFont(attributes);
        }

        public FontInternal getFont()
        {
            if(baseline == null)
                return new OpenTypeFont(oglService, getAwtFont(size), customBaseline);
            return new OpenTypeFont(oglService, getAwtFont(size), baseline);
        }
    }
}
