package com.lytefast.flexinput.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;
import com.lytefast.flexinput.FlexInputCoordinator;
import com.lytefast.flexinput.R;
import com.lytefast.flexinput.R2;
import com.lytefast.flexinput.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;



/**
 * {@link Fragment} that allows the user to take a live photo directly from the camera.
 *
 * @author Sam Shih
 */
public class CameraFragment extends PermissionsFragment {
  private static final String[] REQUIRED_PERMISSIONS = {
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.CAMERA};

  @CameraView.Flash
  private static final int[] FLASH_STATE_CYCLE_LIST = {
      CameraView.FLASH_AUTO,
      CameraView.FLASH_ON,
      CameraView.FLASH_OFF};

  private static final String TAG = CameraFragment.class.getCanonicalName();

  public static final int REQUEST_IMAGE_CAPTURE = 4567;

  @BindView(R2.id.camera_view) CameraView cameraView;
  @BindView(R2.id.camera_container) View cameraContainer;
  @BindView(R2.id.permissions_container) FrameLayout permissionsContainer;
  private Unbinder unbinder;

  private FlexInputCoordinator flexInputCoordinator;

  /**
   * Temporary holder for when we intent to the camera. This is used because the resulting intent
   * doesn't return any data if you set the output file in the request.
   */
  private File photoFile;

