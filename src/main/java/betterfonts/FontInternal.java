package betterfonts;

import java.util.List;

interface FontInternal extends Font
{
    /** A flag indicating that text is left-to-right as determined by Bidi analysis. */
    int LAYOUT_LEFT_TO_RIGHT = 0;

    /** A flag indicating that text is right-to-left as determined by Bidi analysis. */
    int LAYOUT_RIGHT_TO_LEFT = 1;

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
    float layoutFont(List<Glyph> glyphList, char[] text, int start, int limit, int layoutFlags, float advance);

    @Override
    default FontInternal deriveFont(int style)
    {
        return deriveFont(style, getSize());
    }

    @Override
    FontInternal deriveFont(int style, float size);
}
