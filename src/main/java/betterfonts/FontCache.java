package betterfonts;

import java.util.List;

class FontCache
{
    /**
     * A list of all fonts that can be returned by lookupFont(), and that will always be searched for a usable font before.
     * This list will only have plain variation of a font, unlike fontCache which could have multiple entries
     * for the various styles (i.e. bold, italic, etc.) of a font.
     */
    private final List<Font> fonts;

    public FontCache(List<Font> fonts)
    {
        this.fonts = fonts;
    }

    /**
     * Find the first font in the system able to render at least one character from a given string. The function always tries searching first
     * in the fontCache (based on the request style). Failing that, it searches the usedFonts list followed by the allFonts[] array.
     *
     * @param text the string to check against the font
     * @param start the offset into text at which to start checking characters for being supported by a font
     * @param limit the (offset + length) at which to stop checking characters
     * @param style a combination of the Font.PLAIN, Font.BOLD, and Font.ITALIC to request a particular font style
     * @return an OpenType font capable of displaying at least the first character at the start position in text
     */
    public Font lookupFont(char[] text, int start, int limit, int style)
    {
        for (Font font : fonts)
            /* Only use the font if it can layout at least the first character of the requested string range */
            if (font.canDisplayUpTo(text, start, limit) != start)
                /* Return a font instance of the proper style; usedFonts has only plain style fonts */
                return font.deriveFont(style);

        /* If no supported fonts found, use the default one (first in usedFonts) so it can draw its unknown character glyphs */
        final Font font = fonts.get(0);
        /* Return a font instance of the proper point size and style; usedFonts only 1pt sized plain style fonts */
        return font.deriveFont(style);
    }
}
