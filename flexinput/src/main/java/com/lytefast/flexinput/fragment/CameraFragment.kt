package com.lytefast.flexinput.fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import com.lytefast.flexinput.FlexInputCoordinator
import com.lytefast.flexinput.R
import com.lytefast.flexinput.utils.FileUtils.toAttachment
import com.otaliastudios.cameraview.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


/**
 * [Fragment] that allows the user to take a live photo directly from the camera.
 *
 * @author Sam Shih
 */
open class CameraFragment : PermissionsFragment() {

  protected var cameraContainer: View? = null
  protected var cameraView: CameraView? = null
  protected var permissionsContainer: FrameLayout? = null
  protected var cameraFacingBtn: ImageView? = null

  private var flexInputCoordinator: FlexInputCoordinator<Any>? = null

  /**
   * Temporary holder for when we intent to the camera. This is used because the resulting intent
   * doesn't return any data if you set the output file in the request.
   */
  private var photoFile: File? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    retainInstance = true
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    @Suppress("UNCHECKED_CAST")
    this.flexInputCoordinator = parentFragment?.parentFragment as? FlexInputCoordinator<Any>

    return inflater.inflate(R.layout.fragment_camera, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    view.apply {
      permissionsContainer = findViewById(R.id.permissions_container)
      cameraContainer = findViewById(R.id.camera_container)
      cameraView = findViewById(R.id.camera_view)

      findViewById<View>(R.id.take_photo_btn)
          ?.setOnClickListener { onTakePhotoClick() }
      findViewById<View>(R.id.launch_camera_btn)
          ?.setOnClickListener { onLaunchCameraClick() }
      findViewById<ImageView>(R.id.camera_flash_btn)
          ?.setOnClickListener { onCameraFlashClick(it as ImageView) }
      cameraFacingBtn = findViewById<ImageView>(R.id.camera_facing_btn)?.apply {
        setOnClickListener { cameraView?.toggleFacing() }
      }
    }

    cameraView?.apply {
      addCameraListener(cameraCallback)
      tryStartCamera()
    }
  }

  override fun onResume() {
    super.onResume()

    val context = context ?: return

    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        || !hasPermissions(*REQUIRED_PERMISSIONS)) {
      cameraContainer?.visibility = View.GONE
      permissionsContainer?.also {
        it.visibility = View.VISIBLE
        initPermissionsView(it)
      }

      return   // No camera detected. just chill
    }
    cameraContainer?.visibility = View.VISIBLE
    permissionsContainer?.visibility = View.GONE

