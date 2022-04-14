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

import java.util.Objects;

class GlyphCaches
{

    private final OpenTypeGlyphCache openTypeGlyphCache;
    private final BitmapAsciiFontCache bitmapAsciiFontCache;
    private final BitmapUnifontPageCache bitmapUnifontPageCache;

    public GlyphCaches(FontRenderContext fontRenderContext,
                       boolean createOpenTypeGlyphCache,
                       boolean createBitmapAsciiFontCache,
                       boolean createBitmapUnifontPageCache)
    {
        this.openTypeGlyphCache = createOpenTypeGlyphCache ? new OpenTypeGlyphCache(fontRenderContext) : null;
        this.bitmapAsciiFontCache = createBitmapAsciiFontCache ? new BitmapAsciiFontCache(fontRenderContext) : null;
        this.bitmapUnifontPageCache = createBitmapUnifontPageCache ? new BitmapUnifontPageCache(fontRenderContext) : null;
    }

    public void setAntiAlias(boolean antiAlias)
    {
        if(openTypeGlyphCache != null)
            openTypeGlyphCache.setAntiAlias(antiAlias);
    }

    public void invalidate()
    {
        if(openTypeGlyphCache != null)
            openTypeGlyphCache.invalidate();
        if(bitmapUnifontPageCache != null)
            bitmapUnifontPageCache.invalidate();
        if(bitmapAsciiFontCache != null)
            bitmapAsciiFontCache.invalidate();
    }

    public OpenTypeGlyphCache ensureOpenTypeGlyphCache()
    {
        return Objects.requireNonNull(openTypeGlyphCache, "OpenTypeGlyphCache");
    }

    public BitmapAsciiFontCache ensureBitmapAsciiFontCache()
    {
        return Objects.requireNonNull(bitmapAsciiFontCache, "BitmapAsciiFontCache");
    }

    public BitmapUnifontPageCache ensureBitmapUnifontPageCache()
    {
        return Objects.requireNonNull(bitmapUnifontPageCache, "BitmapUnifontPageCache");
    }
}
