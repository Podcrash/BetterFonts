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

import java.util.List;

public interface FontMetrics extends Constants
{

    /**
     * Return the width of a string in pixels. Used for centering strings inside GUI buttons.
     *
     * @param str compute the width of this string
     * @return the width in pixels (divided by 2; this matches the scaled coordinate system used by GUIs in Minecraft)
     */
    float getStringWidth(String str);

    /**
     * Return the width of a character in pixels. Used for centering strings inside GUI buttons.
     *
     * @param character compute the width of this character
     * @return the width in pixels (divided by 2; this matches the scaled coordinate system used by GUIs in Minecraft)
     */
    float getCharWidth(char character);

    default float getFontHeight()
    {
        return getStringHeight(COMMON_CHARS);
    }

    /**
     * Return the height of a string in pixels. Used for centering strings inside GUI buttons.
     *
     * @param str compute the height of this string
     * @return the height in pixels (divided by 2; this matches the scaled coordinate system used by GUIs in Minecraft)
     */
    float getStringHeight(String str);

    /**
     * Return the height of a string baseline in pixels. Used for centering strings inside GUI buttons.
     *
     * @param str compute the height of this string
     * @return the height in pixels (divided by 2; this matches the scaled coordinate system used by GUIs in Minecraft)
     */
    float getStringBaseline(String str);

    /**
     * Return the number of characters in a string that will completely fit inside the specified width when rendered, with
     * or without preferring to break the line at whitespace instead of breaking in the middle of a word. This private provides
     * the real implementation of both sizeStringToWidth() and trimStringToWidth().
     *
     * @param str           the String to analyze
     * @param width         the desired string width (in GUI coordinate system)
     * @param breakAtSpaces set to prefer breaking line at spaces than in the middle of a word
     * @return the number of characters from str that will fit inside width
     */
    int sizeString(String str, float width, boolean breakAtSpaces);

    /**
     * Return the number of characters in a string that will completely fit inside the specified width when rendered.
     *
     * @param str   the String to analyze
     * @param width the desired string width (in GUI coordinate system)
     * @return the number of characters from str that will fit inside width
     */
    int sizeStringToWidth(String str, float width);

    /**
     * Trim a string so that it fits in the specified width when rendered.
     *
     * @param str   the String to trim
     * @param width the desired string width (in GUI coordinate system)
     * @return the trimmed and optionally reversed string
     */
    String trimStringToWidth(String str, float width);

    /**
     * Trim a string so that it fits in the specified width when rendered, optionally reversing the string
     *
     * @param str     the String to trim
     * @param width   the desired string width (in GUI coordinate system)
     * @param reverse if true, the returned string will also be reversed
     * @return the trimmed and optionally reversed string
     */
    String trimStringToWidth(String str, float width, boolean reverse);

    List<String> listFormattedStringToWidth(String str, float wrapWidth);
}
