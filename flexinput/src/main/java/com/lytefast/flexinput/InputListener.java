package com.lytefast.flexinput;

import android.text.Editable;

import com.lytefast.flexinput.model.Attachment;

import java.util.List;


/**
 * Handles events that occur within the {@link FlexInput} widget.
 *
 * @author Sam Shih
 */
public interface InputListener {
  /**
   * User has chosen to send the current contents of the {@link FlexInput}
   */
  void onSend(Editable data, List<? extends Attachment> attachments);
}
