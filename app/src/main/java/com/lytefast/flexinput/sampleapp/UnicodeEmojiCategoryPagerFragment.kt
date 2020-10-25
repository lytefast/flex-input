package com.lytefast.flexinput.sampleapp

import android.util.JsonReader
import android.util.Log
import androidx.annotation.DrawableRes
import com.lytefast.flexinput.fragment.EmojiCategoryPagerFragment
import com.lytefast.flexinput.model.Emoji
import com.lytefast.flexinput.model.EmojiCategory
import com.lytefast.flexinput.utils.FlexInputEmojiStateChangeListener
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.util.*

/**
 * Loads and ordered [EmojiCategory] list from the asset folder.
 * The [Emoji]s loaded are unicode emoji representations.
 *
 * @author Sam Shih
 */
class UnicodeEmojiCategoryPagerFragment : EmojiCategoryPagerFragment(), FlexInputEmojiStateChangeListener {

  override fun buildEmojiCategoryData(): List<EmojiCategory> {
    var jsonReader: JsonReader? = null
    try {
      val reader: Reader = InputStreamReader(
          requireContext().assets.open(ASSET_PATH_EMOJIS), "UTF-8")
      jsonReader = JsonReader(reader)
      jsonReader.beginObject()
      val emojiCategories = ArrayList<EmojiCategory>()
      while (jsonReader.hasNext()) {
        emojiCategories.add(readEmojiCategory(jsonReader))
      }
      jsonReader.endObject()
      return emojiCategories
    } catch (e: IOException) {
      Log.e(javaClass.name, "Unable to load unicode emoji.", e)
    } finally {
      if (jsonReader != null) {
        try {
          jsonReader.close()
        } catch (e: IOException) {
          Log.e(javaClass.name, "Error closing emoji list reader.", e)
        }
      }
    }
    return emptyList()
  }

  @Throws(IOException::class)
  private fun readEmojiCategory(jsonReader: JsonReader): EmojiCategory {
    val name = jsonReader.nextName()
    jsonReader.beginArray()
    val emojis = ArrayList<Emoji>()
    while (jsonReader.hasNext()) {
      emojis.add(readEmoji(jsonReader))
    }
    emojis.trimToSize()
    jsonReader.endArray()
    return EmojiCategory(name, getCategoryIcon(name), emojis)
  }

  @Throws(IOException::class)
  private fun readEmoji(jsonReader: JsonReader): Emoji {
    jsonReader.beginObject()
    var strValue: String? = null
    val aliases = ArrayList<String>()
    while (jsonReader.hasNext()) {
      when (jsonReader.nextName()) {
        "names" -> {
          jsonReader.beginArray()
          while (jsonReader.hasNext()) {
            aliases.add(jsonReader.nextString())
          }
          jsonReader.endArray()
        }
        "surrogates" -> strValue = jsonReader.nextString()
        else -> jsonReader.skipValue()
      }
    }
    jsonReader.endObject()
    return Emoji(strValue!!, aliases.toTypedArray())
  }

  override fun isShown(isActive: Boolean) {
    Log.d(this.javaClass.simpleName, "isActive: $isActive")
  }

  companion object {
    private const val ASSET_PATH_EMOJIS = "emojis.json"

    @DrawableRes
    private fun getCategoryIcon(categoryName: String): Int {
      return when (categoryName) {
        "people" -> R.drawable.ic_mood_black_24dp
        "nature" -> R.drawable.ic_local_florist_black_24dp
        "food" -> R.drawable.ic_local_pizza_black_24dp
        "activity" -> R.drawable.ic_beach_access_black_24px
        "travel" -> R.drawable.ic_directions_car_black_24dp
        "objects" -> R.drawable.ic_stars_black_24dp
        "symbols" -> R.drawable.ic_create_black_24dp
        "flags" -> R.drawable.ic_assistant_photo_black_24dp
        else -> R.drawable.ic_mood_black_24dp
      }
    }
  }
}