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
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

class BitmapUnifont extends BaseBitmapFont
{
    private static final int GRID_ROWS = 16;
    private static final int GRID_COLS = 16;
    private static final int CHARACTERS_PER_PAGE = GRID_ROWS * GRID_COLS;

    private static final float GLYPH_RENDER_BORDER = 0.02F;

    /** Service used to make OpenGL calls */
    private final OglService oglService;
    /** Supplier which returns a page bitmaps given his number */
    private final IntFunction<InputStream> pageSupplier;

    /** A cache of all unicode pages that have at least one glyph rendered in a texture */
    private final Map<Integer, Bitmap> pageCache;

    /** Array of the start/end column (in upper/lower nibble) for every glyph in the /font directory. */
    private final byte[] glyphSizes;

    /** Arbitrary name used to identify this font */
    private final String name;

    /**
     * Creates a bitmap font supporting the first 256 characters from the given image.
     * The image will be loaded in the exact same way Minecraft loads it.
     *
     * @param glyphSizes bitmap image supplier
     * @param name arbitrary name used to identify this font
     * @param style style of this font
     * @param size size of this font
     */
    public BitmapUnifont(OglService oglService,
                         Supplier<InputStream> glyphSizes,
                         IntFunction<InputStream> pageSupplier,
                         String name, int style, float size)
    {
        this(oglService, readGlyphWidths(glyphSizes, new byte[65536]), pageSupplier, new HashMap<>(), name, style, size);
    }

    /**
     * Method used internally to derive a font.
     * This allows sharing the glyph size and the page cache between derived fonts.
     */
    private BitmapUnifont(OglService oglService,
                          byte[] glyphSizes,
                          IntFunction<InputStream> pageSupplier,
                          Map<Integer, Bitmap> pageCache,
                          String name, int style, float size)
    {
        super(style, size);
        this.oglService = oglService;
        this.glyphSizes = glyphSizes;
        this.pageSupplier = pageSupplier;
        this.pageCache = pageCache;
        this.name = name;
    }

    private static byte[] readGlyphWidths(Supplier<InputStream> glyphSizes, byte[] glyphWidths)
    {
        try(InputStream inputstream = glyphSizes.get())
        {
            int curr, read = 0;
            while(read < glyphWidths.length &&
                    (curr = inputstream.read(glyphWidths, read, glyphWidths.length - read)) != -1)
                read += curr;
            return glyphWidths;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    private Bitmap loadPageTexture(int i)
    {
        // TODO: this is never ever invalidated or cleared, we probably should at some point
        Bitmap texture = pageCache.get(i);
        if(texture != null)
            return texture;

        final BufferedImage image;
        try(InputStream is = pageSupplier.apply(i))
        {
            image = ImageIO.read(is);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }

        texture = new Bitmap();
        texture.textureName = oglService.allocateTexture(image);
        texture.width = image.getWidth();
        texture.height = image.getHeight();
        texture.gridRows = GRID_ROWS;
        texture.gridCols = GRID_COLS;
        texture.gridCellWidth = image.getWidth() / texture.gridCols;
        texture.gridCellHeight = image.getHeight() / texture.gridRows;

        pageCache.put(i, texture);
        return texture;
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
            if(!Character.isBmpCodePoint(text[i]))
                return i;
        return -1;
    }

    @Override
    public int canDisplayFrom(char[] text, int start, int limit)
    {
        for(int i = start; i < limit; i++)
            if(Character.isBmpCodePoint(text[i]))
                return i;
        return -1;
    }

    @Override
    protected Bitmap loadBitmap(char ch)
    {
        return loadPageTexture(ch / CHARACTERS_PER_PAGE);
    }

    @Override
    protected int texturePosX(Bitmap bitmap, char ch)
    {
        final int glyphStartX = glyphSizes[ch] >>> 4;
        return (ch % bitmap.gridCols * bitmap.gridCellWidth) + glyphStartX;
    }

    @Override
    protected int texturePosY(Bitmap bitmap, char ch)
    {
        return (ch & 255) / bitmap.gridCols * bitmap.gridCellHeight;
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
            return MINECRAFT_SPACE_SIZE;

        final int glyphStartX = glyphSizes[ch] >>> 4;
        final int glyphEndX = (glyphSizes[ch] & 0b00001111) + 1;
        return glyphEndX - glyphStartX;
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
        return new BitmapUnifont(oglService, glyphSizes, pageSupplier, pageCache, name, style, size);
    }
}
