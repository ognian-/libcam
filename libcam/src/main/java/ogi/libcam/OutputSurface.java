package ogi.libcam;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.util.concurrent.atomic.AtomicBoolean;

import static ogi.libcam.GLHelper.assertTrue;
import static ogi.libcam.GLHelper.eglCheck;

public class OutputSurface {

    private static final String TAG = "LibCam";

    public interface Renderer {
        void onCreate();
        void onDraw(ExternalTexture texture);
        void onDestroy();
    }

    private final AtomicBoolean mStopThread = new AtomicBoolean();
    private final WaitResult mStopResult = new WaitResult();
    private final Renderer mRenderer;

    private final WaitResult mOutputSync = new WaitResult();
    private final WaitResult mInputSync = new WaitResult();

    private EGLConfig mEglConfig = null;
    private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface mEglSurface = EGL14.EGL_NO_SURFACE;

    private final Object mSurfaceLock = new Object();
    private SurfaceTexture mInputSurfaceTexture;
    private boolean mInputSurfaceTextureAttached;
    private Surface mInputSurface;
    private final ExternalTexture mExternalTexture = new ExternalTexture();

    private WaitResult mDetach;
    private WaitResult mAttach;

    public OutputSurface(Renderer renderer, Object window) {
        mRenderer = renderer;
        startThread(window, null);
    }

    public OutputSurface(Renderer renderer, Size size) {
        mRenderer = renderer;
        startThread(null, size);
    }

    public void release() {
        mStopThread.getAndSet(true);
        mStopResult.getResultNoThrows(false);
    }

