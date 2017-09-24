package com.lytefast.flexinput.model

import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.DrawableRes


/**
 * Simple representation of a unicode emoji.
 *
 * @author Sam Shih
 */

class Emoji : Parcelable {

  val strValue: String
  val aliases: Array<String>

  constructor(strValue: String, aliases: Array<String>) {
    this.aliases = aliases
    this.strValue = strValue
  }

  //region Parcelable Impl
  constructor(parcelIn: Parcel) {
    strValue = parcelIn.readString()
    aliases = parcelIn.createStringArray()
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeString(strValue)
    dest.writeStringArray(aliases)
  }

  companion object {

    @JvmStatic
    val CREATOR: Parcelable.Creator<Emoji> = object : Parcelable.Creator<Emoji> {
      override fun createFromParcel(`in`: Parcel): Emoji {
        return Emoji(`in`)
      }

      override fun newArray(size: Int): Array<Emoji?> {
        return arrayOfNulls(size)
      }
    }
  }

  //endregion
}


class EmojiCategory(
    /**
     * String representation of this category.
     */
    val name: String,
    /**
     * Visual representation of this category.
     */
    @DrawableRes
    val icon: Int,
    /**
     * Emojis that are considered part of this category. This does not imply exclusitivity.
     */
    val emojis: List<Emoji>) : Parcelable {

  //region Parcelable Impl
  constructor(parcelIn: Parcel)
      : this(parcelIn.readString(), parcelIn.readInt(), listOf<Emoji>()) {
    parcelIn.readTypedList(emojis, Emoji.CREATOR)
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeString(name)
    dest.writeInt(icon)
    dest.writeTypedList(emojis)
  }

  companion object {

    @JvmStatic
    val CREATOR: Parcelable.Creator<EmojiCategory> = object : Parcelable.Creator<EmojiCategory> {
      override fun createFromParcel(parcelIn: Parcel): EmojiCategory {
        return EmojiCategory(parcelIn)
      }

      override fun newArray(size: Int): Array<EmojiCategory?> {
        return arrayOfNulls(size)
      }
    }
  }
  //endregion
}
