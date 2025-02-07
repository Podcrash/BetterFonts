package betterfonts;

import java.io.InputStream;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public interface FontFactory {

    List<BetterFont> createSystemFonts(Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFonts);

    BetterFont createOpenTypeFont(String name, Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont);

    BetterFont createOpenTypeFont(Supplier<InputStream> is, Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont);

    BetterFont createAwtFont(Supplier<InputStream> is,
                             int fontFormat,
                             Function<AwtBuilder<?, ?>, AwtBuilderEnd<?>> openTypeFont);

    BetterFont createBitmapAsciiFont(String name, Supplier<InputStream> bitmap, int size);

    BetterFont createBitmapUnifont(String name, Supplier<InputStream> glyphSizes, IntFunction<InputStream> pageSupplier, int size);

    interface AwtBuilder<R extends AwtBuilderEnd<R>, T extends AwtBuilder<R, T>> extends BetterFontConditionalClauses.ReturnDiffType<T, R>
    {
        R fromPointSize(int pointSize);

        R fromHeight(float height);
    }

    interface AwtBuilderEnd<T extends AwtBuilderEnd<T>> extends BetterFontConditionalClauses.ReturnSameType<T>
    {
        T withBaseline(float baseline);

        T withBaseline(BetterFont.Baseline baseline);

        T withWeight(float weight);

        T withPosture(float posture);

        T withKerning(boolean kerning);

        T withLigatures(boolean ligatures);
    }
}
