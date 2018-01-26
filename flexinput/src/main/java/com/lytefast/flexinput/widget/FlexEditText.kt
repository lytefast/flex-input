package com.lytefast.flexinput.widget

import android.content.Context
import android.os.Build
import android.support.v13.view.inputmethod.EditorInfoCompat
import android.support.v13.view.inputmethod.InputConnectionCompat
import android.support.v13.view.inputmethod.InputContentInfoCompat
import android.support.v7.widget.AppCompatEditText
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection


/**
 * Basic [AppCompatEditText] component that understands how to consume media from the IME keyboard.
 *
 * @author Sam Shih
 */
open class FlexEditText : AppCompatEditText {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  /**
   * Handle the data from the edit text asynchronously.
   *
   * [InputContentInfo.releasePermission()] should be called when resource is discarded.
   */
  var inputContentHandler: ((InputContentInfoCompat) -> Unit)? = null

  override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection? =
    super.onCreateInputConnection(editorInfo)?.let {
      EditorInfoCompat.setContentMimeTypes(editorInfo, arrayOf("image/*"))

      val callback = InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, _ ->
        // read and display inputContentInfo asynchronously
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
            && (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
          try {
            inputContentInfo.requestPermission()
          } catch (e: Exception) {
            return@OnCommitContentListener false // return false if failed
          }
        }

        inputContentHandler?.invoke(inputContentInfo)
        true  // return true if succeeded
      }
      InputConnectionCompat.createWrapper(it, editorInfo, callback)
    }
}