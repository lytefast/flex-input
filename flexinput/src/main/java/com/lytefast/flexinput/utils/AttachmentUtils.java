package com.lytefast.flexinput.utils;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.lytefast.flexinput.model.Attachment;

import java.io.File;


/**
 * @author Sam Shih
 */
public class AttachmentUtils {

  @NonNull
  public static Attachment fromFile(final File f) {
    return new Attachment(f.hashCode(), Uri.fromFile(f), f.getName());
  }
}
