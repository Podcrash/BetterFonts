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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

class BitmapUnifontPageCache
{
    private static final int GRID_ROWS = 16;
    private static final int GRID_COLS = 16;
    private static final int CHARACTERS_PER_PAGE = GRID_ROWS * GRID_COLS;

    /** Service used to make OpenGL calls */
    private final OglService oglService;

    /**
     * A cache of all unicode pages that have at least one glyph rendered in a texture.
     *
     * The key is a 64 bit number such that the lower 32 bits are the glyph-code and the upper 32 are the
     * unique font id. This makes for a single globally unique number to identify any glyph from any font.
     */
    private final Map<Long, BaseBitmapFont.Bitmap> pageCache = new HashMap<>();

    public BitmapUnifontPageCache(OglService oglService)
    {
        this.oglService = oglService;
    }

    public void invalidate()
    {
        final int[] textures = pageCache.values().stream()
                .mapToInt(g -> g.textureName)
                .distinct()
                .toArray();
        pageCache.clear();
        Arrays.stream(textures).forEach(oglService::glDeleteTextures);
    }

    public BaseBitmapFont.Bitmap loadPageTexture(int fontId,
                                                 IntFunction<InputStream> pageSupplier,
                                                 char ch)
    {
        final int page = ch / CHARACTERS_PER_PAGE;
        final long key = fontId | page;

        BaseBitmapFont.Bitmap texture = pageCache.get(key);
        if(texture != null)
            return texture;

        final BufferedImage image;
        try(InputStream is = pageSupplier.apply(page))
        {
            image = ImageIO.read(is);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }

        texture = new BaseBitmapFont.Bitmap();
        texture.textureName = oglService.allocateTexture(image);
        texture.width = image.getWidth();
        texture.height = image.getHeight();
        texture.gridRows = GRID_ROWS;
        texture.gridCols = GRID_COLS;
        texture.gridCellWidth = image.getWidth() / texture.gridCols;
        texture.gridCellHeight = image.getHeight() / texture.gridRows;

        pageCache.put(key, texture);
        return texture;
    }
}
