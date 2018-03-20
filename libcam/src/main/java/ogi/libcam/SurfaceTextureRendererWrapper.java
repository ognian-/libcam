package ogi.libcam;

import android.util.Size;
import android.view.Surface;

import ogi.libgl.BaseTextureInput;
import ogi.libgl.context.EglContextThread;

public class SurfaceTextureRendererWrapper implements EglContextThread.Callback {

    private static final String TAG = "LibCam";

    private final TextureExternalInput mTexture;
    private final EglContextThread.Callback mCallback;

    private final Object mLock = new Object();
    private final AttachEvents mAttachEvents = new AttachEvents();
    private final AttachEvents.Callback mAttachEventsCallback = new AttachEvents.Callback() {
        @Override
        public void onAttach() {
            mTexture.onCreate();
        }

        @Override
        public void onDetach() {
            mTexture.onDestroy();
        }
    };
    private Size mDefaultBufferSize = new Size(-1, -1);

    public SurfaceTextureRendererWrapper(EglContextThread.Callback callback) {
        mTexture = new TextureExternalInput();
        mCallback = callback;
    }

    public void setDefaultBufferSize(Size size) {
        synchronized (mLock) {
            mDefaultBufferSize = size;
        }
    }

    public Surface getSurface() {
        return mTexture.getSurface();
    }

    public BaseTextureInput getTexture() {
        return mTexture;
    }

    public void attachAnother() {
        mTexture.onCreate();
    }

    public void detachAnother() {
        mTexture.onDestroy();
    }

    public void attach() {
        mAttachEvents.attach();
    }

    public void detach() {
        mAttachEvents.detach();
    }

    @Override
    public void onCreate() {
        synchronized (mLock) {
            mTexture.onCreate();
            mTexture.setDefaultBufferSize(mDefaultBufferSize);
            mAttachEvents.setAttached(true);
        }
        mCallback.onCreate();
    }

    @Override
    public void onDraw(BaseTextureInput ... inputs) {
        mAttachEvents.handleEvents(mAttachEventsCallback);
        if (mAttachEvents.isAttached()) {
            mCallback.onDraw(mTexture);
        }
    }

    @Override
    public void onDestroy() {
        mCallback.onDestroy();
        synchronized (mLock) {
            if (mAttachEvents.isAttached()) {
                mTexture.onDestroy();
                mAttachEvents.setAttached(false);
            }
            mDefaultBufferSize = new Size(-1, -1);
        }
    }

}
