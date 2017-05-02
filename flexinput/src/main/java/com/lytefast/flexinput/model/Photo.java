package com.lytefast.flexinput.model;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.Nullable;


/**
 * Represents a photo obtained from the {@link MediaStore.Images.Media#EXTERNAL_CONTENT_URI} store.
 *
 * @author Sam Shih
 */
public class Photo extends Attachment<String> {
  public Photo(final long id, final Uri uri, final String displayName, String photoDataLocation) {
    super(id, uri, displayName, photoDataLocation);
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
      // Generate thumbnail for next time
      new AsyncTask<Uri, Integer, Void>() {
        @Override
        protected Void doInBackground(final Uri... params) {
          MediaStore.Images.Thumbnails.getThumbnail(
              contentResolver, id, MediaStore.Images.Thumbnails.MINI_KIND, null);
          return null;
        }
      }.execute(uri);
      return uri;  // Slow due to photo size and manipulation but better than nothing
    }
    try {
      final long thumbId = cursor.getLong(0);
      return ContentUris.withAppendedId(
          MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, thumbId);
    } finally {
      cursor.close();
    }
  }
}
