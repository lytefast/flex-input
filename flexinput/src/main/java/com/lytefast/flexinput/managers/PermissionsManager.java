package com.lytefast.flexinput.managers;

/**
 * @author Sam Shih
 */
public interface PermissionsManager {
  interface PermissionsResultCallback {
    void granted();
    void denied();
  }

  boolean requestFileReadPermission(PermissionsResultCallback callback);

  boolean requestCameraPermission(PermissionsResultCallback callback);
}
