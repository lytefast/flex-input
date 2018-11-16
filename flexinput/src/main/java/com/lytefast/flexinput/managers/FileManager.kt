package com.lytefast.flexinput.managers

import android.content.Context
import android.net.Uri

import java.io.File


/**
 * @author Sam Shih
 */
interface FileManager {
  fun newImageFile(): File

  fun toFileProviderUri(context: Context, file: File): Uri
}
