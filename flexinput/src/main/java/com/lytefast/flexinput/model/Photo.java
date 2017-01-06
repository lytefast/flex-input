package com.lytefast.flexinput.model;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;


/**
 * Represents a photo obtained from the {@link MediaStore.Images.Media#EXTERNAL_CONTENT_URI} store.
 */
public class Photo extends Attachment {
  public Photo(final long id, final Uri uri, final String displayName) {
    super(id, uri, displayName);
  }

  @Nullable
  public Uri getThumbnailUri(final ContentResolver contentResolver) {
    final Cursor cursor = contentResolver.query(
          MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
          new String[]{MediaStore.Images.Thumbnails._ID},
          MediaStore.Images.Thumbnails.IMAGE_ID + "= ? AND KIND = ?",
          new String[]{String.valueOf(id), Integer.toString(MediaStore.Images.Thumbnails.MINI_KIND)},
          null);

      if (cursor == null || !cursor.moveToFirst()) {
        return null;
      }
      try {
        final long thumbId = cursor.getLong(0);
        return ContentUris.withAppendedId(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                                          thumbId);
      } finally {
        cursor.close();
      }
  }
}
