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

import java.awt.geom.Rectangle2D;
import java.util.List;

@SuppressWarnings("unused")
public interface BetterFontRenderer extends Constants {

    void setAntiAlias(boolean antiAlias);

    float drawString(String text, float startX, float startY, int initialColor, boolean dropShadow);

    /**
     * Render a single-line string to the screen using the current OpenGL color. The (x,y) coordinates are of the upper-left
     * corner of the string's bounding box, rather than the baseline position as is typical with fonts. This function will also
     * add the string to the cache so the next renderString() call with the same string is faster.
     *
     * @param str          the string being rendered; it can contain color codes
     * @param startX       the x coordinate to draw at
     * @param startY       the y coordinate to draw at
     * @param initialColor the initial RGBA color to use when drawing the string; embedded color codes can override the RGB component
     * @param shadowFlag   if true, color codes are replaces by a darker version used for drop shadows
     * @return the total advance (horizontal distance) of this string
     * @todo Add optional NumericShaper to replace ASCII digits with locale specific ones
     */
    float renderString(String str, float startX, float startY, int initialColor, boolean shadowFlag);

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
     * Return the bounds in pixels where this string gets rendered in, relative to its height and width.
     * Used for centering strings inside GUI buttons.
     *
     * @param str compute the visual bounds of this string
     * @return the visual bounds in pixels
     */
    Rectangle2D.Float getStringVisualBounds(String str);

    /**
     * Return the bounds in pixels where this string gets rendered in, relative to its height and width.
     * Used for centering strings inside GUI buttons.
     *
     * This overload uses and returns the rect provided by the user, without creating another one.
     * Should be used for caching purposes.
     *
     * @param str       compute the visual bounds of this string
     * @param rectangle rect where the visual bounds will be set
     * @return the visual bounds in pixels
     */
    Rectangle2D.Float getStringVisualBounds(String str, Rectangle2D.Float rectangle);

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
