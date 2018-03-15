package ogi.libcam;

import android.graphics.SurfaceTexture;
import android.opengl.Matrix;
import android.view.Surface;

public class SurfaceTextureWrapper {

    private long mThreadId = -1;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private final BaseTextureInput mTexture = new TextureExternalInput();
    private int mTextureId = -1;
    private final Object mLock = new Object();
    private final float[] mTexCoordsMatrix = new float[16];

    public void onCreate() {
        GLHelper.signalOnCreated(this);
        synchronized (mLock) {
            if (mSurfaceTexture != null) throw new IllegalStateException("Already created");
            mThreadId = Thread.currentThread().getId();
            mTextureId = mTexture.onCreate();
            mSurfaceTexture = new SurfaceTexture(mTextureId);
            mSurface = new Surface(mSurfaceTexture);
            mSurfaceTexture.setOnFrameAvailableListener(mListener);
            Matrix.setIdentityM(mTexCoordsMatrix, 0);
            mLock.notifyAll();
        }
    }

    public void attach() {
        synchronized (mLock) {
            if (mThreadId != -1) throw new IllegalStateException("Must detach first");
            mThreadId = Thread.currentThread().getId();
            mTextureId = mTexture.onCreate();
            mSurfaceTexture.attachToGLContext(mTextureId);
        }
    }

    public void detach() {
        synchronized (mLock) {
            if (mThreadId != Thread.currentThread().getId()) throw new IllegalStateException("Called on wrong thread");
            mThreadId = -1;
            mTextureId = -1;
            mSurfaceTexture.detachFromGLContext();
            mTexture.onDestroy();
        }
    }

    public void onDestroy() {
        GLHelper.signalOnDestroyed(this);
        synchronized (mLock) {
            if (mThreadId != -1 && mThreadId != Thread.currentThread().getId()) throw new IllegalStateException("Called on wrong thread");
            mThreadId = -1;
            if (mSurfaceTexture != null) {
                mSurfaceTexture.release();
                mSurfaceTexture = null;
            }
            mSurface = null;
            if (mTextureId != -1) {
                mTexture.onDestroy();
                mTextureId = -1;
            }
        }
    }

    public void onDestroyForced() {
        GLHelper.signalOnDestroyed(this);
        synchronized (mLock) {
            mThreadId = -1;
            if (mSurfaceTexture != null) {
                mSurfaceTexture.release();
                mSurfaceTexture = null;
            }
            mSurface = null;
            if (mTextureId != -1) {
                mTexture.onDestroy();
                mTextureId = -1;
            }
        }
    }

    public Surface getSurface() {
        try {
            synchronized (mLock) {
                while (mSurface == null) mLock.wait();
                return mSurface;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public BaseTextureInput getTexture(boolean update) {
        synchronized (mLock) {
            if (mThreadId != Thread.currentThread().getId()) return null;
            if (update) {
                mSurfaceTexture.updateTexImage();
            }
            return mTexture;
        }
    }

    private final SurfaceTexture.OnFrameAvailableListener mListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            synchronized (mLock) {
                surfaceTexture.getTransformMatrix(mTexCoordsMatrix);
                mTexture.onFrameAvailable(mTexCoordsMatrix);
            }
        }
    };

}
