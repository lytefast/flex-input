package com.lytefast.flexinput.managers

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/**
 * A basic implementation of the [FileManager] that uses a specified [FileProvider].
 *
 * @author Sam Shih
 */
@Suppress("MemberVisibilityCanBePrivate")
open class SimpleFileManager @JvmOverloads constructor(
    protected val providerAuthority: String,
    protected val imageDirName: String,
    protected val imagePrefix: String = "JPEG_") : FileManager {

  protected val imagesDirectory: File
    get() {
      val file = File(
          Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
          imageDirName)
      if (!file.mkdirs() && !file.isDirectory) {
        Log.e(TAG, "Directory not created")
      }
      return file
    }

  override fun newImageFile(): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val imageFileName = "$imagePrefix$timeStamp.jpg"
    return File(imagesDirectory, imageFileName)
  }

  override fun toFileProviderUri(context: Context, file: File): Uri =
      FileProvider.getUriForFile(context, providerAuthority, file)

  companion object {
    private val TAG = SimpleFileManager::class.java.canonicalName
  }
}
