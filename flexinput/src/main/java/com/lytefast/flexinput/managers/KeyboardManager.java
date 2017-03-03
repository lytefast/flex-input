package com.lytefast.flexinput.managers;

import android.widget.EditText;


/**
 * Defines interactions with the on screen keyboard.
 *
 * @author Sam Shih
 */
public interface KeyboardManager {
  void requestDisplay(final EditText editText);
  void requestHide();
}
