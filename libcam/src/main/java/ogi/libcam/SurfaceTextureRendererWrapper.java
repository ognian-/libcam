package ogi.libcam;

import android.util.Size;
import android.view.Surface;

import ogi.libgl.BaseTextureInput;
import ogi.libgl.context.EglContextThread;
import ogi.libgl.util.WaitResult;

public class SurfaceTextureRendererWrapper implements EglContextThread.Callback {

    private static final String TAG = "LibCam";

    private final SurfaceTextureWrapper mSurfaceTexture;
    private final EglContextThread.Callback mCallback;

    private final Object mLock = new Object();
    private boolean mAttached = false;
    private WaitResult mAttach = null;
    private WaitResult mDetach = null;
    private Size mDefaultBufferSize = new Size(-1, -1);

    public SurfaceTextureRendererWrapper(EglContextThread.Callback callback) {
        mSurfaceTexture = new SurfaceTextureWrapper();
        mCallback = callback;
    }

    public void setDefaultBufferSize(Size size) {
        synchronized (mLock) {
            mDefaultBufferSize = size;
        }
    }

    public Surface getSurface() {
        return mSurfaceTexture.getSurface();
    }

    public BaseTextureInput getTexture() {
        return mSurfaceTexture.getTexture();
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
        synchronized (mLock) {
            mSurfaceTexture.onCreate(mDefaultBufferSize);
            mAttached = true;
        }
        mCallback.onCreate();
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
                mCallback.onDraw(mSurfaceTexture.getTexture());
            }
        }
    }

    @Override
    public void onDestroy() {
        mCallback.onDestroy();
        synchronized (mLock) {
            if (mAttached) {
                mSurfaceTexture.onDestroy();
                mAttached = false;
            } else {
                mSurfaceTexture.onDestroyForced();
            }
            mDefaultBufferSize = new Size(-1, -1);
        }
    }

}
