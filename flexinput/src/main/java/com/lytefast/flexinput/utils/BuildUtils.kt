package com.lytefast.flexinput.utils

import android.os.Build

object BuildUtils {
  fun isAndroidQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}