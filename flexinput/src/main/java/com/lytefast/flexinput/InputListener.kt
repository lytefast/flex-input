package com.lytefast.flexinput

import android.text.Editable
import com.lytefast.flexinput.model.Attachment

/**
 * Handles events that occur within the [FlexInputFragment] widget.
 *
 * @author Sam Shih
 */
fun interface InputListener {
  /**
   * User has chosen to send the current contents of the [FlexInputFragment]
   *
   * @return True if the attaachment and data should be cleared. False to keep
   */
  fun onSend(data: Editable?, attachments: List<Attachment<*>?>?): Boolean
}