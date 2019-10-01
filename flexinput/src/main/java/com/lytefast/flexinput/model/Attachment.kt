package com.lytefast.flexinput.model

import android.content.ContentResolver
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.CallSuper
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.facebook.common.util.HashCodeUtil
import com.lytefast.flexinput.utils.FileUtils.getFileName
import java.io.File


/**
 * Represents an attachable resource in the form of [Uri].
 *
 * @author Sam Shih
 */
open class Attachment<out T>(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val data: T? = null
) : Parcelable {

  constructor(parcelIn: Parcel) : this(
      id = parcelIn.readLong(),
      uri = parcelIn.readParcelable(Uri::class.java.classLoader) ?: Uri.EMPTY,
      displayName = parcelIn.readString() ?: "",
      data = null  // this shouldn't be required anyways.
  )

  override fun equals(other: Any?): Boolean {
    if (other != null && other is Attachment<*>) {
      return this.id == other.id && this.uri == other.uri
    }
    return false
  }

  override fun hashCode(): Int = HashCodeUtil.hashCode(id, uri)

  override fun describeContents(): Int = 0

  @CallSuper
  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeLong(id)
    dest.writeParcelable(uri, flags)
    dest.writeString(displayName)
    //    dest.writeParcelable(data, flags);
  }

  companion object {

    @Suppress("unused")  // Used as part of Parcellable
    @JvmField
    val CREATOR = object : Parcelable.Creator<Attachment<*>> {
      override fun createFromParcel(parcelIn: Parcel): Attachment<*> = Attachment<Any>(parcelIn)

      override fun newArray(size: Int): Array<Attachment<*>?> = arrayOfNulls(size)
    }

    @JvmStatic
    fun Uri.toAttachment(resolver: ContentResolver): Attachment<Uri> {
      val fileName = getFileName(resolver) ?: hashCode().toString()
      return Attachment(hashCode().toLong(), this, fileName, null)
    }

    @JvmStatic
    fun InputContentInfoCompat.toAttachment(
        resolver: ContentResolver,
        appendExtension: Boolean = false,
        defaultName: String = "unknown"): Attachment<InputContentInfoCompat> {
      val rawFileName = contentUri.getQueryParameter("fileName") ?: defaultName
      val fileName = rawFileName.substringAfterLast(File.separatorChar)

      val attachmentName = if (appendExtension) {
        val type = description.getMimeType(0)
            ?: contentUri.getQueryParameter("mimeType")
            ?: resolver.getType(contentUri)
        type?.let {
          "$fileName.${it.substringAfterLast('/')}"
        } ?: fileName
      } else {
        fileName
      }

      return Attachment(
          contentUri.hashCode().toLong(),
          contentUri,
          attachmentName,
          this)
    }
  }
}
