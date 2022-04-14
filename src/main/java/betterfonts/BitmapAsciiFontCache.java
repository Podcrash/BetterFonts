/*
 * Minecraft OpenType Font Support Mod
 *
 * Copyright (C) 2021-2022 Podcrash Ltd
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
import java.util.function.Supplier;

class BitmapAsciiFontCache
{

    private final FontRenderContext fontRenderContext;

    /**
     * A cache of all font textures.
     * The key is the unique font id.
     */
    private final Map<Integer, BaseBitmapFont.Bitmap> bitmapCache = new HashMap<>();

    public BitmapAsciiFontCache(FontRenderContext fontRenderContext)
    {
        this.fontRenderContext = fontRenderContext;
    }

    public void invalidate()
    {
        if(fontRenderContext.isGraphicsContext())
        {
            final OglService oglService = fontRenderContext.ensureGraphicsContextCurrent();
            bitmapCache.values().stream()
                    .mapToInt(g -> g.textureName)
                    .distinct()
                    .forEach(oglService::glDeleteTextures);
        }

        bitmapCache.clear();
    }

    public BaseBitmapFont.Bitmap loadBitmap(int fontId, Supplier<InputStream> bitmapSupplier)
    {
        BaseBitmapFont.Bitmap texture = bitmapCache.get(fontId);
        if(texture != null)
            return texture;

        texture = readBitmap(loadBitmapImage(bitmapSupplier));
        if(fontRenderContext.shouldCache())
            bitmapCache.put(fontId, texture);
        return texture;
    }

    private BaseBitmapFont.Bitmap readBitmap(BufferedImage image)
    {
        final BaseBitmapFont.Bitmap bitmap = new BaseBitmapFont.Bitmap();
        bitmap.textureName = fontRenderContext
                .getIfGraphicsContextCurrent(oglService -> oglService.allocateTexture(image))
                .orElse(-1);
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
