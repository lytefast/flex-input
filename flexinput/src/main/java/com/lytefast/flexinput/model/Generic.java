package com.lytefast.flexinput.model;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;


/**
 * @author Sam Shih
 */
public class Generic<T> extends Attachment {
  public final T rawData;

  public Generic(final long id, final Uri uri, final String displayName, T rawData) {
    super(id, uri, displayName);
    this.rawData = rawData;
  }
}
