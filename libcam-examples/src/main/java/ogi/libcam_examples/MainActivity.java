package ogi.libcam_examples;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.IOException;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import ogi.libcam.AttachEvents;
import ogi.libcam.CaptureService;
import ogi.libcam.PermissionsFragment;
import ogi.libcam.PreviewRenderer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LibCamExamples";

    private boolean mPermissions = false;
    private CaptureService.CaptureServiceBinder mCapture;
    private final Object mCaptureLock = new Object();

    private GLSurfaceView mGL1;
    private GLSurfaceView mGL2;
    private GLSurfaceView mGL3;
    private GLSurfaceView mGL4;
    private GLSurfaceView mCurrent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mGL1 = (GLSurfaceView)findViewById(R.id.gl1);
        mGL2 = (GLSurfaceView)findViewById(R.id.gl2);
        mGL3 = (GLSurfaceView)findViewById(R.id.gl3);
        mGL4 = (GLSurfaceView)findViewById(R.id.gl4);

        setupGLSurfaceView(mGL1);
        setupGLSurfaceView(mGL2);
        setupGLSurfaceView(mGL3);
        setupGLSurfaceView(mGL4);

        PermissionsFragment.attach(MainActivity.this, mPermissionsListener, "cam_perm");

    }

    private void setupGLSurfaceView(final GLSurfaceView view) {
        view.setEGLContextClientVersion(2);

        final AttachEvents attachEvents = new AttachEvents();
        final AttachEvents.Callback callback = new AttachEvents.Callback() {
            @Override
            public void onAttach() {
                mCapture.attachAnotherContext();
            }

            @Override
            public void onDetach() {
                mCapture.attachOriginalContext();
            }
        };

        view.setTag(attachEvents);

        try {
            view.setRenderer(new PreviewRenderer(getAssets()) {

                @Override
                public void onDrawFrame(GL10 gl) {
                    if (mCapture != null) {
                        attachEvents.handleEvents(callback);
                        if (attachEvents.isAttached()) {
                            onDraw(mCapture.getTexture());
                        }
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        view.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrent == view) {
                    attachEvents.detach();
                    mCurrent = null;
                } else if (mCurrent != null) {
                    ((AttachEvents)mCurrent.getTag()).detach();
                    attachEvents.attach();
                    mCurrent = view;
                } else {
                    attachEvents.attach();
                    mCurrent = view;
                }
            }
        });

        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                startActivity(intent);
                return true;
            }
        });
    }

    private void bindService() {
        if (mCapture != null) return;
        if (!mPermissions) return;
        bindService(new Intent(MainActivity.this, CaptureService.class), mCaptureConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        synchronized (mCaptureLock) {
            bindService();
        }
        super.onStart();
    }

    @Override
    protected void onStop() {
        synchronized (mCaptureLock) {
            if (mCapture != null) {
                unbindService(mCaptureConnection);
            }
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGL1.onResume();
        mGL2.onResume();
        mGL3.onResume();
        mGL4.onResume();
    }

    @Override
    protected void onPause() {
        mGL1.onPause();
        mGL2.onPause();
        mGL3.onPause();
        mGL4.onPause();
        super.onPause();
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
            synchronized (mCaptureLock) {
                mPermissions = true;
                bindService();
            }
        }

        @Override
        public void onCameraPermissionsDenied() {
            synchronized (mCaptureLock) {
                mPermissions = false;
            }
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
