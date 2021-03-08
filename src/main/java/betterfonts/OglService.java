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

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

/**
 * Service used to make OpenGL calls, in order to allow having different implementations depending on the Minecraft
 * version/mod environment/etc.
 *
 * For example in Minecraft 1.8, it could be implemented using GlStatManager and Tessellator instead of raw gl calls.
 * @author Ferlo
 */
public interface OglService {

    boolean isContextCurrent();

    Tessellator tessellator();

    boolean glIsEnabled(int cap);

    void glEnable(int cap);

    void glDisable(int cap);

    void glColor3f(float red, float green, float blue);

    void glTranslatef(float x, float y, float z);

    int glGetTexParameteri(int target, int pName);

    void glTexParameteri(int target, int pName, int param);

    float glGetTexParameterf(int target, int pName);

    void glTexParameterf(int target, int pName, float param);

    void glTexEnvi(int target, int pName, int param);

    void glBlendFunc(int sFactor, int dFactor);

    int glGenTextures();

    void glBindTexture(int target, int texture);

    void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, IntBuffer pixels);

    void glTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, IntBuffer pixels);

    /*
     * Initialize texture with the provided BufferedImage. The texture is created with the same exact parameters
     * used by minecraft
     */
    int allocateTexture(BufferedImage image);

    @SuppressWarnings("UnusedReturnValue")
    interface Tessellator {

        Tessellator startDrawingQuads();

        Tessellator startDrawingQuadsWithUV();

        Tessellator setColorRGBA(int red, int green, int blue, int alpha);

        default Tessellator setColorRGBA(int color) {
            return setColorRGBA(color >> 16 & 0xff, color >> 8 & 0xff, color & 0xff, color >> 24 & 0xff);
        }

        Tessellator addVertex(float x, float y, float z);

        Tessellator addVertexWithUV(float x, float y, float z, float u, float v);

        void draw();
    }
}
