package com.lytefast.flexinput.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailUtils {

  @RequiresApi(Build.VERSION_CODES.Q)
  suspend fun getThumbnailQ(contentResolver: ContentResolver, uri: Uri,
                            width: Int, height: Int,
                            cancelSignal: CancellationSignal? = null)
      : Bitmap? = withContext(Dispatchers.IO) {
    try {
      Log.d("Thumbnail", "Loading thumbnail $uri")
      contentResolver.loadThumbnail(uri, Size(width, height), cancelSignal)
    } catch (e: Exception) {
      Log.e("Thumbnail", "Thumbnail Failed to load $e")
      null
    }
  }
}