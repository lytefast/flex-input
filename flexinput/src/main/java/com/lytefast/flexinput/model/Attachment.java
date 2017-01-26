package com.lytefast.flexinput.model;

import android.net.Uri;
import android.provider.MediaStore;

import com.facebook.common.util.HashCodeUtil;


/**
 * Represents an attachable resource in the form of {@link Uri}.
 *
 * @author Sam Shih
 */
public class Attachment<T> {
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

  //region Getters

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
}
