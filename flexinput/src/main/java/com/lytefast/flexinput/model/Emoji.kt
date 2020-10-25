package com.lytefast.flexinput.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.android.parcel.Parcelize


/**
 * Simple representation of a unicode emoji.
 *
 * @author Sam Shih
 */
@Parcelize
data class Emoji(
    val strValue: String,
    val aliases: Array<String>
) : Parcelable


@Parcelize
data class EmojiCategory(
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
    val emojis: List<Emoji>
) : Parcelable
