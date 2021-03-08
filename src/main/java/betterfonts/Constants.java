/*
 * Minecraft OpenType Font Support Mod
 *
 * Copyright (C) 2021 Podcrash Ltd
 * Copyright (C) 2012 Wojciech Stryjewski <thvortex@gmail.com>
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

interface Constants {

    float MINECRAFT_SCALE_FACTOR = 0.5F;

    int MINECRAFT_FONT_SIZE = 8;

    int MINECRAFT_GLYPH_GAP = 1;

    int MINECRAFT_SPACE_SIZE = 3;

    /** Vertical adjustment (in pixels * 2) to string position because Minecraft uses top of string instead of baseline */
    int MINECRAFT_BASELINE_OFFSET = 7;

    /** Common characters used to calculate stuff with AWT fonts */
    String COMMON_CHARS = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{}~";
}
