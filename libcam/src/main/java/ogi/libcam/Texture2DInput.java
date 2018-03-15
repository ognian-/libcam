package ogi.libcam;

import android.opengl.GLES20;

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
