package com.lytefast.flexinput.model

import android.content.ContentResolver
import android.content.ContentUris
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi


/**
 * Represents a photo obtained from the [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] store.
 *
 * @author Sam Shih
 */
class Photo : Attachment<String> {

  constructor(id: Long, uri: Uri, displayName: String, photoDataLocation: String?)
      : super(id, uri, displayName, photoDataLocation)

  constructor(parcelIn: Parcel) : super(parcelIn)

  @RequiresApi(Build.VERSION_CODES.Q)
  fun getThumbnailQ(contentResolver: ContentResolver, width: Int, height: Int): Bitmap? {
    return try {
      contentResolver.loadThumbnail(uri, Size(width, height), null)
    } catch (e: java.lang.Exception) {
      Log.e("Thumbnail", "Thumbnail Failed to load $e")
      null
    }
  }

  fun getThumbnailUri(contentResolver: ContentResolver): Uri? {
    val cursor = contentResolver.query(
        MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Images.Thumbnails._ID),
        "${MediaStore.Images.Thumbnails.IMAGE_ID} = ? AND KIND = ?",
        arrayOf(id.toString(), Integer.toString(MediaStore.Images.Thumbnails.MINI_KIND)), null)

    if (cursor == null || !cursor.moveToFirst()) {
      asyncGenerateThumbnail(contentResolver)
      cursor?.close()
      return uri  // Slow due to photo size and manipulation but better than nothing
    }
    cursor.use {
      val thumbId = it.getLong(0)
      return ContentUris.withAppendedId(
          MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, thumbId)
    }
  }

  /**
   * Generate thumbnail for next time.
   */
  private fun asyncGenerateThumbnail(contentResolver: ContentResolver) {
    AsyncTask.execute {
      try {
        MediaStore.Images.Thumbnails.getThumbnail(
            contentResolver, id, MediaStore.Images.Thumbnails.MINI_KIND, null)
      } catch (e: Exception) {
        Log.v(Photo::class.java.name, "Error generating thumbnail for photo $id.")
      }
    }
  }

  companion object {
    @Suppress("unused")  // Used as part of Parcellable
    @JvmField
    val CREATOR = object : Parcelable.Creator<Photo> {
      override fun createFromParcel(parcel: Parcel): Photo = Photo(parcel)
      override fun newArray(size: Int): Array<Photo?> = arrayOfNulls(size)
    }
  }
}
