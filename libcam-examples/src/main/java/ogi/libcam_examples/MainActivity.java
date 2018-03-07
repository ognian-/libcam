package ogi.libcam_examples;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ogi.libcam.BaseRenderer;
import ogi.libcam.CaptureService;
import ogi.libcam.ExternalTexture;
import ogi.libcam.GLHelper;
import ogi.libcam.PermissionsFragment;
import ogi.libcam.WaitResult;

import static ogi.libcam.GLHelper.glCheck;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LibCamExamples";

    private GLSurfaceView mGLSurface;

    private CaptureService.CaptureServiceBinder mCapture;
    private final Object mCaptureLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        mGLSurface = new GLSurfaceView(this);
        mGLSurface.setEGLContextClientVersion(2);
        mGLSurface.setRenderer(new GLSurfaceView.Renderer() {

            final BaseRenderer renderer;
            final ExternalTexture texture = new ExternalTexture();

            {
                try {
                    renderer = new BaseRenderer(getAssets());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
                Log.d(TAG, "onSurfaceCreated");

                try {
                    synchronized (mCaptureLock) {
                        while (mCapture == null) mCaptureLock.wait();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                mCapture.attachToGLContext(texture);

                renderer.onCreate();

            }

            @Override
            public void onSurfaceChanged(GL10 gl10, int i, int i1) {
                Log.d(TAG, "onSurfaceChanged");

            }

            @Override
            public void onDrawFrame(GL10 gl10) {
                mCapture.updateTexImage();
                renderer.onDraw(texture);
            }
        });
        mGLSurface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                setContentView(mGLSurface);
            }
        }, 1000);

        PermissionsFragment.attach(MainActivity.this, mPermissionsListener, "cam_perm");

    }

    final CaptureService.Listener mCaptureListener = new CaptureService.Listener() {
        @Override
        public void onCameraCaptureSessionConfigured(String cameraId) {
            Log.d(TAG, "onCameraCaptureSessionConfigured");
            mCapture.startPreview();
        }

        @Override
        public void onCameraError(String cameraId) {
            throw new RuntimeException();
        }

        @Override
        public void onPreviewStarted(String cameraId) {
            Log.d(TAG, "onPreviewStarted");
        }
    };

    final ServiceConnection mCaptureConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected");
            synchronized (mCaptureLock) {
                mCapture = (CaptureService.CaptureServiceBinder) iBinder;
                mCapture.openCamera(mCapture.getFrontCameraId(), mCaptureListener);
                mCaptureLock.notifyAll();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");
            synchronized (mCaptureLock) {
                mCapture = null;
            }
        }
    };

    final PermissionsFragment.Listener mPermissionsListener = new PermissionsFragment.Listener() {

        @Override
        public void onCameraPermissionsGranted() {
            Log.d(TAG, "onCameraPermissionsGranted");
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
