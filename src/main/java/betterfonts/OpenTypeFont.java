package betterfonts;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.util.List;
import java.util.Objects;

class OpenTypeFont implements Font
{
    /** Cache needed for creating GlyphVectors and retrieving glyph texture coordinates. */
    private final OpenTypeGlyphCache glyphCache;

    /** Service used to make OpenGL calls */
    private final OglService oglService;

    /** Actual underlying java Font */
    private final java.awt.Font font;

    public OpenTypeFont(OglService oglService, OpenTypeGlyphCache glyphCache, java.awt.Font font)
    {
        this.oglService = oglService;
        this.glyphCache = glyphCache;
        this.font = font;
    }

    @Override
    public String getName()
    {
        return font.getFontName();
    }

    @Override
    public int getSize()
    {
        return font.getSize();
    }

    @Override
    public int getStyle()
    {
        return font.getStyle();
    }

    @Override
    public int canDisplayUpTo(char[] text, int start, int limit)
    {
        return font.canDisplayUpTo(text, start, limit);
    }

    @Override
    public int layoutFont(List<Glyph> glyphList, char[] text, int start, int limit, int layoutFlags, int advance)
    {
        /*
         * Ensure that all glyphs used by the string are pre-rendered and cached in the texture. Only safe to do so from the
         * main thread because cacheGlyphs() can crash LWJGL if it makes OpenGL calls from any other thread. In this case,
         * cacheString() will also not insert the entry into the stringCache since it may be incomplete if lookupGlyph()
         * returns null for any glyphs not yet stored in the glyph cache.
         */
        if(oglService.isContextCurrent())
        {
            glyphCache.cacheGlyphs(font, text, start, limit, layoutFlags);
        }

        /* Creating a GlyphVector takes care of all language specific OpenType glyph substitutions and positioning */
        GlyphVector vector = glyphCache.layoutGlyphVector(font, text, start, limit, layoutFlags);

        /*
         * Extract all needed information for each glyph from the GlyphVector so it won't be needed for actual rendering.
         * Note that initially, glyph.start holds the character index into the stripped text array. But after the entire
         * string is layed out, this field will be adjusted on every Glyph object to correctly index the original unstripped
         * string.
         */
        Glyph glyph = null;
        int numGlyphs = vector.getNumGlyphs();
        for(int index = 0; index < numGlyphs; index++)
        {
            Point position = vector.getGlyphPixelBounds(index, null, advance, 0).getLocation();

            /* Compute horizontal advance for the previous glyph based on this glyph's position */
            if(glyph != null)
            {
                glyph.advance = position.x - glyph.x;
            }

            /*
             * Allocate a new glyph object and add to the glyphList. The glyph.stringIndex here is really like stripIndex but
             * it will be corrected later to account for the color codes that have been stripped out.
             */
            glyph = new Glyph();
            glyph.stringIndex = start + vector.getGlyphCharIndex(index);
            glyph.texture = glyphCache.lookupGlyph(font, vector.getGlyphCode(index));
            glyph.x = position.x;
            glyph.y = position.y;
            glyphList.add(glyph);
        }

        /* Compute the advance position of the last glyph (or only glyph) since it can't be done by the above loop */
        advance += (int) vector.getGlyphPosition(numGlyphs).getX();
        if(glyph != null)
        {
            glyph.advance = advance - glyph.x;
        }

        /* Return the overall horizontal advance in pixels from the start of string */
        return advance;
    }

    @Override
    public Font deriveFont(int style, float size)
    {
        return new OpenTypeFont(oglService, glyphCache, font.deriveFont(style, size));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpenTypeFont that = (OpenTypeFont) o;
        return font.equals(that.font);
    }

    @Override
    public int hashCode() {
        return Objects.hash(font);
    }

    @Override
    public String toString() {
        return "OpenTypeFont{" +
                "font=" + font +
                '}';
    }
}
