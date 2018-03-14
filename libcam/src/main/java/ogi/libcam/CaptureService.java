package ogi.libcam;

import android.app.Service;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Binder;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Surface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class CaptureService extends Service {

    private static final String TAG = "LibCam";

    private final CaptureServiceBinder mBinder;
    private BaseRenderer mRenderer;
    private EglContextThread mEglContextThread;

    private CameraManager mManager;
    private Listener mListener;

    private final HashSet<String> mAvailableCameras = new HashSet<>();

    private final CameraManager.AvailabilityCallback mAvailabilityCallback = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(@NonNull String cameraId) {
            synchronized (mAvailableCameras) {
                mAvailableCameras.add(cameraId);
                mAvailableCameras.notifyAll();
            }
        }

        @Override
        public void onCameraUnavailable(@NonNull String cameraId) {
            synchronized (mAvailableCameras) {
                mAvailableCameras.remove(cameraId);
                mAvailableCameras.notifyAll();
            }
        }
    };

    private final CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            try {
                final StreamConfigurationMap configs = mManager.getCameraCharacteristics(camera.getId())
                        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                Size max = new Size(0, 0);
                long maxCount = 0;
                for (Size size : configs.getOutputSizes(ImageFormat.YUV_420_888)) {
                    final long count = (long)size.getWidth() * (long)size.getHeight();
                    if (count > maxCount) {
                        max = size;
                        maxCount = count;
                    }
                }

                makeSurface(max);

                final ArrayList<Surface> surfaces = new ArrayList<>();
                surfaces.add(mRenderer.getSurface());

                camera.createCaptureSession(surfaces, mCameraCaptureSessionStateCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                mListener.onCameraError(camera.getId());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            destroySurface();
            mListener.onCameraError(camera.getId());
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            destroySurface();
            mListener.onCameraError(camera.getId());
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            destroySurface();
        }

        //TODO override
    };

    private final CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mSession = session;
            mListener.onCameraCaptureSessionConfigured(session.getDevice().getId());
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            mSession = null;
            mListener.onCameraError(session.getDevice().getId());
        }

        @Override
        public void onReady(@NonNull CameraCaptureSession session) {
        }

        @Override
        public void onActive(@NonNull CameraCaptureSession session) {
        }

        @Override
        public void onCaptureQueueEmpty(@NonNull CameraCaptureSession session) {
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
        }

        @Override
        public void onSurfacePrepared(@NonNull CameraCaptureSession session, @NonNull Surface surface) {
        }

        //TODO override

    };

    private CameraCaptureSession mSession;
    private final CameraCaptureSession.CaptureCallback mCameraCaptureSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            //XXX mListener.onPreviewStarted(session.getDevice().getId());
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            mListener.onCameraError(session.getDevice().getId());
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        }

        @Override
        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
        }

        @Override
        public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
        }

        @Override
        public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
        }

        //TODO override
    };

    public CaptureService() {
        mBinder = new CaptureServiceBinder();
    }

    @Override
    public CaptureServiceBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mRenderer = new BaseRenderer(getAssets());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        mManager.registerAvailabilityCallback(mAvailabilityCallback, null);
    }

    @Override
    public void onDestroy() {
        if (mManager != null) {
            mManager.unregisterAvailabilityCallback(mAvailabilityCallback);
        }
        destroySurface();
        super.onDestroy();
    }

    private void makeSurface(Size size) {
        destroySurface();
        mEglContextThread = new EglContextThread(mRenderer, size);
    }

    private void destroySurface() {
        if (mEglContextThread != null) {
            mEglContextThread.release();
            mEglContextThread = null;
        }
    }

    public interface Listener {
        void onCameraCaptureSessionConfigured(String cameraId);
        void onPreviewStarted(String cameraId);

        void onCameraError(String cameraId);
    }

    public class CaptureServiceBinder extends Binder {

        public boolean openCamera(String cameraId, @NonNull Listener listener) {
            //TODO close previous
            mListener = listener;
            try {
                mManager.openCamera(cameraId, mCameraDeviceStateCallback, null);
                return true;
            } catch (CameraAccessException e) {
                return false;
            } catch (SecurityException e) {
                return false;
            }
        }

        public boolean startPreview() {
            try {
                final CaptureRequest.Builder request = mSession.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                request.addTarget(mRenderer.getSurface());
                request.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                request.set(CaptureRequest.CONTROL_AE_LOCK, false);
                request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                request.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
                mSession.setRepeatingRequest(request.build(), mCameraCaptureSessionCaptureCallback, null);
                return true;
            } catch (CameraAccessException e) {
                e.printStackTrace();
                return false;
            }
        }

        public void attachAnotherContext() {
            mRenderer.detach();
            mRenderer.attachAnother();
        }

        public void attachOriginalContext() {
            mRenderer.attach();
        }

        public String getBackCameraId() {
            return getCameraId(CameraCharacteristics.LENS_FACING_BACK);
        }

        public String getFrontCameraId() {
            return getCameraId(CameraCharacteristics.LENS_FACING_FRONT);
        }

        private String getCameraId(int lensFacing) {
            synchronized (mAvailableCameras) {
                for (final String cameraId : mAvailableCameras) {
                    try {
                        if (mManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING) == lensFacing) {
                            return cameraId;
                        }
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        }


    }

}
