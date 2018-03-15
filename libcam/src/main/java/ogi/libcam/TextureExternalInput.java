package ogi.libcam;

import android.opengl.GLES11Ext;

public class TextureExternalInput extends BaseTextureInput {

    @Override
    protected int genTextureId() {
        return GLHelper.genTextureExternal();
    }

    @Override
    protected int getTarget() {
        return GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    }

}
