package com.lytefast.flexinput.model

import android.content.ContentResolver
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.CallSuper

import com.facebook.common.util.HashCodeUtil
import com.lytefast.flexinput.utils.FileUtils


/**
 * Represents an attachable resource in the form of [Uri].
 *
 * @author Sam Shih
 */
open class Attachment<T> (
  val id: Long,
  val uri: Uri,
  val displayName: String,
  val data: T? = null
): Parcelable {

  constructor(parcelIn: Parcel) : this(
    id = parcelIn.readLong(),
    uri = parcelIn.readParcelable(Uri::class.java.classLoader),
    displayName = parcelIn.readString(),
    data = null  // this shouldn't be required anyways.
  )

  override fun equals(obj: Any?): Boolean {
    if (obj != null && obj is Attachment<*>) {
      return this.id == obj.id && this.uri == obj.uri
    }
    return false
  }

  override fun hashCode(): Int {
    return HashCodeUtil.hashCode(id, uri)
  }

  override fun describeContents(): Int {
    return 0
  }

  @CallSuper
  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeLong(id)
    dest.writeParcelable(uri, flags)
    dest.writeString(displayName)
    //    dest.writeParcelable(data, flags);
  }

  companion object {

    @JvmStatic
    val CREATOR: Parcelable.Creator<Attachment<*>> = object : Parcelable.Creator<Attachment<*>> {
      override fun createFromParcel(parcelIn: Parcel): Attachment<*> = Attachment<Any>(parcelIn)

      override fun newArray(size: Int): Array<Attachment<*>?> = arrayOfNulls(size)
    }

    @JvmStatic
    fun fromUri(resolver: ContentResolver, uri: Uri): Attachment<*> {
      val fileName = FileUtils.getFileNameFromUri(resolver, uri)
      return Attachment(uri.hashCode().toLong(), uri, fileName, null)
    }
  }
}
