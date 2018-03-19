package ogi.libcam;

import android.opengl.GLES20;

import ogi.libgl.BaseTextureInput;
import ogi.libgl.GLHelper;

public class Texture2DInput extends BaseTextureInput {

    @Override
    protected int genTextureId() {
        return GLHelper.genTexture(getTarget(), -1, null);
    }

    @Override
    protected int getTarget() {
        return GLES20.GL_TEXTURE_2D;
    }

}
