package com.lytefast.flexinput;

import android.text.Editable;


/**
 * Handles events that occur within the {@link FlexInput} widget.
 */
public interface InputListener {
  /**
   * User has chosen to send the current contents of the {@link FlexInput}
   */
  void onSend(Editable data);
}
