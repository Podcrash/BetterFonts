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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class BitmapAsciiFont extends BaseBitmapFont
{
    private static final Map<Character, Integer> CHARS_TO_BITMAP_INDEXES;
    static {
        final String str = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000";
        final char[] arr = str.toCharArray();
        CHARS_TO_BITMAP_INDEXES = IntStream.range(0, arr.length)
                .boxed()
                .collect(Collectors.toMap(i -> arr[i], i -> i, (u, v) -> {
                    // The 0 char is used as a placeholder for unused spaces, so it can be contained multiple times
                    if(str.charAt(u) == '\0')
                        return u;
                    throw new IllegalStateException(String.format(
                            "Duplicate key %s (attempted merging values %s and %s)",
                            str.charAt(u), u, v));
                }));
    }

    private static final int GRID_ROWS = 16;
    private static final int GRID_COLS = 16;

    private static final float GLYPH_RENDER_BORDER = 0.01F;

    /** Arbitrary name used to identify this font */
    private final String name;

    /** Bitmap used for rendering glyphs */
    private final LazyBitmap bitmap;
    /** Width for each singular glyphs */
    public int[] glyphWidths;

    /**
     * Creates a bitmap font supporting the first 256 characters from the given image.
     * The image will be loaded in the exact same way Minecraft loads it.
     *
     * @param bitmap bitmap image supplier
     * @param name arbitrary name used to identify this font
     * @param style style of this font
     * @param size size of this font
     */
    public BitmapAsciiFont(Supplier<InputStream> bitmap, String name, int style, float size)
    {
        this(loadBitmapImage(bitmap), name, style, size);
    }

    /** Bridge constructor to be able to load the bufferedImage once */
    private BitmapAsciiFont(BufferedImage bitmap,
                            String name, int style, float size)
    {
        this(
                new LazyBitmap(oglService -> readBitmap(oglService, bitmap)),
                readGlyphWidths(bitmap, bitmap.getWidth(), bitmap.getHeight()),
                name, style, size
        );
    }

    /** Constructor used internally which allows sharing the bitmap between derived fonts */
    private BitmapAsciiFont(LazyBitmap bitmap, int[] glyphWidths,
                            String name, int style, float size)
    {
        super(style, size);
        this.bitmap = bitmap;
        this.glyphWidths = glyphWidths;
        this.name = name;
    }

    private static BufferedImage loadBitmapImage(Supplier<InputStream> bitmapSupplier)
    {
        try(InputStream is = bitmapSupplier.get())
        {
            return ImageIO.read(is);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    private static Bitmap readBitmap(OglService oglService, BufferedImage image)
    {
        final Bitmap bitmap = new Bitmap();
        bitmap.textureName = oglService.allocateTexture(image);
        bitmap.width = image.getWidth();
        bitmap.height = image.getHeight();
        bitmap.gridRows = GRID_ROWS;
        bitmap.gridCols = GRID_COLS;
        bitmap.gridCellHeight = image.getHeight() / bitmap.gridRows;
        bitmap.gridCellWidth = image.getWidth() / bitmap.gridCols;
        return bitmap;
    }

    private static int[] readGlyphWidths(BufferedImage image, int bitmapWidth, int bitmapHeight)
    {
        int[] bitmap = new int[bitmapWidth * bitmapHeight];
        image.getRGB(0, 0, bitmapWidth, bitmapHeight, bitmap, 0, bitmapWidth);

        final int glyphHeight = bitmapHeight / GRID_ROWS;
        final int glyphWidth = bitmapWidth / GRID_COLS;

        final int[] charWidth = new int[GRID_ROWS * GRID_COLS];
        for (int asciiCode = 0; asciiCode < GRID_ROWS * GRID_COLS; asciiCode++)
        {
            final int row = asciiCode / GRID_COLS;
            final int col = asciiCode % GRID_COLS;

            if(asciiCode == (int) ' ')
            {
                charWidth[asciiCode] = MINECRAFT_SPACE_SIZE;
                continue;
            }

            // Find the first pixel from the end of the glyph which is visible
            int firstVisiblePixel = 0;
            outer: for(int glyphPixelX = glyphWidth - 1; glyphPixelX >= 0; glyphPixelX--)
            {
                final int bitmapPixelX = col * glyphWidth + glyphPixelX;

                for(int glyphPixelY = 0; glyphPixelY < glyphHeight; glyphPixelY++)
                {
                    final int bitmapPixelY = row * glyphHeight + glyphPixelY;
                    final int rgba = bitmap[bitmapPixelY * bitmapWidth + bitmapPixelX];
                    // Check the alpha channel
                    if((rgba >> 24 & 255) != 0)
                    {
                        firstVisiblePixel = glyphPixelX;
                        break outer;
                    }
                }
            }

            charWidth[asciiCode] = firstVisiblePixel + 1;
        }

        return charWidth;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public int canDisplayUpTo(char[] text, int start, int limit)
    {
        for(int i = start; i < limit; i++)
            if(!CHARS_TO_BITMAP_INDEXES.containsKey(text[i]))
                return i;
        return -1;
    }

    @Override
    public int canDisplayFrom(char[] text, int start, int limit)
    {
        for(int i = start; i < limit; i++)
            if(CHARS_TO_BITMAP_INDEXES.containsKey(text[i]))
                return i;
        return -1;
    }

    @Override
    protected BaseBitmapFont.Bitmap loadBitmap(OglService oglService, GlyphCaches glyphCaches, char ch)
    {
        return bitmap.get(oglService);
    }

    private int charToIndex(char ch) {
        return CHARS_TO_BITMAP_INDEXES.getOrDefault(ch, -1);
    }

    @Override
    protected int texturePosX(BaseBitmapFont.Bitmap bitmap, char ch)
    {
        return charToIndex(ch) % bitmap.gridCols * bitmap.gridCellWidth;
    }

    @Override
    protected int texturePosY(BaseBitmapFont.Bitmap bitmap, char ch)
    {
        return charToIndex(ch) / bitmap.gridCols * bitmap.gridCellHeight;
    }

    @Override
    protected int defaultFontSize()
    {
        return MINECRAFT_FONT_SIZE;
    }

    @Override
    protected int getGlyphWidth(char ch)
    {
        if(ch == ' ')
            return glyphWidths[ch];
        return (int) (0.5D + glyphWidths[charToIndex(ch)]);
    }

    @Override
    protected float glyphRenderBorder()
    {
        return GLYPH_RENDER_BORDER;
    }

    @Override
    protected float baselineOffset()
    {
        return MINECRAFT_BASELINE_OFFSET * (size * MINECRAFT_SCALE_FACTOR / defaultFontSize());
    }

    @Override
    protected int glyphGap()
    {
        return MINECRAFT_GLYPH_GAP;
    }

    @Override
    public FontInternal deriveFont(int style, float size)
    {
        return new BitmapAsciiFont(bitmap, glyphWidths, name, style, size);
    }
}
