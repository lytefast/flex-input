package com.lytefast.flexinput.managers;

import android.content.Context;
import android.net.Uri;

import java.io.File;


/**
 * @author Sam Shih
 */
public interface FileManager {
  File newImageFile();

  Uri toFileProviderUri(final Context context, File file);
}
