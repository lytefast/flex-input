package com.lytefast.flexinput.model

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.AsyncTask
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore


/**
 * Represents a photo obtained from the [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] store.
 *
 * @author Sam Shih
 */
class Photo : Attachment<String> {

  constructor(id: Long, uri: Uri, displayName: String, photoDataLocation: String)
      : super(id, uri, displayName, photoDataLocation)

  constructor(parcelIn: Parcel) : super(parcelIn)

  fun getThumbnailUri(contentResolver: ContentResolver): Uri? {
    val cursor = contentResolver.query(
        MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Images.Thumbnails._ID),
        "${MediaStore.Images.Thumbnails.IMAGE_ID} = ? AND KIND = ?",
        arrayOf(id.toString(), Integer.toString(MediaStore.Images.Thumbnails.MINI_KIND)), null)

    if (cursor == null || !cursor.moveToFirst()) {
      // Generate thumbnail for next time
      object : AsyncTask<Uri, Int, Void>() {
        override fun doInBackground(vararg params: Uri): Void? {
          MediaStore.Images.Thumbnails.getThumbnail(
              contentResolver, id, MediaStore.Images.Thumbnails.MINI_KIND, null)
          return null
        }
      }.execute(uri)
      return uri  // Slow due to photo size and manipulation but better than nothing
    }
    cursor.use {
      val thumbId = it.getLong(0)
      return ContentUris.withAppendedId(
          MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, thumbId)
    }
  }

  companion object {
    @JvmStatic
    val CREATOR: Parcelable.Creator<Photo> = object : Parcelable.Creator<Photo> {
      override fun createFromParcel(parcel: Parcel): Photo = Photo(parcel)
      override fun newArray(size: Int): Array<Photo?> = arrayOfNulls(size)
    }
  }
}
