package ogi.libcam;

import android.view.Surface;

public class SurfaceTextureRendererWrapper implements EglContextThread.Renderer {

    private static final String TAG = "LibCam";

    private final SurfaceTextureWrapper mSurfaceTexture;
    private final EglContextThread.Renderer mRenderer;

    private final Object mLock = new Object();
    private boolean mAttached = false;
    private WaitResult mAttach = null;
    private WaitResult mDetach = null;

    public SurfaceTextureRendererWrapper(EglContextThread.Renderer renderer) {
        mSurfaceTexture = new SurfaceTextureWrapper();
        mRenderer = renderer;
    }

    public Surface getSurface() {
        return mSurfaceTexture.getSurface();
    }

    public BaseTextureInput getTexture(boolean update) {
        return mSurfaceTexture.getTexture(update);
    }

    public void attachAnother() {
        mSurfaceTexture.attach();
    }

    public void detachAnother() {
        mSurfaceTexture.detach();
    }

    public void attach() {
        WaitResult attach;
        synchronized (mLock) {
            if (mAttach != null) {
                attach = mAttach;
            } else {
                attach = mAttach = new WaitResult();
            }
        }
        attach.getResult();
    }

    public void detach() {
        WaitResult detach;
        synchronized (mLock) {
            if (mDetach != null) {
                detach = mDetach;
            } else {
                detach = mDetach = new WaitResult();
            }
        }
        detach.getResult();
    }

    @Override
    public void onCreate() {
        GLHelper.signalOnCreated(this);
        synchronized (mLock) {
            mSurfaceTexture.onCreate();
            mAttached = true;
        }
        mRenderer.onCreate();
    }

    @Override
    public void onDraw(BaseTextureInput ... inputs) {
        synchronized (mLock) {
            if (mAttach != null) {
                mSurfaceTexture.attach();
                mAttach.setResult(null);
                mAttach = null;
                mAttached = true;
            }
            if (mDetach != null) {
                mSurfaceTexture.detach();
                mDetach.setResult(null);
                mDetach = null;
                mAttached = false;
            }
            if (mAttached) {
                mRenderer.onDraw(mSurfaceTexture.getTexture(true));
            }
        }
    }

    @Override
    public void onDestroy() {
        GLHelper.signalOnDestroyed(this);
        mRenderer.onDestroy();
        synchronized (mLock) {
            if (mAttached) {
                mSurfaceTexture.onDestroy();
                mAttached = false;
            } else {
                mSurfaceTexture.onDestroyForced();
            }
        }
    }

}
