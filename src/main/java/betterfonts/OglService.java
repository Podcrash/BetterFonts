package betterfonts;

import java.nio.IntBuffer;

/**
 * Service used to make OpenGL calls, in order to allow having different implementations depending on the Minecraft
 * version/mod environment/etc.
 *
 * For example in Minecraft 1.8, it could be implemented using GlStatManager and Tessellator instead of raw gl calls.
 * @author Ferlo
 */
public interface OglService {

    Tessellator tessellator();

    void glEnable(int cap);

    void glDisable(int cap);

    void glColor3f(float red, float green, float blue);

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

    @SuppressWarnings("UnusedReturnValue")
    interface Tessellator {

        Tessellator startDrawingQuads();

        Tessellator setColorRGBA(int red, int green, int blue, int alpha);

        default Tessellator setColorRGBA(int color) {
            return setColorRGBA(color >> 16 & 0xff, color >> 8 & 0xff, color & 0xff, color >> 24 & 0xff);
        }

        Tessellator addVertex(float x, float y, float z);

        Tessellator addVertexWithUV(float x, float y, float z, float u, float v);

        void draw();
    }
}