    public Surface getSurface() {
        try {
            synchronized (mSurfaceLock) {
                while (mInputSurface == null && !mStopThread.get()) mSurfaceLock.wait();
                return mInputSurface;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void attachToGLContext(ExternalTexture texture) {
        synchronized (mSurfaceLock) {
            if (mInputSurfaceTextureAttached) throw new IllegalStateException("Must detach first");
            texture.onCreate(mInputSurfaceTexture);
        }
    }

    public void detachFromGLContext() {
        WaitResult detach = new WaitResult();
        synchronized (mSurfaceLock) {
            mDetach = detach;
            mSurfaceLock.notifyAll();
        }
        detach.getResultNoThrows(false);
    }

    public void attachToGLContext() {
        WaitResult attach = new WaitResult();
        synchronized (mSurfaceLock) {
            mAttach = attach;
            mSurfaceLock.notifyAll();
        }
        attach.getResultNoThrows(false);
    }

    public void updateTexImage() {
        synchronized (mSurfaceLock) {
            if (mInputSurfaceTexture != null) {
                mInputSurfaceTexture.updateTexImage();
            }
        }
    }

    private void startThread(final Object window, final Size pbufferSize) {
        final WaitResult created = new WaitResult();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        init(window, pbufferSize);
                        created.setResult(null);
                    } catch (Exception e) {
                        created.setException(e);
                        return;
                    }
                    mRenderer.onCreate();

                    mInputSync.setResult(null);
                    mOutputSync.setResult(null);

                    loop();
                } finally {
                    try {
                        mRenderer.onDestroy();
                    } finally {
                        destroy();
                    }
                }
            }
        }).start();
        created.getResultNoThrows(false);
    }

    private void loop() {
        while (!mStopThread.get()) {

            try {
                synchronized (mSurfaceLock) {
                    while (!mInputSurfaceTextureAttached && !haveEvents()) mSurfaceLock.wait();
                    if (handleEvents()) continue;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            mInputSync.getResultNoThrows(false);
            mInputSurfaceTexture.updateTexImage();

            mOutputSync.getResultNoThrows(false);

            mRenderer.onDraw(mExternalTexture);
            EGL14.eglSwapBuffers(mEglDisplay, mEglSurface); eglCheck();
        }
    }

    private boolean haveEvents() {
        return mDetach != null || mAttach != null;
    }

    private boolean handleEvents() {
        boolean handled = false;
        if (mDetach != null) {
            mExternalTexture.onDestroy(mInputSurfaceTexture);
            mInputSurfaceTextureAttached = false;
            mDetach.setResult(null);
            mDetach = null;
            handled = true;
        }
        if (mAttach != null) {
            mExternalTexture.onCreate(mInputSurfaceTexture);
            mInputSurfaceTextureAttached = true;
            mAttach.setResult(null);
            mAttach = null;
            handled = true;
        }
        return handled;
    }

    private void init(Object window, Size pbufferSize) {
        final EglAttribs attribs = new EglAttribs()
                .setRenderableType(EGL14.EGL_OPENGL_ES2_BIT)
                .setRedSize(8).setGreenSize(8).setBlueSize(8);
        try {
            createContext(attribs);
            if (window != null) {
                createWindowSurface(window);
            } else if (pbufferSize != null) {
                createPbufferSurface(pbufferSize);
            } else {
                throw new RuntimeException("No surface specified");
            }
            makeCurrent();
            createSurface(null);
        } catch (Exception e) {
            destroy();
            throw e;
        }
    }

    private void createContext(EglAttribs attribs) {
        assertTrue(mEglDisplay == EGL14.EGL_NO_DISPLAY);
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY); eglCheck();
        assertTrue(mEglDisplay != EGL14.EGL_NO_DISPLAY && mEglDisplay != null);

        final int[] version = new int[2];
        final boolean inited = EGL14.eglInitialize(mEglDisplay, version, 0, version, 1); eglCheck();
        assertTrue(inited);
        Log.v(TAG, "EGL version: " + version[0] + '.' + version[1]);

        assertTrue(mEglConfig == null);
        mEglConfig = GLHelper.chooseConfig(mEglDisplay, attribs);
        assertTrue(mEglConfig != null);

        final int[] attribList = {GLHelper.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE };
        assertTrue(mEglContext == EGL14.EGL_NO_CONTEXT);
        mEglContext = EGL14.eglCreateContext(mEglDisplay, mEglConfig, EGL14.EGL_NO_CONTEXT, attribList, 0); eglCheck();
        assertTrue(mEglContext != EGL14.EGL_NO_CONTEXT && mEglContext != null);
    }

    private void createPbufferSurface(Size size) {
        final int[] surfaceAttribs = {
            EGL14.EGL_WIDTH, size.getWidth(),
            EGL14.EGL_HEIGHT, size.getHeight(),
            EGL14.EGL_NONE
        };
        assertTrue(mEglSurface == EGL14.EGL_NO_SURFACE);
        mEglSurface = EGL14.eglCreatePbufferSurface(mEglDisplay, mEglConfig, surfaceAttribs, 0); eglCheck();
        assertTrue(mEglSurface != EGL14.EGL_NO_SURFACE && mEglSurface != null);
    }

    private void createWindowSurface(Object window) {
        assertTrue(mEglSurface == EGL14.EGL_NO_SURFACE);
        mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, window, null, 0); eglCheck();
        assertTrue(mEglSurface != EGL14.EGL_NO_SURFACE && mEglSurface != null);
    }

    private void makeCurrent() {
        final boolean current = EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext); eglCheck();
        assertTrue(current);
    }

    private void createSurface(Size size) {
        synchronized (mSurfaceLock) {
            mInputSurfaceTexture = mExternalTexture.onCreate(null);
            if (size != null) {
                mInputSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
            }
            mInputSurface = new Surface(mInputSurfaceTexture);
            mSurfaceLock.notifyAll();
        }
    }

    private void destroy() {
        try {
            synchronized (mSurfaceLock) {
                try {
                    mExternalTexture.onDestroy(mInputSurfaceTexture);
                } finally {
                    try {
                        if (mInputSurfaceTexture != null) {
                            mInputSurfaceTexture.release();
                        }
                    } finally {
                        mInputSurfaceTexture = null;
                        mInputSurface = null;
                        mSurfaceLock.notifyAll();
                    }
                }

            }
        } finally {
            try {
                mEglConfig = null;
                if (mEglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(mEglDisplay, mEglSurface); eglCheck();
                    mEglSurface = EGL14.EGL_NO_SURFACE;
                }
            } finally {
                try {
                    if (mEglContext != EGL14.EGL_NO_CONTEXT) {
                        EGL14.eglDestroyContext(mEglDisplay, mEglContext); eglCheck();
                        mEglContext = EGL14.EGL_NO_CONTEXT;
                    }
                } finally {
                    try {
                        if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
                            EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT); eglCheck();
                            EGL14.eglTerminate(mEglDisplay); eglCheck();
                            mEglDisplay = EGL14.EGL_NO_DISPLAY;
                        }
                    } finally {
                        mStopResult.setResult(null);
                    }
                }
            }
        }
    }

}

