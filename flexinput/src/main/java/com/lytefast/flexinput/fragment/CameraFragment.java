package com.lytefast.flexinput.fragment;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;
import com.lytefast.flexinput.FileManager;
import com.lytefast.flexinput.R;
import com.lytefast.flexinput.R2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


/**
 * {@link Fragment} that allows the user to take a live photo directly from the camera.
 *
 * @author Sam Shih
 */
public class CameraFragment extends Fragment {

  private static final String TAG = CameraFragment.class.getCanonicalName();

  public static final int REQUEST_IMAGE_CAPTURE = 4567;

  @BindView(R2.id.camera_view) CameraView cameraView;
  private Unbinder unbinder;

  private PhotoTakenCallback photoTakenCallback;
  private FileManager fileManager;


  @Nullable
  @Override
  public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_camera, container, false);
    unbinder = ButterKnife.bind(this, rootView);

    cameraView.addCallback(cameraCallback);
    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();

    if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
      return;  // No camera detected. just chill
      // TODO show empty state so buttons are disabled
    }
    cameraView.start();
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

  @OnClick(R2.id.take_photo_btn)
  void onTakePhotoClick() {
    cameraView.takePicture();
  }

  @OnClick(R2.id.launch_camera_btn)
  void onLaunchCameraClick() {
    cameraView.stop();

    final File photoFile = fileManager.newImageFile();
    photoTakenCallback.onPhotoTaken(photoFile);

    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        .putExtra(MediaStore.EXTRA_OUTPUT, fileManager.toFileProviderUri(getContext(), photoFile))
        .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
      startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
      // TODO need to handle the result: 1) save thumbnail, 2) call #addToMediaStore
    }
  }

  public void setPhotoTakenCallback(final PhotoTakenCallback photoTakenCallback) {
    this.photoTakenCallback = photoTakenCallback;
  }


  public void setFileManager(final FileManager fileManager) {
    this.fileManager = fileManager;
  }

  private final CameraView.Callback cameraCallback
            = new CameraView.Callback() {

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
              File file = fileManager.newImageFile();
              OutputStream os = null;
              try {
                os = new FileOutputStream(file);
                os.write(data);
                os.close();

                addToMediaStore(file);
                photoTakenCallback.onPhotoTaken(file);
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

  private void addToMediaStore(final File photo) {
    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(photo));
    getContext().sendBroadcast(mediaScanIntent);
    Log.d(TAG, "Photo added to MediaStore: " + photo.getName());
  }

  public interface PhotoTakenCallback {
    void onPhotoTaken(File photoFile);
  }
}
