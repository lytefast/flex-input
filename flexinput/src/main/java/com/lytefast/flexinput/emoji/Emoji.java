package com.lytefast.flexinput.emoji;

import android.support.annotation.DrawableRes;

import java.util.List;


/**
 * @author ${USER_NAME} on 1/10/17.
 */

public class Emoji {

  public final String strValue;
  public final String[] aliases;

  public Emoji(final String strValue, final String[] aliases) {
    this.aliases = aliases;
    this.strValue = strValue;
  }

  public static class EmojiCategory {
    /**
     * String representation of this category.
     */
    public final String name;
    /**
     * Visual representation of this category.
     */
    @DrawableRes public final int icon;
    /**
     * Emojis that are considered part of this category. This does not imply exclusitivity.
     */
    public final List<Emoji> emojis;

    public EmojiCategory(String name, @DrawableRes int icon, List<Emoji> emojis) {
      this.name = name;
      this.icon = icon;
      this.emojis = emojis;
    }
  }
}
