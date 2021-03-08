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

/**
 * Identifies a single glyph in the layed-out string. Includes a reference to a GlyphCache.Entry with the OpenGL texture ID
 * and position of the pre-rendered glyph image, and includes the x/y pixel coordinates of where this glyph occurs within
 * the string to which this Glyph object belongs.
 */
class Glyph implements Comparable<Glyph> {
    /** The index into the original string (i.e. with color codes) for the character that generated this glyph. */
    public int stringIndex;

    /** Texture ID and position/size of the glyph's pre-rendered image within the cache texture. */
    public GlyphTexture texture;

    /** Scale factor to apply to the texture's with and height */
    public float textureScale;

    /** Glyph's horizontal position (in pixels) relative to the entire string's baseline */
    public float x;

    /** Glyph's vertical position (in pixels) relative to the entire string's baseline */
    public float y;

    /** Glyph's horizontal advance (in pixels) used for strikethrough and underline effects */
    public float advance;

    /** Glyph's distance from the baseline to the ascender line. */
    public float ascent;

    /** Glyph's height. */
    public float height;

    /** The numeric color code (i.e. index into the colorCode[] array); -1 to reset default color */
    public byte colorCode;

    /** Combination of Font.PLAIN, Font.BOLD, and Font.ITALIC specifying font specific styles */
    public byte fontStyle;

    /** Combination of UNDERLINE, STRIKETHROUGH and RANDOM flags specifying effects performed by renderString() */
    public byte renderStyle;

    /**
     * Allows arrays of Glyph objects to be sorted. Performs numeric comparison on stringIndex.
     *
     * @param o the other Glyph object being compared with this one
     * @return either -1, 0, or 1 if this < other, this == other, or this > other
     */
    @Override
    public int compareTo(Glyph o) {
        return Integer.compare(stringIndex, o.stringIndex);
    }
}
