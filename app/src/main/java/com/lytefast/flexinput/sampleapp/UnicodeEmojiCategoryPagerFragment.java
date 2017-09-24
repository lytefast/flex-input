package com.lytefast.flexinput.sampleapp;

import android.support.annotation.DrawableRes;
import android.util.JsonReader;
import android.util.Log;

import com.lytefast.flexinput.fragment.EmojiCategoryPagerFragment;
import com.lytefast.flexinput.model.Emoji;
import com.lytefast.flexinput.model.EmojiCategory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Loads and ordered {@link EmojiCategory} list from the asset folder.
 * The {@link Emoji}s loaded are unicode emoji representations.
 *
 * @author Sam Shih
 */
public class UnicodeEmojiCategoryPagerFragment extends EmojiCategoryPagerFragment {

  public static final String ASSET_PATH_EMOJIS = "emojis.json";

  @Override
  public List<EmojiCategory> buildEmojiCategoryData() {
    JsonReader jsonReader = null;
    try {
      final Reader reader = new InputStreamReader(
          getContext().getAssets().open(ASSET_PATH_EMOJIS), "UTF-8");
      jsonReader = new JsonReader(reader);
      jsonReader.beginObject();

      ArrayList<EmojiCategory> emojiCategories = new ArrayList<>();
      while (jsonReader.hasNext()) {
        emojiCategories.add(readEmojiCategory(jsonReader));
      }

      jsonReader.endObject();
      return emojiCategories;
    } catch (IOException e) {
      Log.e(getClass().getName(), "Unable to load unicode emoji.", e);
    } finally {
      if (jsonReader != null) {
        try {
          jsonReader.close();
        } catch (IOException e) {
          Log.e(getClass().getName(), "Error closing emoji list reader.", e);
        }
      }
    }

    return Collections.EMPTY_LIST;
  }

  private EmojiCategory readEmojiCategory(final JsonReader jsonReader) throws IOException {
    String name = jsonReader.nextName();

    jsonReader.beginArray();

    ArrayList<Emoji> emojis = new ArrayList<>();
    while (jsonReader.hasNext()) {
      emojis.add(readEmoji(jsonReader));
    }
    emojis.trimToSize();

    jsonReader.endArray();
    return new EmojiCategory(name, getCategoryIcon(name), emojis);
  }

  private Emoji readEmoji(final JsonReader jsonReader) throws IOException {
    jsonReader.beginObject();

    String strValue = null;
    ArrayList<String> aliases = new ArrayList<>();

    while (jsonReader.hasNext()) {
      switch (jsonReader.nextName()) {
        case "names":
          jsonReader.beginArray();
          while (jsonReader.hasNext()) {
            aliases.add(jsonReader.nextString());
          }
          jsonReader.endArray();
          break;
        case "surrogates":
          strValue = jsonReader.nextString();
          break;
        default:
          jsonReader.skipValue();
          break;
      }
    }

    jsonReader.endObject();
    return new Emoji(strValue, aliases.toArray(new String[aliases.size()]));
  }

  @DrawableRes
  private static int getCategoryIcon(String categoryName) {
    switch (categoryName) {
      case "people":
        return R.drawable.ic_mood_black_24dp;
      case "nature":
        return R.drawable.ic_local_florist_black_24dp;
      case "food":
        return R.drawable.ic_local_pizza_black_24dp;
      case "activity":
        return R.drawable.ic_beach_access_black_24px;
      case "travel":
        return R.drawable.ic_directions_car_black_24dp;
      case "objects":
        return R.drawable.ic_stars_black_24dp;
      case "symbols":
        return R.drawable.ic_create_black_24dp;
      case "flags":
        return R.drawable.ic_assistant_photo_black_24dp;
      default:
        return R.drawable.ic_mood_black_24dp;
    }
  }
}
