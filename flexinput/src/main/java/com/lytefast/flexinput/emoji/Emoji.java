package com.lytefast.flexinput.emoji;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;

import java.util.ArrayList;
import java.util.List;


/**
 * Simple representation of a unicode emoji.
 *
 * @author Sam Shih
 */

public class Emoji implements Parcelable {

  public final String strValue;
  public final String[] aliases;

  public Emoji(final String strValue, final String[] aliases) {
    this.aliases = aliases;
    this.strValue = strValue;
  }

  //region Parcelable Impl
  protected Emoji(Parcel in) {
    strValue = in.readString();
    aliases = in.createStringArray();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(final Parcel dest, final int flags) {
    dest.writeString(strValue);
    dest.writeStringArray(aliases);
  }

  public static final Creator<Emoji> CREATOR = new Creator<Emoji>() {
    @Override
    public Emoji createFromParcel(Parcel in) {
      return new Emoji(in);
    }

    @Override
    public Emoji[] newArray(int size) {
      return new Emoji[size];
    }
  };
  //endregion

  public static class EmojiCategory implements Parcelable {
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

    //region Parcelable Impl
    protected EmojiCategory(Parcel in) {
      this.name = in.readString();
      this.icon = in.readInt();

      this.emojis = new ArrayList<>();
      in.readTypedList(emojis, Emoji.CREATOR);
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
      dest.writeString(name);
      dest.writeInt(icon);
      dest.writeTypedList(emojis);
    }

    public static final Creator<EmojiCategory> CREATOR = new Creator<EmojiCategory>() {
      @Override
      public EmojiCategory createFromParcel(Parcel in) {
        return new EmojiCategory(in);
      }

      @Override
      public EmojiCategory[] newArray(int size) {
        return new EmojiCategory[size];
      }
    };
    //endregion
  }
}