    // Delayed restart since we are coming back from camera, and for some reason the API
    // isn't fast enough to acknowledge the other activity closed the camera.
    cameraView?.postDelayed({
      cameraView?.tryStartCamera()
    }, 350)
  }

  protected open fun initPermissionsView(permissionsContainer: FrameLayout) {
    val view = LayoutInflater.from(permissionsContainer.context)
        .inflate(R.layout.view_camera_permissions, permissionsContainer, true)
    view.findViewById<View>(R.id.permissions_req_btn)
        ?.setOnClickListener { requestPermissionClick() }
  }

  /**
   * Some cameras don't properly set the [android.hardware.Camera.CameraInfo.facing] value.
   * So here, if we fail, just try getting the first front facing camera.
   */
  private fun CameraView.tryStartCamera() {
    try {
      if (isStarted) {
        stop()
      }
      start()
    } catch (e: Exception) {
      Log.w(TAG, "Camera could not be loaded, try front facing camera", e)

      try {
        facing = Facing.FRONT
        start()
      } catch (ex: Exception) {
        Log.e(TAG, "Camera could not be loaded", e)
      }
    }

    cameraContainer?.findViewById<ImageView>(R.id.camera_flash_btn)?.also {
      setFlash(it, Flash.AUTO)
    }
  }

  override fun onPause() {
    cameraView?.stop()
    super.onPause()
  }

  override fun onDestroy() {
    cameraView?.destroy()
    super.onDestroy()
  }

  protected open fun requestPermissionClick() {
    requestPermissions(object : PermissionsFragment.PermissionsResultCallback {
      override fun granted() {
        cameraView?.post {
          // #onResume will take care of this for us
        }
      }

      override fun denied() {}
    }, *REQUIRED_PERMISSIONS)
  }

  private fun onCameraFlashClick(flashBtn: ImageView) {
    val currentFlashState = cameraView?.flash
    val currentStateIndex = FLASH_STATE_CYCLE_LIST.indices.firstOrNull {
      currentFlashState == FLASH_STATE_CYCLE_LIST[it]
    } ?: 0

    val newStateIndex = (currentStateIndex + 1) % FLASH_STATE_CYCLE_LIST.size
    setFlash(flashBtn, FLASH_STATE_CYCLE_LIST[newStateIndex])
  }


  private fun onTakePhotoClick() {
    if (cameraView?.isStarted == true) {
      cameraView?.capturePicture()
    }
  }

  private fun onLaunchCameraClick() {
    val context = context ?: return
    cameraView?.stop()

    photoFile = flexInputCoordinator!!.fileManager.newImageFile()
    val photoUri = flexInputCoordinator!!.fileManager.toFileProviderUri(context, photoFile)
    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        .putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

    if (takePictureIntent.resolveActivity(context.packageManager) != null) {
      context.grantWriteAccessToURI(takePictureIntent, photoUri)
      startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (REQUEST_IMAGE_CAPTURE != requestCode) {
      return
    }

    photoFile?.also {
      when {
        Activity.RESULT_CANCELED == resultCode -> { /* Do nothing*/ }
        Activity.RESULT_OK != resultCode -> {
          Toast.makeText(context, R.string.camera_intent_result_error, Toast.LENGTH_SHORT).show()
          it.delete()  // cleanup
        }
        else -> {
          context?.addToMediaStore(it)
          flexInputCoordinator?.addExternalAttachment(it.toAttachment())
        }
      }
    }
  }

  private val cameraCallback = object : CameraListener() {

    override fun onCameraOpened(cameraOptions: CameraOptions) {
      Log.d(TAG, "onCameraOpened")
      cameraView?.apply {
        onFacingChanged(facing)
        cameraContainer?.findViewById<ImageView>(R.id.camera_flash_btn)?.also {
          onFlashChanged(it, flash)
        }
      }
    }

    override fun onCameraClosed() {
      Log.d(TAG, "onCameraClosed")
    }

    override fun onPictureTaken(jpeg: ByteArray?) {
      Log.d(TAG, "onPictureTaken ${jpeg?.size ?: 0}")
      if (jpeg == null) {
        return
      }
      Toast.makeText(context, "Picture saved", Toast.LENGTH_SHORT).show()

      AsyncTask.execute {
        flexInputCoordinator?.fileManager?.newImageFile()?.also { file ->
          try {
            FileOutputStream(file).use {
              it.write(jpeg)
            }

            context?.addToMediaStore(file)
            cameraView?.post {
              flexInputCoordinator?.addExternalAttachment(file.toAttachment())
            }
          } catch (e: IOException) {
            Log.w(TAG, "Cannot write to $file", e)
          }
        }
      }
    }
  }

  private fun onFacingChanged(newFacingState: Facing) {
    @DrawableRes val facingImg: Int =
        when (newFacingState) {
          Facing.FRONT -> R.drawable.ic_camera_rear_white_24dp
//          Facing.BACK,
          else -> R.drawable.ic_camera_front_white_24dp
        }
    cameraFacingBtn?.setImageResource(facingImg)
  }

  private fun setFlash(btn: ImageView, newFlashState: Flash) {
    if (cameraView?.flash == newFlashState) {
      return
    }
    cameraView?.flash = newFlashState

    onFlashChanged(btn, newFlashState)
    @StringRes val flashMsg: Int = when (newFlashState) {
      Flash.ON -> R.string.flash_on
      Flash.OFF -> R.string.flash_off
      else -> R.string.flash_auto
    }
    Toast.makeText(btn.context, flashMsg, Toast.LENGTH_SHORT).show()
  }

  private fun onFlashChanged(btn: ImageView, newFlashState: Flash) {
    @DrawableRes val flashImage = when (newFlashState) {
      Flash.ON -> R.drawable.ic_flash_on_24dp
      Flash.OFF -> R.drawable.ic_flash_off_24dp
      else -> R.drawable.ic_flash_auto_24dp
    }
    btn.setImageResource(flashImage)
  }

  companion object {
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA)

    private val FLASH_STATE_CYCLE_LIST = arrayOf(Flash.AUTO, Flash.ON, Flash.OFF)

    private val TAG = CameraFragment::class.java.canonicalName

    val REQUEST_IMAGE_CAPTURE = 4567

    /**
     * This is a hack to allow the file provider API to still
     * work on older API versions.
     *
     * http://bit.ly/2iC4bUJ
     */
    private fun Context.grantWriteAccessToURI(intent: Intent, uri: Uri) {
      val resInfoList =
          packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

      for (resolveInfo in resInfoList) {
        val packageName = resolveInfo.activityInfo.packageName
        val mode = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION

        grantUriPermission(packageName, uri, mode)
      }
    }

    private fun Context.addToMediaStore(photo: File) {
      val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(photo))
      sendBroadcast(mediaScanIntent)
      Log.d(TAG, "Photo added to MediaStore: ${photo.name}")
    }
  }
}
