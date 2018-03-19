package ogi.libcam;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import static ogi.libgl.util.GLHelper.eglCheck;

public class DestroyableGLSurfaceView extends GLSurfaceView {

    private static final String TAG = "LibCam";

    public interface DestroyableRenderer extends Renderer {
        void onDestroy();
    }

    private Renderer mRenderer;
    private EGLWindowSurfaceFactory mFactory;
    private final Object mLock = new Object();

    public DestroyableGLSurfaceView(Context context) {
        super(context);
        init();
    }

    public DestroyableGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        super.setEGLWindowSurfaceFactory(mFactoryImpl);
    }

    @Override
    public void setRenderer(final Renderer renderer) {
        synchronized (mLock) {
            mRenderer = renderer;
        }
        super.setRenderer(new Renderer() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                renderer.onSurfaceCreated(gl, config);
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                renderer.onSurfaceChanged(gl, width, height);
            }

            @Override
            public void onDrawFrame(GL10 gl) {
                renderer.onDrawFrame(gl);
            }
        });
    }

    @Override
    public void setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory factory) {
        synchronized (mLock) {
            mFactory = factory;
        }
    }

    private final EGLWindowSurfaceFactory mFactoryImpl = new EGLWindowSurfaceFactory() {

        @Override
        public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig config, Object nativeWindow) {
            synchronized (mLock) {
                if (mFactory != null) {
                    return mFactory.createWindowSurface(egl, display, config, nativeWindow);
                } else {
                    EGLSurface result = null;
                    try {
                        result = egl.eglCreateWindowSurface(display, config, nativeWindow, null); eglCheck();
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "eglCreateWindowSurface", e);
                    }
                    return result;
                }
            }
        }

        @Override
        public void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface) {
            synchronized (mLock) {
                if (mRenderer instanceof DestroyableRenderer) {
                    ((DestroyableRenderer)mRenderer).onDestroy();
                }
                if (mFactory != null) {
                    mFactory.destroySurface(egl, display, surface);
                } else {
                    egl.eglDestroySurface(display, surface); eglCheck();
                }
            }
        }

    };
}
