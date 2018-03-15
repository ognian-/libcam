package ogi.libcam;

import android.opengl.GLES20;
import android.opengl.Matrix;

import static ogi.libcam.GLHelper.glCheck;

public abstract class BaseTextureInput {

    private static final String TAG = "LibCam";

    private int mTextureId = -1;
    private final Object mLock = new Object();
    private final float[] mTexCoordsMatrix = new float[16];
    private boolean mFrameAvailable;

    protected abstract int genTextureId();
    protected abstract int getTarget();

    public void onFrameAvailable(float[] texCoordMatrix) {
        synchronized (mLock) {
            System.arraycopy(texCoordMatrix, 0, mTexCoordsMatrix, 0, 16);
            mFrameAvailable = true;
            mLock.notifyAll();
        }
    }

    public int onCreate() {
        GLHelper.signalOnCreated(this);
        synchronized (mLock) {
            if (mTextureId != -1) throw new IllegalStateException("Must destroy first");
            mTextureId = genTextureId();
            Matrix.setIdentityM(mTexCoordsMatrix, 0);
            return mTextureId;
        }
    }

    public void onDraw(int uniformTexture, int uniformMatrix, int textureSlot) {
        synchronized (mLock) {
            try {
                while (mTextureId != -1 && !mFrameAvailable) mLock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            mFrameAvailable = false;
            if (mTextureId == -1) return;
            GLES20.glUniformMatrix4fv(uniformMatrix, 1, false, mTexCoordsMatrix, 0); glCheck();
            GLES20.glActiveTexture(textureSlot); glCheck();
            GLES20.glBindTexture(getTarget(), mTextureId); glCheck();
            GLES20.glUniform1i(uniformTexture, GLHelper.getTextureSlot(textureSlot)); glCheck();
        }
    }

    public void onDestroy() {
        GLHelper.signalOnDestroyed(this);
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
                mLock.notifyAll();
            }
        }
    }

}
