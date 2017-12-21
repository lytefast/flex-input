package com.lytefast.flexinput.fragment

import android.content.pm.PackageManager
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat


/**
 * Fragment that knows how to check, request, and handle permissions.
 *
 * @author Sam Shih
 */
open class PermissionsFragment : Fragment() {

  interface PermissionsResultCallback {
    fun granted()
    fun denied()
  }

  private var permissionRequestCallback: PermissionsResultCallback? = null


  fun requestPermissions(
      callback: PermissionsResultCallback, vararg requiredPermissions: String): Boolean {
    if (!hasPermissions(*requiredPermissions)) {
      this.permissionRequestCallback = callback
      requestPermissions(requiredPermissions, PERMISSIONS_REQUEST_CODE)
      return false
    }
    callback.granted()
    return true
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    if (requestCode != PERMISSIONS_REQUEST_CODE) {
      permissionRequestCallback = null
      return
    }

    permissionRequestCallback?.apply {
      // If request is cancelled, the result arrays are empty.
      if (areAllPermissionsGranted(*grantResults)) {
        granted()
      } else {
        denied()
      }
    }
    permissionRequestCallback = null
  }

  protected open fun hasPermissions(vararg requiredPermissionList: String): Boolean =
      context?.let { context ->
        requiredPermissionList.all { reqPerm ->
          ContextCompat.checkSelfPermission(context, reqPerm) == PackageManager.PERMISSION_GRANTED
        }
      } ?: false

  protected open fun areAllPermissionsGranted(vararg permissionsAccessList: Int): Boolean {
    if (permissionsAccessList.isEmpty()) {
      return false
    }
    return permissionsAccessList.all {
      PackageManager.PERMISSION_GRANTED == it
    }
  }

  companion object {

    /**
     * Random code to uniquely identify a permissions response.
     */
    private val PERMISSIONS_REQUEST_CODE = 2525
  }
}
