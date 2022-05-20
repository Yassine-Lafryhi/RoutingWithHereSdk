package com.here.routing;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class PermissionsRequestor {

    private static final int PERMISSIONS_REQUEST_CODE = 42;
    private ResultListener resultListener;
    private final Activity activity;

    public PermissionsRequestor(Activity activity) {
        this.activity = activity;
    }

    public interface ResultListener {
        void permissionsGranted();
        void permissionsDenied();
    }

    public void request(ResultListener resultListener) {
        this.resultListener = resultListener;

        String[] missingPermissions = getPermissionsToRequest();
        if (missingPermissions.length == 0) {
            resultListener.permissionsGranted();
        } else {
            ActivityCompat.requestPermissions(activity, missingPermissions, PERMISSIONS_REQUEST_CODE);
        }
    }

    private String[] getPermissionsToRequest() {
        ArrayList<String> permissionList = new ArrayList<>();
        try {
            PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(
                    activity.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (packageInfo.requestedPermissions != null) {
                for (String permission : packageInfo.requestedPermissions) {
                    if (ContextCompat.checkSelfPermission(
                            activity, permission) != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M &&
                                permission.equals(Manifest.permission.CHANGE_NETWORK_STATE)) {
                            // Exclude CHANGE_NETWORK_STATE as it does not require explicit user approval.
                            // This workaround is needed for devices running Android 6.0.0,
                            // see https://issuetracker.google.com/issues/37067994
                            continue;
                        }
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                                (permission.equals(Manifest.permission.ACTIVITY_RECOGNITION) ||
                                        permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION))) {
                            continue;
                        }
                        permissionList.add(permission);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return permissionList.toArray(new String[0]);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        if (resultListener == null) {
            return;
        }

        if (grantResults.length == 0) {
            // Request was cancelled.
            return;
        }

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                allGranted &= result == PackageManager.PERMISSION_GRANTED;
            }

            if (allGranted) {
                resultListener.permissionsGranted();
            } else {
                resultListener.permissionsDenied();
            }
        }
    }
}
