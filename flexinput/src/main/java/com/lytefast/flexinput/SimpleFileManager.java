package com.lytefast.flexinput;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * A basic implementation of the {@link FileManager} that uses a specified {@link FileProvider}.
 * @author Sam Shih
 */
public class SimpleFileManager implements FileManager {

  private static final String TAG = SimpleFileManager.class.getCanonicalName();
  private final String providerAuthority;
  private final String imageDirName;
  private final String imagePrefix;

  public SimpleFileManager(String providerAuthority, String imageDirName) {
    this(providerAuthority, imageDirName, "JPEG_");
  }

  public SimpleFileManager(String providerAuthority, String imageDirName, String imagePrefix) {
    this.providerAuthority = providerAuthority;
    this.imageDirName = imageDirName;
    this.imagePrefix = imagePrefix;
  }

  protected File getImagesDirectory() {
    File file = new File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        imageDirName);
    if (!file.mkdirs() && !file.isDirectory()) {
      Log.e(TAG, "Directory not created");
    }
    return file;
  }

  @Override
  public File newImageFile() {
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String imageFileName = imagePrefix + timeStamp + ".jpg";
    return new File(getImagesDirectory(), imageFileName);
  }

  @Override
  public Uri toFileProviderUri(final Context context, final File file) {
    return FileProvider.getUriForFile(context, providerAuthority, file);
  }
}
