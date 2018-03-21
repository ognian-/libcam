package ogi.libgl;

import android.opengl.GLES20;
import android.opengl.Matrix;

import ogi.libgl.util.GLHelper;

public class Texture2DInput extends BaseTextureInput {

    @Override
    protected int genTextureId() {
        return GLHelper.genTexture(getTarget(), -1, null);
    }

    @Override
    protected int getTarget() {
        return GLES20.GL_TEXTURE_2D;
    }

    @Override
    protected void updateTexImage() {

    }

    @Override
    protected void getTexCoordsMatrix(float[] matrix) {
        Matrix.setIdentityM(matrix, 0);
    }

    @Override
    protected void releaseTexImage() {

    }
}
