package betterfonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

class FontCache
{
    /** List of all available physical fonts on the system. Used by lookupFont() to find alternate fonts. */
    private final List<Font> allFonts;

    /**
     * A list of all fonts that have been returned so far by lookupFont(), and that will always be searched first for a usable font before
     * searching through allFonts[]. This list will only have plain variation of a font at a dummy point size, unlike fontCache which could
     * have multiple entries for the various styles (i.e. bold, italic, etc.) of a font. This list starts with Java's "SansSerif" logical
     * font.
     */
    private final List<Font> usedFonts = new ArrayList<>();

    /** The point size at which every OpenType font is rendered. */
    private int fontSize = 18;

    public FontCache(FontFactory fontFactory)
    {
        this.allFonts = Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts())
                .map(fontFactory::createOpenType)
                .collect(Collectors.toList());

        /* Use Java's logical font as the default initial font if user does not override it in some configuration file */
        GraphicsEnvironment.getLocalGraphicsEnvironment().preferLocaleFonts();
        usedFonts.add(fontFactory.createOpenType(java.awt.Font.SANS_SERIF, 1));
    }

    /** Change the default font used to pre-render glyph images */
    public void setDefaultFont(Font font)
    {
        System.out.println("BetterFonts loading font \"" + font.getName() + "\"");
        usedFonts.clear();
        usedFonts.add(font.deriveFont(Font.PLAIN, 1));

        fontSize = font.getSize();
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
        /* Try using an already known base font; the first font in usedFonts list is the one set with setDefaultFont() */
        Iterator<Font> iterator = usedFonts.iterator();
        while(iterator.hasNext())
        {
            /* Only use the font if it can layout at least the first character of the requested string range */
            Font font = iterator.next();
            if(font.canDisplayUpTo(text, start, limit) != start)
            {
                /* Return a font instance of the proper point size and style; usedFonts has only 1pt sized plain style fonts */
                return font.deriveFont(style, fontSize);
            }
        }

        /* If still not found, try searching through all fonts installed on the system for the first that can layout this string */
        iterator = allFonts.iterator();
        while(iterator.hasNext())
        {
            /* Only use the font if it can layout at least the first character of the requested string range */
            Font font = iterator.next();
            if(font.canDisplayUpTo(text, start, limit) != start)
            {
                /* If found, add this font to the usedFonts list so it can be looked up faster next time */
                System.out.println("BetterFonts loading font \"" + font.getName() + "\"");
                usedFonts.add(font);

                /* Return a font instance of the proper point size and style; allFonts has only 1pt sized plain style fonts */
                return font.deriveFont(style, fontSize);
            }
        }

        /* If no supported fonts found, use the default one (first in usedFonts) so it can draw its unknown character glyphs */
        Font font = usedFonts.get(0);

        /* Return a font instance of the proper point size and style; usedFonts only 1pt sized plain style fonts */
        return font.deriveFont(style, fontSize);
    }
}
