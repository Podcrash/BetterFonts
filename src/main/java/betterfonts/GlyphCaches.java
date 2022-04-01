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

import java.util.Objects;

class GlyphCaches
{

    private final OpenTypeGlyphCache openTypeGlyphCache;

    public GlyphCaches(OglService oglService, boolean createOpenTypeGlyphCache)
    {
        this.openTypeGlyphCache = createOpenTypeGlyphCache ? new OpenTypeGlyphCache(oglService) : null;
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
    }

    public OpenTypeGlyphCache ensureOpenTypeGlyphCache()
    {
        return Objects.requireNonNull(openTypeGlyphCache, "OpenTypeGlyphCache");
    }
}
