package betterfonts;

import java.io.InputStream;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface BetterFontRendererFactory extends BetterFontConditionalClauses.ReturnSameType<BetterFontRendererFactory>
{
    static BetterFontRendererFactory create(OglService oglService, int[] colors)
    {
        return new BetterFontRendererFactoryImpl(oglService, colors);
    }

    BetterFontRendererFactory withFont(Font font);

    BetterFontRendererFactory withFonts(Font... fonts);

    BetterFontRendererFactory withFonts(Collection<Font> fonts);

    BetterFontRendererFactory useSystemFonts(int pointSize);

    BetterFontRendererFactory withOpenTypeFont(String name, Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont);

    BetterFontRendererFactory withOpenTypeFont(Supplier<InputStream> is, Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont);

    BetterFontRendererFactory withAwtFont(Supplier<InputStream> is, int fontFormat, Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont);

    BetterFontRendererFactory withBitmapAsciiFont(String name, Supplier<InputStream> bitmap, int size);

    BetterFontRendererFactory withBitmapUnifont(String name, Supplier<InputStream> glyphSizes, IntFunction<InputStream> pageSupplier, int size);

    BetterFontRenderer build();

    interface AwtBuilder<R extends AwtBuilderEnd<R>, T extends AwtBuilder<R, T>> extends BetterFontConditionalClauses.ReturnDiffType<T, R>
    {
        R fromPointSize(int pointSize);

        R fromHeight(float height);
    }

    interface AwtBuilderEnd<T extends AwtBuilderEnd<T>> extends BetterFontConditionalClauses.ReturnSameType<T>
    {
        T withBaseline(float baseline);

        T withBaseline(Baseline baseline);

        T withWeight(float weight);

        T withPosture(float posture);

        T withKerning(boolean kerning);

        T withLigatures(boolean ligatures);
    }

    enum Baseline
    {
        /** Uses the default Minecraft baseline (7 units from the top) */
        MINECRAFT,
        /** Uses the baseline calculated by awt */
        AWT,
        /** Calculates the baseline by laying out the most common characters */
        COMMON_CHARS
    }
}
