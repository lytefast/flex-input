package com.lytefast.flexinput.model;

import android.net.Uri;
import android.provider.MediaStore;

import com.facebook.common.util.HashCodeUtil;


/**
 * Represents an attachable resource in the form of {@link Uri}.
 */
public class Attachment {
    public final long id;
    public final Uri uri;
    public final String displayName;


    public Attachment(final long id, final Uri uri, final String displayName) {
      this.id = id;
      this.uri = uri;
      this.displayName = displayName;
    }

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
