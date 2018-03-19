package ogi.libcam;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;

import ogi.libgl.BaseTextureInput;
import ogi.libgl.util.GLHelper;

public class TextureExternalInput extends BaseTextureInput {

    private final SurfaceTexture mSurfaceTexture;
    private final boolean mSingleBuffer;

    public TextureExternalInput(SurfaceTexture surfaceTexture, boolean singleBuffer) {
        mSurfaceTexture = surfaceTexture;
        mSingleBuffer = singleBuffer;
    }

    @Override
    protected int genTextureId() {
        return GLHelper.genTextureExternal();
    }

    @Override
    protected int getTarget() {
        return GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    }

    @Override
    protected void updateTexImage() {
        mSurfaceTexture.updateTexImage();
    }

    @Override
    protected void getTexCoordsMatrix(float[] matrix) {
        mSurfaceTexture.getTransformMatrix(matrix);
    }

    @Override
    protected void releaseTexImage() {
        if (mSingleBuffer) mSurfaceTexture.releaseTexImage();
    }

}
