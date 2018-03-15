package ogi.libcam_examples;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.IOException;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ogi.libcam.CaptureService;
import ogi.libcam.DestroyableGLSurfaceView;
import ogi.libcam.ExternalTexture;
import ogi.libcam.GLHelper;
import ogi.libcam.Pass;
import ogi.libcam.PermissionsFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LibCamExamples";

    private CaptureService.CaptureServiceBinder mCapture;
    private final Object mCaptureLock = new Object();

    private GLSurfaceView mCurrent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        setupGLSurfaceView((GLSurfaceView)findViewById(R.id.gl1));
        setupGLSurfaceView((GLSurfaceView)findViewById(R.id.gl2));
        setupGLSurfaceView((GLSurfaceView)findViewById(R.id.gl3));
        setupGLSurfaceView((GLSurfaceView)findViewById(R.id.gl4));

        PermissionsFragment.attach(MainActivity.this, mPermissionsListener, "cam_perm");

    }

    private void setupGLSurfaceView(final GLSurfaceView view) {
        view.setEGLContextClientVersion(2);
        view.setRenderer(new DestroyableGLSurfaceView.DestroyableRenderer() {

            final Pass blit;

            {
                try {
                    blit = new Pass(GLHelper.loadShaderSource(getAssets(), "shaders/blit.vert"),
                            GLHelper.loadShaderSource(getAssets(), "shaders/blit.frag"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
                blit.onCreate();
            }

            @Override
            public void onSurfaceChanged(GL10 gl10, int i, int i1) {
            }

            @Override
            public void onDrawFrame(GL10 gl10) {
                synchronized (mCaptureLock) {
                    try {
                        while (mCapture == null) mCaptureLock.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    ExternalTexture texture = mCapture.getTexture(true);
                    if (texture != null) {
                        blit.onDraw(texture, GLES20.GL_TEXTURE0);
                    }
                }
            }

            @Override
            public void onDestroy() {
                blit.onDestroy();
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
                    }
                };

                if (mCurrent != null) {
                    mCurrent.queueEvent(new Runnable() {
                        @Override
                        public void run() {
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
