package ogi.libcam;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import static ogi.libcam.GLHelper.glCheck;

public class ExternalTexture {

    private int mTextureId = -1;
    private final Object mLock = new Object();
    private final float[] mTexCoordsMatrix = new float[16];
    private boolean mFrameAvailable;

    public int getId() {
        synchronized (mLock) {
            return mTextureId;
        }
    }

    public void onFrameAvailable(float[] texCoordMatrix) {
        synchronized (mLock) {
            System.arraycopy(texCoordMatrix, 0, mTexCoordsMatrix, 0, 16);
            mFrameAvailable = true;
            mLock.notifyAll();
        }
    }

    public void onCreate() {
        synchronized (mLock) {
            if (mTextureId != -1) throw new IllegalStateException("Must destroy first");
            mTextureId = GLHelper.genTextureExternal();
            Matrix.setIdentityM(mTexCoordsMatrix, 0);
        }
    }

    public void onDraw(int uniformTexture, int uniformMatrix, int textureSlot) {
        synchronized (mLock) {
            /*TODO
            try {
                while (!mFrameAvailable) mLock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            mFrameAvailable = false;
            */
            GLES20.glUniformMatrix4fv(uniformMatrix, 1, false, mTexCoordsMatrix, 0); glCheck();
            GLES20.glActiveTexture(textureSlot); glCheck();
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId); glCheck();
            GLES20.glUniform1i(uniformTexture, GLHelper.getTextureSlot(textureSlot)); glCheck();
        }
    }

    public void onDestroy() {
        synchronized (mLock) {
            try {
                if (mTextureId != -1) {
                    GLES20.glDeleteTextures(1, new int[]{mTextureId}, 0); glCheck();
                }
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            } finally {
                mTextureId = -1;
            }
        }
    }

}
