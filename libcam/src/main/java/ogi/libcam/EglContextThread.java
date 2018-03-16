package ogi.libcam;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.util.Log;
import android.util.Size;

import java.util.concurrent.atomic.AtomicBoolean;

import static ogi.libcam.GLHelper.assertTrue;
import static ogi.libcam.GLHelper.eglCheck;

public class EglContextThread {

    private static final String TAG = "LibCam";

    public interface Renderer {
        void onCreate();
        void onDraw(BaseTextureInput ... inputs);
        void onDestroy();
    }

    private final AtomicBoolean mStopThread = new AtomicBoolean();
    private final WaitResult mStopResult = new WaitResult();
    private final Renderer mRenderer;

    private EGLConfig mEglConfig = null;
    private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface mEglSurface = EGL14.EGL_NO_SURFACE;

    public EglContextThread(Renderer renderer, Object window) {
        mRenderer = renderer;
        startThread(window, null);
    }

    public EglContextThread(Renderer renderer, Size size) {
        mRenderer = renderer;
        startThread(null, size);
    }

    public void release() {
        mStopThread.set(true);
        mStopResult.getResult();
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

                    loop();
                } catch (Throwable e) {
                    e.printStackTrace();
                    throw e;
                } finally {
                    try {
                        mRenderer.onDestroy();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        throw e;
                    } finally {
                        destroy();
                    }
                }
            }
        }, "OutputSurface").start();
        created.getResult();
    }

    private void loop() {
        while (!mStopThread.get()) {
            mRenderer.onDraw();
            EGL14.eglSwapBuffers(mEglDisplay, mEglSurface); eglCheck();
        }
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

    private void destroy() {
        try {
            mEglConfig = null;
            if (mEglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(mEglDisplay, mEglSurface); eglCheck();
                mEglSurface = EGL14.EGL_NO_SURFACE;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (mEglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(mEglDisplay, mEglContext); eglCheck();
                    mEglContext = EGL14.EGL_NO_CONTEXT;
                }
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            } finally {
                try {
                    if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
                        EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT); eglCheck();
                        EGL14.eglTerminate(mEglDisplay); eglCheck();
                        mEglDisplay = EGL14.EGL_NO_DISPLAY;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    throw e;
                } finally {
                    mStopResult.setResult(null);
                }
            }
        }
    }

}

