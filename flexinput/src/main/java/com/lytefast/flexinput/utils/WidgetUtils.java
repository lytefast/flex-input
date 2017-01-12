package com.lytefast.flexinput.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.design.widget.TabLayout;
import android.support.v4.graphics.drawable.DrawableCompat;

import com.lytefast.flexinput.R;

import java.util.Arrays;
import java.util.List;


/**
 * @author Sam Shih
 */
public class WidgetUtils {

  public static ColorStateList getColorStateList(final Context context, final @ColorRes int colorSelector) {
    ColorStateList colors;
    if (Build.VERSION.SDK_INT >= 23) {
      colors = context.getColorStateList(colorSelector);
    } else {
      colors = context.getResources().getColorStateList(colorSelector);
    }
    return colors;
  }
}
