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

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

class NoContextOglService implements OglService
{
    @Override
    public boolean isGraphicsContext()
    {
        return false;
    }

    @Override
    public boolean isContextCurrent()
    {
        return false;
    }

    private RuntimeException missingContextException()
    {
        return new UnsupportedOperationException("This is not a Graphics context");
    }

    @Override
    public Tessellator tessellator()
    {
        throw missingContextException();
    }

    @Override
    public boolean glIsEnabled(int cap)
    {
        throw missingContextException();
    }

    @Override
    public void glEnable(int cap)
    {
        throw missingContextException();
    }

    @Override
    public void glDisable(int cap)
    {
        throw missingContextException();
    }

    @Override
    public void glColor3f(float red, float green, float blue)
    {
        throw missingContextException();
    }

    @Override
    public void glTranslatef(float x, float y, float z)
    {
        throw missingContextException();
    }

    @Override
    public int glGetTexParameteri(int target, int pName)
    {
        throw missingContextException();
    }

    @Override
    public void glTexParameteri(int target, int pName, int param)
    {
        throw missingContextException();
    }

    @Override
    public float glGetTexParameterf(int target, int pName)
    {
        throw missingContextException();
    }

    @Override
    public void glTexParameterf(int target, int pName, float param)
    {
        throw missingContextException();
    }

    @Override
    public void glTexEnvi(int target, int pName, int param)
    {
        throw missingContextException();
    }

    @Override
    public void glBlendFunc(int sFactor, int dFactor)
    {
        throw missingContextException();
    }

    @Override
    public int glGenTextures()
    {
        throw missingContextException();
    }

    @Override
    public void glDeleteTextures(int texture)
    {
        throw missingContextException();
    }

    @Override
    public void glBindTexture(int target, int texture)
    {
        throw missingContextException();
    }

    @Override
    public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, IntBuffer pixels)
    {
        throw missingContextException();
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, IntBuffer pixels)
    {
        throw missingContextException();
    }

    @Override
    public int allocateTexture(BufferedImage image)
    {
        throw missingContextException();
    }
}
