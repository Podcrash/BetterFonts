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
import java.util.function.Supplier;

class BitmapAsciiFontCache
{

    /** Service used to make OpenGL calls */
    private final OglService oglService;

    /**
     * A cache of all font textures.
     * The key is the unique font id.
     */
    private final Map<Integer, BaseBitmapFont.Bitmap> bitmapCache = new HashMap<>();

    public BitmapAsciiFontCache(OglService oglService) {
        this.oglService = oglService;
    }

    public void invalidate()
    {
        final int[] textures = bitmapCache.values().stream()
                .mapToInt(g -> g.textureName)
                .distinct()
                .toArray();
        bitmapCache.clear();
        Arrays.stream(textures).forEach(oglService::glDeleteTextures);
    }

    public BaseBitmapFont.Bitmap loadBitmap(int fontId, Supplier<InputStream> bitmapSupplier)
    {
        BaseBitmapFont.Bitmap texture = bitmapCache.get(fontId);
        if(texture != null)
            return texture;

        bitmapCache.put(fontId, texture = readBitmap(loadBitmapImage(bitmapSupplier)));
        return texture;
    }

    private BaseBitmapFont.Bitmap readBitmap(BufferedImage image)
    {
        final BaseBitmapFont.Bitmap bitmap = new BaseBitmapFont.Bitmap();
        bitmap.textureName = oglService.allocateTexture(image);
        bitmap.width = image.getWidth();
        bitmap.height = image.getHeight();
        bitmap.gridRows = BitmapAsciiFont.GRID_ROWS;
        bitmap.gridCols = BitmapAsciiFont.GRID_COLS;
        bitmap.gridCellHeight = image.getHeight() / bitmap.gridRows;
        bitmap.gridCellWidth = image.getWidth() / bitmap.gridCols;
        return bitmap;
    }

    private BufferedImage loadBitmapImage(Supplier<InputStream> bitmapSupplier)
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
}
