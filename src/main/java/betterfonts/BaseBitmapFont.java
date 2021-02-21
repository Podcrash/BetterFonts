package betterfonts;

import java.util.List;

public abstract class BaseBitmapFont implements FontInternal, Constants
{
    /** Style of this font */
    protected final int style;
    /** Size of this font */
    protected final float size;

    public BaseBitmapFont(int style, float size)
    {
        this.size = size;
        this.style = style;
    }

    protected static class Bitmap
    {
        /** The OpenGL texture ID of the loaded bitmap */
        public int textureName;
        /** Width of the bitmap texture */
        public int width;
        /** Height of the bitmap texture */
        public int height;
        /** Number of rows in the bitmap grid */
        public int gridRows;
        /** Number of cols in the bitmap grid */
        public int gridCols;
        /** Height of each cell which makes up the bitmap grid */
        public int gridCellHeight;
        /** Width of each cell which makes up the bitmap grid */
        public int gridCellWidth;
    }

    @Override
    public int getSize()
    {
        return Math.round(size);
    }

    @Override
    public int getStyle()
    {
        return style;
    }

    protected abstract Bitmap loadBitmap(char ch);

    protected abstract int texturePosX(Bitmap bitmap, char ch);

    protected abstract int texturePosY(Bitmap bitmap, char ch);

    protected abstract int defaultFontSize();

    protected abstract int getGlyphWidth(char ch);

    protected abstract float glyphRenderBorder();

    protected abstract float baselineOffset();

    protected abstract int glyphGap();

    @Override
    public float layoutFont(List<Glyph> glyphList, char[] text, int start, int limit, int layoutFlags, final float advance)
    {
        float newAdvance = advance;
        for(int i = start; i < limit; i++)
        {
            char ch = text[i];
            if(ch == ' ')
            {
                newAdvance += getGlyphWidth(ch) + glyphGap();
                continue;
            }

            final Bitmap bitmap = loadBitmap(ch);

            final int texturePosX = texturePosX(bitmap, ch);
            final int texturePosY = texturePosY(bitmap, ch);
            final int italics = (style & Font.ITALIC) != 0 ? 1 : 0; // TODO

            final float scaleFactor = ((float) defaultFontSize() / bitmap.gridCellWidth) * ((float) defaultFontSize() / (size * MINECRAFT_SCALE_FACTOR));
            final int glyphWidth = getGlyphWidth(ch);

            /*
             * Allocate a new glyph object and add to the glyphList. The glyph.stringIndex here is really like stripIndex but
             * it will be corrected later to account for the color codes that have been stripped out.
             */
            final Glyph glyph = new Glyph();
            glyph.stringIndex = i;

            glyph.texture = new GlyphTexture();
            glyph.texture.textureName = bitmap.textureName;
            glyph.texture.width = glyphWidth - glyphRenderBorder();
            glyph.texture.height = bitmap.gridCellHeight - glyphRenderBorder();
            glyph.texture.u1 = (float) texturePosX / bitmap.width;
            glyph.texture.v1 = (float) texturePosY / bitmap.height;
            glyph.texture.u2 = ((float) texturePosX + glyphWidth - glyphRenderBorder()) / bitmap.width;
            glyph.texture.v2 = ((float) texturePosY + bitmap.gridCellHeight - glyphRenderBorder()) / bitmap.height;

            glyph.textureScale = scaleFactor;
            glyph.x = advance + (newAdvance - advance);
            glyph.y = -baselineOffset();
            glyph.advance = glyphWidth * scaleFactor + glyphGap();
            glyph.ascent = baselineOffset();
            glyph.height = size * MINECRAFT_SCALE_FACTOR;
            glyphList.add(glyph);

            newAdvance += glyph.advance;
            if((style & Font.BOLD) != 0) // TODO
                newAdvance += 1;
        }

        return newAdvance;
    }


}
