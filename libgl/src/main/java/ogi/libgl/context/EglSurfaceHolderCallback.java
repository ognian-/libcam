package ogi.libgl.context;

import android.view.SurfaceHolder;

import ogi.libgl.BaseTextureInput;

public class EglSurfaceHolderCallback implements SurfaceHolder.Callback2, EglContextThread.Callback {

    private final String mName;
    private final EglContextThread.Callback mCallback;
    private final Object mLock = new Object();
    private EglContextThread mThread;

    public EglSurfaceHolderCallback(String name, EglContextThread.Callback callback) {
        mName = name;
        mCallback = callback;
    }

    @Override
    public void surfaceRedrawNeeded(SurfaceHolder surfaceHolder) {
        //TODO
    }

    @Override
    public void surfaceRedrawNeededAsync(SurfaceHolder holder, Runnable drawingFinished) {
        //TODO
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        synchronized (mLock) {
            if (mThread != null) throw new IllegalStateException("Not destroyed yet");
            mThread = new EglContextThread(mName, this, surfaceHolder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        //TODO

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        synchronized (mLock) {
            if (mThread == null) throw new IllegalStateException("Not created yet");
            mThread.release();
            mThread = null;
        }
    }

    @Override
    public void onCreate() {
        mCallback.onCreate();
    }

    @Override
    public void onDraw(BaseTextureInput... inputs) {
        mCallback.onDraw(inputs);
    }

    @Override
    public void onDestroy() {
        mCallback.onDestroy();
    }
}