  @Override
  public void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
  }

  @Nullable
  @Override
  public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
    final Fragment targetFragment = getParentFragment().getTargetFragment();
    if (targetFragment instanceof FlexInputCoordinator) {
      this.flexInputCoordinator = (FlexInputCoordinator) targetFragment;
    }

    View rootView = inflater.inflate(R.layout.fragment_camera, container, false);
    unbinder = ButterKnife.bind(this, rootView);

    cameraView.addCallback(cameraCallback);

    tryStartCamera();
    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();

    if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)
        || !hasPermissions(REQUIRED_PERMISSIONS)) {
      cameraContainer.setVisibility(View.GONE);
      permissionsContainer.setVisibility(View.VISIBLE);

      initPermissionsView(permissionsContainer);

      return;  // No camera detected. just chill
    }
    cameraContainer.setVisibility(View.VISIBLE);
    permissionsContainer.setVisibility(View.GONE);

    // Delayed restart since we are coming back from camera, and for some reason the API
    // isn't fast enough to acknowledge the other activity closed the camera.
    cameraView.postDelayed(new Runnable() {
      @Override
      public void run() {
        if (cameraView != null) {
          tryStartCamera();
        }
      }
    }, 350);
  }

  protected void initPermissionsView(final FrameLayout permissionsContainer) {
    View view = LayoutInflater.from(permissionsContainer.getContext())
        .inflate(R.layout.view_camera_permissions, permissionsContainer, true);
    View reqBtn = ButterKnife.findById(view, R.id.permissions_req_btn);
    reqBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(final View v) {
        requestPermissionClick();
      }
    });
  }

  /**
   * Some cameras don't properly set the {@link android.hardware.Camera.CameraInfo#facing} value.
   * So here, if we fail, just try getting the first front facing camera.
   */
  private void tryStartCamera() {
    try {
      if (cameraView.isCameraOpened()) {
        cameraView.stop();
      }
      cameraView.start();
    } catch (Exception e) {
      Log.w(TAG, "Camera could not be loaded, try front facing camera", e);

      try {
        cameraView.setFacing(CameraView.FACING_FRONT);
        cameraView.start();
      } catch (Exception ex) {
        Log.e(TAG, "Camera could not be loaded", e);
      }
    }

    ImageView flashBtn = ButterKnife.findById(cameraContainer, R.id.camera_flash_btn);
    setFlash(flashBtn, CameraView.FLASH_AUTO);
  }

  @Override
  public void onPause() {
    cameraView.stop();
    super.onPause();
  }

  @Override
  public void onDestroyView() {
    unbinder.unbind();
    super.onDestroyView();
  }

  protected void requestPermissionClick() {
    requestPermissions(new PermissionsResultCallback() {
      @Override
      public void granted() {
        cameraView.post(new Runnable() {
          @Override
          public void run() {
            // #onResume will take care of this for us
          }
        });
      }

      @Override
      public void denied() {
      }
    }, REQUIRED_PERMISSIONS);
  }

  @OnClick(R2.id.camera_flash_btn)
  void onCameraFlashClick(ImageView flashBtn) {
    final int currentFlashState = cameraView.getFlash();
    int currentStateIndex = -1;
    for (int i = 0; i < FLASH_STATE_CYCLE_LIST.length; i++) {
      if (currentFlashState == FLASH_STATE_CYCLE_LIST[i]) {
        currentStateIndex = i;
        break;
      }
    }

    final int newStateIndex = (currentStateIndex + 1) % FLASH_STATE_CYCLE_LIST.length;
    setFlash(flashBtn, FLASH_STATE_CYCLE_LIST[newStateIndex]);
  }

  @OnClick(R2.id.take_photo_btn)
  void onTakePhotoClick() {
    if (cameraView.isCameraOpened()) {
      cameraView.takePicture();
    }
  }

  @OnClick(R2.id.launch_camera_btn)
  void onLaunchCameraClick() {
    cameraView.stop();

    photoFile = flexInputCoordinator.getFileManager().newImageFile();
    Uri photoUri =  flexInputCoordinator.getFileManager().toFileProviderUri(getContext(), photoFile);
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        .putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

    if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
      grantWriteAccessToURI(getContext(), takePictureIntent, photoUri);
      startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
      // TODO need to handle the result: 1) save thumbnail, 2) call #addToMediaStore
    }
  }

  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (REQUEST_IMAGE_CAPTURE != requestCode) {
      return;
    }
    if (Activity.RESULT_CANCELED == resultCode) {
      // Do nothing
    } else if (Activity.RESULT_OK != resultCode) {
      Toast.makeText(getContext(), R.string.camera_intent_result_error, Toast.LENGTH_SHORT).show();
      if (photoFile != null) {
        photoFile.delete();  // cleanup
      }
    } else if (photoFile != null) {
      flexInputCoordinator.addExternalAttachment(FileUtils.toAttachment(photoFile));
      return;
    }
  }

  private final CameraView.Callback cameraCallback = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);
            Toast.makeText(cameraView.getContext(), "Picture saved", Toast.LENGTH_SHORT)
                    .show();

          AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
              File file = flexInputCoordinator.getFileManager().newImageFile();
              OutputStream os = null;
              try {
                os = new FileOutputStream(file);
                os.write(data);
                os.close();

                addToMediaStore(file);
                flexInputCoordinator.addExternalAttachment(FileUtils.toAttachment(file));
              } catch (IOException e) {
                Log.w(TAG, "Cannot write to " + file, e);
              } finally {
                if (os != null) {
                  try {
                    os.close();
                  } catch (IOException e) {
                    // Ignore
                  }
                }
              }
            }
          });
        }
  };

  private void setFlash(final ImageView btn, @CameraView.Flash final int newFlashState) {
    if (cameraView.getFlash() == newFlashState) {
      return;
    }

    @DrawableRes final int flashImage;
    @StringRes final int flashMsg;
    switch(newFlashState) {
      case CameraView.FLASH_ON:
        flashMsg = R.string.flash_on;
        flashImage = R.drawable.ic_flash_on_24dp;
        break;
      case CameraView.FLASH_OFF:
        flashMsg = R.string.flash_off;
        flashImage = R.drawable.ic_flash_off_24dp;
        break;
      default:
        flashMsg = R.string.flash_auto;
        flashImage = R.drawable.ic_flash_auto_24dp;
        break;
    }

    cameraView.setFlash(newFlashState);
    Toast.makeText(getContext(), flashMsg, Toast.LENGTH_SHORT).show();
    btn.setImageResource(flashImage);
  }

  private void addToMediaStore(final File photo) {
    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(photo));
    getContext().sendBroadcast(mediaScanIntent);
    Log.d(TAG, "Photo added to MediaStore: " + photo.getName());
  }

  /**
   * This is a hack to allow the file provider API to still
   * work on older API versions.
   *
   * http://bit.ly/2iC4bUJ
   */
  private static void grantWriteAccessToURI(final @NonNull Context context,
                                            final @NonNull Intent intent,
                                            final @NonNull Uri uri) {
    final List<ResolveInfo> resInfoList = context
        .getPackageManager()
        .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

    for (ResolveInfo resolveInfo : resInfoList) {
      final String packageName = resolveInfo.activityInfo.packageName;
      final int mode = Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION;

      context.grantUriPermission(packageName, uri, mode);
    }
  }
}
