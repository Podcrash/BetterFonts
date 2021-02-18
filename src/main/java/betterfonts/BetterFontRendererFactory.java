package betterfonts;

import java.io.InputStream;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface BetterFontRendererFactory
{
    static BetterFontRendererFactory create(OglService oglService, int[] colors) {
        return new BetterFontRendererFactoryImpl(oglService, colors);
    }

    BetterFontRendererFactory useSystemFonts(int pointSize);

    BetterFontRendererFactory withOpenTypeFont(String name, Function<AwtBuilder, AwtBuilderEnd> openTypeFont);

    BetterFontRendererFactory withOpenTypeFont(Supplier<InputStream> is, Function<AwtBuilder, AwtBuilderEnd> openTypeFont);

    BetterFontRendererFactory withAwtFont(Supplier<InputStream> is, int fontFormat, Function<AwtBuilder, AwtBuilderEnd> openTypeFont);

    BetterFontRendererFactory withBitmapAsciiFont(String name, Supplier<InputStream> bitmap, int size);

    BetterFontRendererFactory withBitmapUnifont(String name, Supplier<InputStream> glyphSizes, IntFunction<InputStream> pageSupplier, int size);

    BetterFontRenderer build();

    interface AwtBuilder
    {
        AwtBuilderEnd fromPointSize(int pointSize);
    }

    interface AwtBuilderEnd
    {
    }
}
