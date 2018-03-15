package ogi.libcam;

import android.content.res.AssetManager;
import android.opengl.GLES20;
import android.view.Surface;

import java.io.IOException;

public class BaseRenderer implements EglContextThread.Renderer {

    private static final String TAG = "LibCam";

    private final SurfaceTextureWrapper mSurfaceTexture;
    private final Pass mBlit;

    private final Object mLock = new Object();
    private boolean mAttached = false;
    private WaitResult mAttach = null;
    private WaitResult mDetach = null;

    public BaseRenderer(AssetManager am) throws IOException {
        mSurfaceTexture = new SurfaceTextureWrapper();
        mBlit = new Pass(GLHelper.loadShaderSource(am, "shaders/blit.vert"), GLHelper.loadShaderSource(am, "shaders/blit.frag"));
    }

    public Surface getSurface() {
        return mSurfaceTexture.getSurface();
    }

    public ExternalTexture getTexture(boolean update) {
        return mSurfaceTexture.getTexture(update);
    }

    public void attachAnother() {
        mSurfaceTexture.attach();
    }

    public void detachAnother() {
        mSurfaceTexture.detach();
    }

    public void attach() {
        WaitResult attach = null;
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
        WaitResult detach = null;
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
            mSurfaceTexture.onCreate();
            mAttached = true;
        }
        mBlit.onCreate();
    }

    @Override
    public boolean onDraw() {
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
                mBlit.onDraw(mSurfaceTexture.getTexture(true), GLES20.GL_TEXTURE0);
            }
        }
        return true;
    }

    @Override
    public void onDestroy() {
        mBlit.onDestroy();
        synchronized (mLock) {
            mSurfaceTexture.onDestroy();
            mAttached = false;
        }
    }

}
