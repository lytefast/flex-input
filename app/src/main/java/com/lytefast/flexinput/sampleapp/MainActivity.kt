package com.lytefast.flexinput.sampleapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import com.facebook.drawee.backends.pipeline.Fresco


class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // This should be in the Application class so it only gets invoked once
    Fresco.initialize(this)

    // Play around with the below to see how it looks like in each theme
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    val view = layoutInflater.inflate(R.layout.actionbar_layout, null)
    supportActionBar?.setDisplayShowCustomEnabled(true)
    supportActionBar?.customView = view
    val nightModeToggle: SwitchCompat = view.findViewById(R.id.toggle)
    nightModeToggle.isChecked = true
    nightModeToggle.setOnCheckedChangeListener { _, isChecked ->
      AppCompatDelegate.setDefaultNightMode(
          if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
    }
  }
}