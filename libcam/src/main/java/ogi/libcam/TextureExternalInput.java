package ogi.libcam;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.util.Size;
import android.view.Surface;

import ogi.libgl.BaseTextureInput;
import ogi.libgl.util.GLHelper;
import ogi.libgl.util.WaitResult;

public class TextureExternalInput extends BaseTextureInput {

    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;

    private final WaitResult mSurfaceResult = new WaitResult();

    public Surface getSurface() {
        return (Surface) mSurfaceResult.getResult();
    }

    public void setDefaultBufferSize(Size size) {
        mCheckThread.check();
        mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
    }

    @Override
    public void close() {
        super.close();
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        mSurfaceResult.close();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mSurfaceTexture == null) {
            mSurfaceTexture = new SurfaceTexture(getTextureId());
            mSurface = new Surface(mSurfaceTexture);
            mSurfaceResult.setResult(mSurface);
        } else {
            mSurfaceTexture.attachToGLContext(getTextureId());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSurfaceTexture != null) mSurfaceTexture.detachFromGLContext();
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
        //TODO only in single buffer mode
        // mSurfaceTexture.releaseTexImage();
    }

}
