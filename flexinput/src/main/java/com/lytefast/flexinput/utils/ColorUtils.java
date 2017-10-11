package com.lytefast.flexinput.utils;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;


/**
 * Useful methods to manage colors.
 *
 * @author Sam Shih
 */
public final class ColorUtils {
  /**
   * Cause android support libraries don't bother with this stuff...
   *
   * @return a drawable for the given attribute. If it's a color and not a reference will create a {@link ColorDrawable}.
   */
  public static Drawable getColor(Context context, @AttrRes int colorAttrId) {
    TypedValue value = new TypedValue();
    context.getTheme().resolveAttribute(colorAttrId, value, true);

    if (value.resourceId == 0) {
      return new ColorDrawable(value.data);
    }
    return ContextCompat.getDrawable(context, value.resourceId);
  }
}
