package com.lytefast.flexinput;

import android.text.Editable;

import com.lytefast.flexinput.fragment.FlexInputFragment;
import com.lytefast.flexinput.model.Attachment;

import java.util.List;


/**
 * Handles events that occur within the {@link FlexInputFragment} widget.
 *
 * @author Sam Shih
 */
public interface InputListener {
  /**
   * User has chosen to send the current contents of the {@link FlexInputFragment}
   *
   * @return True if the attaachment and data should be cleared. False to keep
   */
  boolean onSend(Editable data, List<? extends Attachment> attachments);
}
