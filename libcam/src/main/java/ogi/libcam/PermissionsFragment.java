package ogi.libcam;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


public class PermissionsFragment extends Fragment {

    public interface Listener {
        void onCameraPermissionsGranted();
        void onCameraPermissionsDenied();
        void showRationale(List<String> permissions, Runnable done);
    }

    public static void attach(AppCompatActivity activity, Listener listener, String tag) {
        activity.getSupportFragmentManager().beginTransaction().add(new PermissionsFragment().setListener(listener), tag).commit();
    }

    public PermissionsFragment() {
    }

    public PermissionsFragment setListener(Listener listener) {
        mListener = listener;
        return this;
    }

    private Listener mListener;

    @Override
    public void onResume() {
        super.onResume();

        final Activity activity = getActivity();
        final Listener listener = mListener;
        if (activity != null && listener != null) {
            if (checkPermission(activity)) {
                listener.onCameraPermissionsGranted();
            } else {
                final List<String> rationale = shouldShowRationale(activity);
                if (rationale != null) {
                    listener.showRationale(rationale, new Runnable() {
                        @Override
                        public void run() {
                            final Activity activity = getActivity();
                            if (activity != null) {
                                requestPermissions(activity, 0);
                            }
                        }
                    });
                } else {
                    requestPermissions(activity, 0);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != 0) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        final Listener listener = mListener;
        if (listener != null) {
            final HashSet<String> granted = new HashSet<>();
            for (int i = 0; i < permissions.length && i < grantResults.length; ++i) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    granted.add(permissions[i]);
                }
            }
            if (granted.containsAll(sPermissions)) {
                listener.onCameraPermissionsGranted();
            } else {
                listener.onCameraPermissionsDenied();
            }
        }
    }

    private static boolean checkPermission(Context context) {
        for (String permission : sPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    private static List<String> shouldShowRationale(Activity activity) {
        List<String> rationale = null;
        for (String permission : sPermissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                if (rationale == null) rationale = new ArrayList<>();
                rationale.add(permission);
            }
        }
        return rationale;
    }

    private static void requestPermissions(Activity activity, int request) {
        ActivityCompat.requestPermissions(activity, (String[])sPermissions.toArray(), request);
    }

    private static final List<String> sPermissions = Collections.unmodifiableList(Arrays.asList(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE));

}
