/*
 * Minecraft OpenType Font Support Mod
 *
 * Copyright (C) 2021-2022 Podcrash Ltd
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

import java.util.List;

interface BetterFontInternal extends BetterFont
{
    /** A flag indicating that text is left-to-right as determined by Bidi analysis. */
    int LAYOUT_LEFT_TO_RIGHT = 0;

    /** A flag indicating that text is right-to-left as determined by Bidi analysis. */
    int LAYOUT_RIGHT_TO_LEFT = 1;

    static BetterFontInternal cast(BetterFont font) {
        if(!(font instanceof BetterFontInternal))
            throw new AssertionError("" +
                    "All types need to implement FontInternal " +
                    "(instance: " + font + ", type: " + font.getClass() + ")");
        return (BetterFontInternal) font;
    }

    /**
     * Indicates whether or not this {@code Font} can display the characters in the specified {@code text}
     * starting at {@code start} and ending at {@code limit}.
     *
     * @param text the specified array of {@code char} values
     * @param start the offset into text
     * @param limit the (offset + length)
     * @return an offset into {@code text} that points to the first character in {@code text} that this
     *          {@code Font} cannot display; or {@code -1} if this {@code Font} can display all characters in
     *          {@code text}.
     */
    int canDisplayUpTo(char[] text, int start, int limit);

    /**
     * Indicates the first character this {@code Font} can display in the specified {@code text}
     * starting at {@code start} and ending at {@code limit}.
     *
     * @param text the specified array of {@code char} values
     * @param start the offset into text
     * @param limit the (offset + length)
     * @return an offset into {@code text} that points to the first character in {@code text} that this
     *          {@code Font} can display; or {@code -1} if this {@code Font} can display no characters in
     *          {@code text}.
     */
    int canDisplayFrom(char[] text, int start, int limit);

    /**
     * Allocate new Glyph objects and add them to the glyph list. This sequence of Glyphs represents a portion of the
     * string where all glyphs run contiguously in either LTR or RTL and come from the same physical/logical font.
     *
     * @param fontRenderContext service used to make OpenGL calls, if present
     * @param glyphCaches cache needed for creating GlyphVectors and retrieving glyph texture coordinates
     * @param glyphList all newly created Glyph objects are added to this list
     * @param text the string to layout
     * @param start the offset into text at which to start the layout
     * @param limit the (offset + length) at which to stop performing the layout
     * @param layoutFlags either Font.LAYOUT_RIGHT_TO_LEFT or Font.LAYOUT_LEFT_TO_RIGHT
     * @param advance the horizontal advance (i.e. X position) returned by previous call to layoutString()
     * @return the advance (horizontal distance) of this string plus the advance passed in as an argument
     *
     * @todo need to adjust position of all glyphs if digits are present, by assuming every digit should be 0 in length
     */
    float layoutFont(BetterFontRenderContext fontRenderContext,
                     GlyphCaches glyphCaches,
                     List<Glyph> glyphList,
                     char[] text, int start, int limit, int layoutFlags, float advance);

    @Override
    default BetterFontInternal deriveFont(int style)
    {
        return deriveFont(style, getSize());
    }

    @Override
    BetterFontInternal deriveFont(int style, float size);
}
