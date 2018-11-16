package com.lytefast.flexinput.managers

import android.widget.EditText


/**
 * Defines interactions with the on screen keyboard.
 *
 * @author Sam Shih
 */
interface KeyboardManager {
  fun requestDisplay(editText: EditText)
  fun requestHide()
}
