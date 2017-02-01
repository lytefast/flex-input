package com.lytefast.flexinput.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.CallSuper;

import com.facebook.common.util.HashCodeUtil;


/**
 * Represents an attachable resource in the form of {@link Uri}.
 *
 * @author Sam Shih
 */
public class Attachment<T> implements Parcelable {
  protected final long id;
  protected final Uri uri;
  protected final String displayName;
  protected final T data;


  public Attachment(final long id, final Uri uri, final String displayName, T data) {
    this.id = id;
    this.uri = uri;
    this.displayName = displayName;
    this.data = data;
  }

  protected Attachment(Parcel in) {
    this.id = in.readLong();
    this.uri = in.readParcelable(Uri.class.getClassLoader());
    this.displayName = in.readString();
    this.data = null;  // this shouldn't be required anyways.
  }

  //region Getters

  public static final Creator<Attachment> CREATOR = new Creator<Attachment>() {
    @Override
    public Attachment createFromParcel(Parcel in) {
      return new Attachment(in);
    }

    @Override
    public Attachment[] newArray(int size) {
      return new Attachment[size];
    }
  };

  public long getId() {
    return id;
  }

  public Uri getUri() {
    return uri;
  }

  public String getDisplayName() {
    return displayName;
  }

  public T getData() {
    return data;
  }

  //endregion

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Attachment) {
      Attachment other = (Attachment) obj;
      return this.id == other.id && this.uri.equals(other.uri);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return HashCodeUtil.hashCode(id, uri);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @CallSuper
  @Override
  public void writeToParcel(final Parcel dest, final int flags) {
    dest.writeLong(id);
    dest.writeParcelable(uri, flags);
    dest.writeString(displayName);
//    dest.writeParcelable(data, flags);
  }
}
