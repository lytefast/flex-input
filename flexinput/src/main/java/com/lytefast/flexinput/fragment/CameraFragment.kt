package com.lytefast.flexinput.fragment

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.android.cameraview.CameraView
import com.lytefast.flexinput.FlexInputCoordinator
import com.lytefast.flexinput.R
import com.lytefast.flexinput.utils.FileUtils.toAttachment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


/**
 * [Fragment] that allows the user to take a live photo directly from the camera.
 *
 * @author Sam Shih
 */
@Suppress("MemberVisibilityCanBePrivate")
open class CameraFragment : PermissionsFragment() {

  protected lateinit var cameraContainer: View
  protected lateinit var cameraView: CameraView
  protected lateinit var permissionsContainer: FrameLayout
  protected lateinit var cameraFacingBtn: ImageView

  private var flexInputCoordinator: FlexInputCoordinator<Any>? = null

  private lateinit var handler: Handler

  /**
   * Temporary holder for when we intent to the camera. This is used because the resulting intent
   * doesn't return any data if you set the output file in the request.
   */
  private var photoFile: File? = null

  private var flashState: Int
    @UiThread
    get() = with(PreferenceManager.getDefaultSharedPreferences(requireContext())) {
      getInt(PREF_FLASH_STATE, CameraView.FLASH_AUTO)
    }
    @UiThread
    set(@CameraView.Flash value) = with(PreferenceManager.getDefaultSharedPreferences(requireContext())) {
      edit()
          .putInt(PREF_FLASH_STATE, value)
          .apply()
    }

