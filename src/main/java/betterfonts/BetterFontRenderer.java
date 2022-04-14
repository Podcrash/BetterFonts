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

import java.awt.geom.Rectangle2D;

@SuppressWarnings("unused")
public interface BetterFontRenderer extends FontMetrics, Constants
{

    void setAntiAlias(boolean antiAlias);

    default float drawString(String text, float startX, float startY, int initialColor)
    {
        return drawString(text, startX, startY, initialColor, false);
    }

    default float drawString(String text, float startX, float startY, int initialColor, boolean dropShadow)
    {
        return drawString(text, startX, startY, initialColor, dropShadow, HorizontalAlignment.LEADING);
    }

    default float drawString(String text, float startX, float startY, int initialColor, HorizontalAlignment hAlignment)
    {
        return drawString(text, startX, startY, initialColor, false, hAlignment);
    }

    float drawString(String text, float startX, float startY, int initialColor, boolean dropShadow, HorizontalAlignment hAlignment);

    default float drawSplitString(String text, float startX, float startY, int wrapWidth, int initialColor)
    {
        return drawSplitString(text, startX, startY, wrapWidth, initialColor, false);
    }

    default float drawSplitString(String text, float startX, float startY, int wrapWidth, int initialColor, boolean dropShadow)
    {
        return drawSplitString(text, startX, startY, wrapWidth, initialColor, dropShadow, HorizontalAlignment.LEADING);
    }

    float drawSplitString(String text, float startX, float startY, int wrapWidth, int initialColor, boolean dropShadow, HorizontalAlignment hAlignment);

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
}
