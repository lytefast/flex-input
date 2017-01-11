package com.lytefast.flexinput.sampleapp;

import android.util.JsonReader;
import android.util.Log;

import com.lytefast.flexinput.emoji.Emoji;
import com.lytefast.flexinput.fragment.EmojiCategoryPagerFragment;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Loads and ordered {@link com.lytefast.flexinput.emoji.Emoji.EmojiCategory} list from the asset folder.
 * The {@link Emoji}s loaded are unicode emoji representations.
 *
 * @author Sam Shih
 */
public class UnicodeEmojiCategoryPagerFragment extends EmojiCategoryPagerFragment {

  public static final String ASSET_PATH_EMOJIS = "emojis.json";

  @Override
  public List<Emoji.EmojiCategory> buildEmojiCategoryData(final Emoji[] emojis) {
    JsonReader jsonReader = null;
    try {
      final Reader reader = new InputStreamReader(
          getContext().getAssets().open(ASSET_PATH_EMOJIS), "UTF-8");
      jsonReader = new JsonReader(reader);
      jsonReader.beginObject();

      ArrayList<Emoji.EmojiCategory> emojiCategories = new ArrayList<>();
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

  private Emoji.EmojiCategory readEmojiCategory(final JsonReader jsonReader) throws IOException {
    String name = jsonReader.nextName();

    jsonReader.beginArray();

    ArrayList<Emoji> emojis = new ArrayList<>();
    while (jsonReader.hasNext()) {
      emojis.add(readEmoji(jsonReader));
    }
    emojis.trimToSize();

    jsonReader.endArray();
    return new Emoji.EmojiCategory(name, R.drawable.ic_audiotrack_light, emojis);
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
}