  var onFlashChanged: (flashBtn: View, newState: Int) -> Unit = { flashBtn, newState ->
    @StringRes val messageStringRes = when (newState) {
      CameraView.FLASH_ON -> R.string.flash_on
      CameraView.FLASH_OFF -> R.string.flash_off
      else -> R.string.flash_auto
    }

    Toast.makeText(flashBtn.context, messageStringRes, Toast.LENGTH_SHORT).show()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    retainInstance = true

    handler = Handler()
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    @Suppress("UNCHECKED_CAST")
    this.flexInputCoordinator = parentFragment?.parentFragment as? FlexInputCoordinator<Any>

    return inflater.inflate(R.layout.fragment_camera, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    view.apply {
      cameraContainer = findViewById(R.id.camera_container)
      cameraView = findViewById(R.id.camera_view)
      permissionsContainer = findViewById(R.id.permissions_container)

      findViewById<View>(R.id.camera_view_cropper)
          ?.setOnTouchListener { _, _ -> true  /* disable user scroll*/ }
      findViewById<View>(R.id.take_photo_btn)
          ?.setOnClickListener { onTakePhotoClick() }
      findViewById<View>(R.id.launch_camera_btn)
          ?.setOnClickListener { onLaunchCameraClick() }

      val flashBtn = findViewById<ImageView>(R.id.camera_flash_btn)
      flashBtn?.setOnClickListener { onCameraFlashClick(it as ImageView) }
      setFlash(flashBtn, flashState, notify = false)

      cameraFacingBtn = findViewById(R.id.camera_facing_btn)
      cameraFacingBtn.setOnClickListener { onCameraFacingClick(it as ImageView) }
      if (isSingleCamera) {
        cameraFacingBtn.visibility = View.GONE
      }
    }

    cameraView.addCallback(cameraCallback)
  }

  private val isSingleCamera by lazy {
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    when (Build.VERSION.SDK_INT) {
      in 0..Build.VERSION_CODES.KITKAT_WATCH -> Camera.getNumberOfCameras() == 1
      else -> {
        (context!!.getSystemService(CAMERA_SERVICE) as CameraManager)
          .cameraIdList.size == 1
      }
    }
  }

  override fun onResume() {
    super.onResume()
    val context = context ?: return

    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        || !hasPermissions(*REQUIRED_PERMISSIONS)) {
      cameraContainer.visibility = View.GONE
      permissionsContainer.also {
        it.visibility = View.VISIBLE
        if (it.childCount == 0) {
          initPermissionsView(it)
        }
      }

      return   // No camera detected. just chill
    }
    cameraContainer.visibility = View.VISIBLE
    permissionsContainer.visibility = View.GONE

    // Delayed restart since we are coming back from camera, and for some reason the API
    // isn't fast enough to acknowledge the other activity closed the camera.
    handler.postDelayed({
      if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
        cameraView.tryStartCamera()
      }
    }, 300)
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
    Log.d(TAG, "Try starting the camera")
    try {
      if (isCameraOpened) {
        stop()
      }
      start()
    } catch (e: Exception) {
      onCameraError(e, "Camera could not be loaded, try front facing camera")

      try {
        facing = CameraView.FACING_FRONT
        start()
      } catch (ex: Exception) {
        onCameraError(e, "Camera could not be loaded")
        Toast.makeText(context, R.string.camera_unknown_error, Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun onPause() {
    handler.removeCallbacksAndMessages(null)
    cameraView.stop()
    super.onPause()
  }

  protected open fun requestPermissionClick() {
    requestPermissions(object : PermissionsFragment.PermissionsResultCallback {
      override fun granted() {
        cameraView.post {
          // #onResume will take care of this for us
        }
      }

      override fun denied() {}
    }, *REQUIRED_PERMISSIONS)
  }

  private fun onCameraFlashClick(flashBtn: ImageView) {
    val currentFlashState = cameraView.flash
    val currentStateIndex = FLASH_STATE_CYCLE_LIST.indices.firstOrNull {
      currentFlashState == FLASH_STATE_CYCLE_LIST[it]
    } ?: 0

    val newStateIndex = (currentStateIndex + 1) % FLASH_STATE_CYCLE_LIST.size
    setFlash(flashBtn, FLASH_STATE_CYCLE_LIST[newStateIndex])
  }


  private fun onCameraFacingClick(facingBtn: ImageView) {
    val currentFlashState = cameraView.facing
    val currentStateIndex = FACING_STATE_CYCLE_LIST.indices.firstOrNull {
      currentFlashState == FACING_STATE_CYCLE_LIST[it]
    } ?: 0

    val newStateIndex = (currentStateIndex + 1) % FACING_STATE_CYCLE_LIST.size
    setFacing(facingBtn, FACING_STATE_CYCLE_LIST[newStateIndex])
  }

  private fun onTakePhotoClick() {
    cameraView.apply {
      if (isCameraOpened) {
        try {
          takePicture()
        } catch (e: Exception) {
          onCameraError(e, "Camera error on take picture")
          Toast.makeText(context, R.string.camera_unknown_error, Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  private fun onLaunchCameraClick() {
    val context = context ?: return
    cameraView.stop()

    val fileManager = flexInputCoordinator?.fileManager ?: return

    val newPhotoFile = fileManager.newImageFile()
    val photoUri = fileManager.toFileProviderUri(context, newPhotoFile)
    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        .putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

    if (takePictureIntent.resolveActivity(context.packageManager) != null) {
      this.photoFile = newPhotoFile
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
      when(resultCode) {
        Activity.RESULT_CANCELED -> { /* Do nothing*/ }
        Activity.RESULT_OK -> {
          context?.addToMediaStore(it)
          flexInputCoordinator?.addExternalAttachment(it.toAttachment())
          cameraView.stop()  // make sure we stop the camera since we are just going to exit
        }
        else -> {
          Toast.makeText(context, R.string.camera_intent_result_error, Toast.LENGTH_SHORT).show()
          it.delete()  // cleanup
        }
      }
    }
  }

  private val cameraCallback = object : CameraView.Callback() {

    override fun onCameraOpened(cameraView: CameraView) {
      Log.d(TAG, "onCameraOpened")
      setFacing(cameraFacingBtn, cameraView.facing)
    }

    override fun onCameraClosed(cameraView: CameraView) {
      Log.d(TAG, "onCameraClosed")
    }

    override fun onPictureTaken(cameraView: CameraView, data: ByteArray?) {
      Log.d(TAG, "onPictureTaken ${data?.size ?: 0}")
      if (data == null) {
        return
      }
      Toast.makeText(cameraView.context, "Picture saved", Toast.LENGTH_SHORT)
          .show()

      AsyncTask.execute {
        flexInputCoordinator?.fileManager?.newImageFile()?.also { file ->
          try {
            FileOutputStream(file).use {
              it.write(data)
            }

            handler.post {
              context?.addToMediaStore(file)
              flexInputCoordinator?.addExternalAttachment(file.toAttachment())
            }
          } catch (e: IOException) {
            Log.w(TAG, "Cannot write to $file", e)
          }
        }
      }
    }
  }

  private fun setFacing(btn: ImageView, @CameraView.Facing newFacingState: Int) {
    try {
      cameraView.apply {
        if (facing != newFacingState) {
          facing = newFacingState
          Toast.makeText(context, R.string.camera_switched, Toast.LENGTH_SHORT).show()
        }
      }
      @DrawableRes
      val facingImg: Int = when (newFacingState) {
        CameraView.FACING_FRONT -> R.drawable.ic_camera_rear_white_24dp
//        CameraView.FACING_BACK,
        else -> R.drawable.ic_camera_front_white_24dp
      }
      btn.setImageResource(facingImg)

    } catch (e: Exception) {
      onCameraError(e, "Cannot switch camera facing")
      Toast.makeText(context, R.string.camera_unknown_error, Toast.LENGTH_SHORT).show()
    }
  }

  private fun setFlash(btn: ImageView, @CameraView.Flash newFlashState: Int, notify: Boolean = true) {
    if (cameraView.flash == newFlashState) {
      return
    }
    flashState = newFlashState

    try {
      cameraView.flash = newFlashState
      btn.setImageResource(when (cameraView.flash) {
        CameraView.FLASH_ON -> R.drawable.ic_flash_on_24dp
        CameraView.FLASH_OFF -> R.drawable.ic_flash_off_24dp
        else -> R.drawable.ic_flash_auto_24dp
      })

      if (notify) onFlashChanged(btn, cameraView.flash)

    } catch (e: Exception) {
      onCameraError(e, "Camera error on set flash")
      Toast.makeText(context, R.string.camera_unknown_error, Toast.LENGTH_SHORT)
          .show()
    }
  }

  protected open fun onCameraError(e: Exception, message: String) {
    Log.e(TAG, message, e)
  }

  companion object {
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA)

    @CameraView.Flash
    private val FLASH_STATE_CYCLE_LIST = intArrayOf(CameraView.FLASH_AUTO, CameraView.FLASH_ON, CameraView.FLASH_OFF)

    @CameraView.Facing
    private val FACING_STATE_CYCLE_LIST = intArrayOf(CameraView.FACING_BACK, CameraView.FACING_FRONT)

    private val TAG = CameraFragment::class.java.canonicalName

    private const val PREF_FLASH_STATE = "FLASH_STATE"

    const val REQUEST_IMAGE_CAPTURE = 4567

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
