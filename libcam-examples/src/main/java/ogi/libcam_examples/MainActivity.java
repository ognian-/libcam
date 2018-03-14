package ogi.libcam_examples;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ogi.libcam.BaseRenderer;
import ogi.libcam.CaptureService;
import ogi.libcam.DestroyableGLSurfaceView;
import ogi.libcam.ExternalTexture;
import ogi.libcam.PermissionsFragment;

import static ogi.libcam.GLHelper.glCheck;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LibCamExamples";

    private CaptureService.CaptureServiceBinder mCapture;
    private final Object mCaptureLock = new Object();

    private GLSurfaceView mCurrent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        setupGLSurfaceView((GLSurfaceView)findViewById(R.id.gl1), Color.RED);
        setupGLSurfaceView((GLSurfaceView)findViewById(R.id.gl2), Color.GREEN);
        setupGLSurfaceView((GLSurfaceView)findViewById(R.id.gl3), Color.BLUE);
        setupGLSurfaceView((GLSurfaceView)findViewById(R.id.gl4), Color.CYAN);


        PermissionsFragment.attach(MainActivity.this, mPermissionsListener, "cam_perm");

    }

    private void setupGLSurfaceView(final GLSurfaceView view, final int color) {
        final AtomicBoolean active = new AtomicBoolean();
        view.setTag(active);
        view.setEGLContextClientVersion(2);
        view.setRenderer(new DestroyableGLSurfaceView.DestroyableRenderer() {

            final BaseRenderer renderer;

            {
                try {
                    renderer = new BaseRenderer(getAssets());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
                renderer.onCreate();
            }

            @Override
            public void onSurfaceChanged(GL10 gl10, int i, int i1) {
            }

            @Override
            public void onDrawFrame(GL10 gl10) {
                if (active.get()) {
                    GLES20.glClearColor(Color.red(color) / 255.0f, Color.green(color) / 255.0f, Color.blue(color) / 255.0f, 1); glCheck();
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT); glCheck();
                    renderer.onDraw();
                } else {
                    GLES20.glClearColor(0, 0, 0, 1); glCheck();
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT); glCheck();
                }
            }

            @Override
            public void onDestroy() {
                renderer.onDestroy();
            }
        });
        view.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrent == view) return;
                final Runnable activate = new Runnable() {
                    @Override
                    public void run() {
                        mCapture.attachAnotherContext();
                        active.set(true);
                    }
                };
                if (mCurrent != null) {
                    final AtomicBoolean activeCurrent = (AtomicBoolean) mCurrent.getTag();
                    mCurrent.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            activeCurrent.set(false);
                            mCapture.attachOriginalContext();
                            view.queueEvent(activate);
                        }
                    });
                } else {
                    view.queueEvent(activate);
                }
                mCurrent = view;
            }
        });
    }

    private final CaptureService.Listener mCaptureListener = new CaptureService.Listener() {
        @Override
        public void onCameraCaptureSessionConfigured(String cameraId) {
            mCapture.startPreview();
        }

        @Override
        public void onCameraError(String cameraId) {
            throw new RuntimeException();
        }

        @Override
        public void onPreviewStarted(String cameraId) {
        }
    };

    private final ServiceConnection mCaptureConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (mCaptureLock) {
                mCapture = (CaptureService.CaptureServiceBinder) iBinder;
                mCapture.openCamera(mCapture.getFrontCameraId(), mCaptureListener);
                mCaptureLock.notifyAll();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (mCaptureLock) {
                mCapture = null;
            }
        }
    };

    private final PermissionsFragment.Listener mPermissionsListener = new PermissionsFragment.Listener() {

        @Override
        public void onCameraPermissionsGranted() {
            bindService(new Intent(MainActivity.this, CaptureService.class), mCaptureConnection, Context.BIND_AUTO_CREATE);
        }

        @Override
        public void onCameraPermissionsDenied() {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("This app can't run without camera permissions")
                    .setCancelable(false)
                    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    }).show();
        }

        @Override
        public void showRationale(List<String> permissions, final Runnable done) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("Please consider granting camera permissions to this app")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            done.run();
                        }
                    }).setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    }).show();
        }

    };
}
