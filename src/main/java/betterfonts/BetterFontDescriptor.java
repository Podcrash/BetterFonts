/*
 * Minecraft OpenType Font Support Mod
 *
 * Copyright (C) 2022 Podcrash Ltd
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

import java.util.Collection;
import java.util.List;

public interface BetterFontDescriptor
{

    static BetterFontDescriptorBuilder builder()
    {
        return new BetterFontDescriptorBuilderImpl(FontFactoryImpl.INSTANCE);
    }

    static BetterFontDescriptor create(BetterFont... fonts)
    {
        return new CompositeFont(fonts);
    }

    static BetterFontDescriptor create(Collection<BetterFont> fonts)
    {
        return new CompositeFont(fonts);
    }

    BetterFontMetrics getMetrics();

    List<? extends BetterFont> getFonts();
}
