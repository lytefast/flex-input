package com.lytefast.flexinput.sampleapp

import android.util.Log
import com.lytefast.flexinput.fragment.FlexInputFragment
import com.lytefast.flexinput.model.Attachment


/**
 * @author Sam Shih
 */
class CustomFlexInputFragment : FlexInputFragment() {

  /**
   * Do not use this directly. Use the tracked [#addExternalAttachment(Attachment, String)]
   */
  override fun addExternalAttachment(attachment: Attachment<Any>) {
    addExternalAttachment(attachment, "default")
  }

  fun addExternalAttachment(attachment: Attachment<Any>, source: String) {
    super.addExternalAttachment(attachment)

    Log.i(javaClass.simpleName, "Attachment[${attachment.uri}] sent from $source")
  }
}