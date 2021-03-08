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
 * This class holds information for a glyph about its pre-rendered image in an OpenGL texture. The texture coordinates in
 * this class are normalized in the standard 0.0 - 1.0 OpenGL range.
 */
class GlyphTexture
{
    /** The OpenGL texture ID that contains this glyph image. */
    public int textureName;

    /** The width in pixels of the glyph image. */
    public float width;

    /** The height in pixels of the glyph image. */
    public float height;

    /** The horizontal texture coordinate of the upper-left corner. */
    public float u1;

    /** The vertical texture coordinate of the upper-left corner. */
    public float v1;

    /** The horizontal texture coordinate of the lower-right corner. */
    public float u2;

    /** The vertical texture coordinate of the lower-right corner. */
    public float v2;
}
