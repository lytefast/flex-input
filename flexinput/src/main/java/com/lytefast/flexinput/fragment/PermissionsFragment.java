package com.lytefast.flexinput.fragment;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;


/**
 * Fragment that knows how to check, request, and handle permissions.
 *
 * @author Sam Shih
 */
public class PermissionsFragment extends Fragment {

  /**
   * Random code to uniquely identify a permissions response.
   */
  private static final int PERMISSIONS_REQUEST_CODE = 2525;


  interface PermissionsResultCallback {
    void granted();
    void denied();
  }
  private PermissionsResultCallback permissionRequestCallback;


  public boolean requestPermissions(
      final PermissionsResultCallback callback, final String... requiredPermissions) {
    if (!hasPermissions(requiredPermissions)) {
      this.permissionRequestCallback = callback;
      requestPermissions(requiredPermissions, PERMISSIONS_REQUEST_CODE);
      return false;
    }
    callback.granted();
    return true;
  }

  @Override
  public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
    if (requestCode != PERMISSIONS_REQUEST_CODE) {
      permissionRequestCallback = null;
      return;
    }

    // If request is cancelled, the result arrays are empty.
    if (areAllPermissionsGranted(grantResults)) {
      permissionRequestCallback.granted();
    } else {
      permissionRequestCallback.denied();
    }
    permissionRequestCallback = null;
  }

  protected boolean hasPermissions(String... requiredPermissionList) {
    final Context context = getContext();

    for (String reqPerm : requiredPermissionList) {
      boolean isGranted = ContextCompat.checkSelfPermission(context, reqPerm) == PackageManager.PERMISSION_GRANTED;
      if (!isGranted) {
        return false;
      }
    }
    return true;
  }

  protected boolean areAllPermissionsGranted(int... permissionsAccessList) {
    if (permissionsAccessList.length < 1) {
      return false;
    }
    for (int reqPermAccess : permissionsAccessList) {
      if (PackageManager.PERMISSION_GRANTED != reqPermAccess) {
        return false;
      }
    }
    return true;
  }
}
