package ogi.libgl;

import android.opengl.GLES20;

import java.io.Closeable;

import ogi.libgl.util.CheckThread;
import ogi.libgl.util.GLHelper;

import static ogi.libgl.util.GLHelper.glCheck;

public abstract class BaseTextureInput implements Closeable {

    private static final String TAG = "LibGL";

    protected final CheckThread mCheckThread = new CheckThread();
    private int mTextureId = -1;
    private final float[] mTexCoordsMatrix = new float[16];

    protected abstract int genTextureId();
    protected abstract int getTarget();
    protected abstract void updateTexImage();
    protected abstract void getTexCoordsMatrix(float[] matrix);
    protected abstract void releaseTexImage();

    protected final int getTextureId() {
        mCheckThread.check();
        return mTextureId;
    }

    public void onCreate() {
        mCheckThread.init();
        if (mTextureId != -1) throw new IllegalStateException("Must destroy first");
        mTextureId = genTextureId();
    }

    public void onDraw(int uniformTexture, int uniformMatrix, int textureSlot) {
        mCheckThread.check();
        updateTexImage();
        getTexCoordsMatrix(mTexCoordsMatrix);
        GLES20.glUniformMatrix4fv(uniformMatrix, 1, false, mTexCoordsMatrix, 0); glCheck();
        GLES20.glActiveTexture(textureSlot); glCheck();
        GLES20.glBindTexture(getTarget(), mTextureId); glCheck();
        GLES20.glUniform1i(uniformTexture, GLHelper.getTextureSlot(textureSlot)); glCheck();
    }

    public void onDoneDraw() {
        mCheckThread.check();
        releaseTexImage();
    }

    public void onDestroy() {
        mCheckThread.deinit(false);
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

    @Override
    public void close() {
    }

}
