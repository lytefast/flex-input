package com.lytefast.flexinput.utils;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.lytefast.flexinput.model.Attachment;

import java.io.File;


/**
 * @author Sam Shih
 */
public class FileUtils {

  @NonNull
  public static Attachment<File> toAttachment(final File f) {
    return new Attachment<>(f.hashCode(), toUri(f), f.getName(), f);
  }

  public static Uri toUri(final File f) {
    // Use parse due to bug with fresco loader: https://github.com/facebook/fresco/issues/1596
    // FIXME 2017-01: when https://github.com/facebook/fresco/issues/1596 is resolve remove
    // Uri fileUri = Uri.fromFile(f);
    return Uri.parse("file://" + f.getAbsolutePath());
  }

}
