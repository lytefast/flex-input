package com.lytefast.flexinput.utils

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

import com.lytefast.flexinput.model.Attachment

import java.io.File


/**
 * @author Sam Shih
 */
object FileUtils {

  @JvmStatic
  fun File.toAttachment(): Attachment<File> =
      Attachment(this.hashCode().toLong(), Uri.fromFile(this), this.name, this)

  @JvmStatic
  @Throws(IllegalArgumentException::class)
  fun Uri.getFileName(contentResolver: ContentResolver): String {
    when (this.scheme) {
      ContentResolver.SCHEME_FILE -> {
        val file = File(this.path)
        return file.name
      }
      ContentResolver.SCHEME_CONTENT -> {
        contentResolver.query(this, null, null, null, null)?.use { cursor ->
          if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columnIndex >= 0) {
              return cursor.getString(columnIndex) ?: this.lastPathSegment
            }
          }
        }
        return this.lastPathSegment
      }
      else -> return this.lastPathSegment
    }
  }

  @JvmStatic
  fun File.getFileSize(): String {
    val sizeInKb = this.length() / 1024
    if (sizeInKb < 1024) {
      return "$sizeInKb KB"
    }

    val sizeInMb = sizeInKb / 1024
    if (sizeInMb < 1024) {
      return "$sizeInMb MB"
    }

    val sizeInGb = sizeInMb / 1024
    return "$sizeInGb GB"
  }
}
