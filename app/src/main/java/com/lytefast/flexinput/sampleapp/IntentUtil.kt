package com.lytefast.flexinput.sampleapp

import android.content.ClipData
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.lytefast.flexinput.model.Attachment
import com.lytefast.flexinput.model.Attachment.Companion.toAttachment

/**
 * @author Sam Shih
 */
object IntentUtil {

  @JvmStatic
  fun Intent.consumeSendIntent(contentResolver: ContentResolver): Attachment<*>? {
    if (this.action !== Intent.ACTION_SEND) {
      return null
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
        && this.clipData != null
        && this.clipData.itemCount > 0) {
      val item = this.clipData.getItemAt(0)
      this.clipData = ClipData.newPlainText("", "")

      item.uri.toAttachment(contentResolver)
    } else {
      val uri = this.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
      uri?.let {
        this.removeExtra(Intent.EXTRA_STREAM)
        it.toAttachment(contentResolver)
      }
    }

  }
}