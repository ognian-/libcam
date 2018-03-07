package ogi.libcam;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import static ogi.libcam.GLHelper.glCheck;

public class ExternalTexture {

    private int mTextureId = -1;
    private long mThreadId = -1;
    private final Object mLock = new Object();
    private final float[] mTexCoordsMatrix = new float[16];

    private final SurfaceTexture.OnFrameAvailableListener mListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            synchronized (mLock) {
                surfaceTexture.getTransformMatrix(mTexCoordsMatrix);
            }
        }
    };

    public SurfaceTexture onCreate(SurfaceTexture surfaceTexture) {
        synchronized (mLock) {
            if (mTextureId != -1 || mThreadId != -1) throw new IllegalStateException("Must destroy first");
            mTextureId = GLHelper.genTextureExternal();
            if (surfaceTexture == null) {
                surfaceTexture = new SurfaceTexture(mTextureId);
            } else {
                surfaceTexture.attachToGLContext(mTextureId);
            }
            surfaceTexture.setOnFrameAvailableListener(mListener);
            mThreadId = Thread.currentThread().getId();
            Matrix.setIdentityM(mTexCoordsMatrix, 0);
            return surfaceTexture;
        }
    }

    public void onDraw(int uniformTexture, int uniformMatrix, int textureSlot) {
        synchronized (mLock) {//TODO wait for frame?
            GLES20.glUniformMatrix4fv(uniformMatrix, 1, false, mTexCoordsMatrix, 0); glCheck();
            GLES20.glActiveTexture(textureSlot); glCheck();
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId); glCheck();
            GLES20.glUniform1i(uniformTexture, GLHelper.getTextureSlot(textureSlot)); glCheck();
        }
    }

    public void onDestroy(SurfaceTexture surfaceTexture) {
        synchronized (mLock) {
            if (mThreadId != -1 && mThreadId != Thread.currentThread().getId()) throw new IllegalStateException("Called on wrong thread");
            try {
                if (surfaceTexture != null) {
                    surfaceTexture.setOnFrameAvailableListener(null);
                    surfaceTexture.detachFromGLContext();
                }
            } finally {
                try {
                    if (mTextureId != -1) {
                        GLES20.glDeleteTextures(1, new int[]{mTextureId}, 0); glCheck();
                    }
                } finally {
                    mTextureId = -1;
                    mThreadId = -1;
                }
            }
        }
    }

}
