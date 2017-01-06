package com.lytefast.flexinput.fragment;

import com.lytefast.flexinput.model.Photo;

import java.util.Collection;


/**
 * Listener for when attachments are added/removed.
 *
 * @param <T> The type of attachment. Refer to the {@link com.lytefast.flexinput.model} package for types.
 */
public interface AttachmentSelector<T> {
  Collection<T> getSelectedAttachments();
  void clearSelectedAttachments();
}
