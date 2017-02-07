package com.lytefast.flexinput.sampleapp;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.SwitchCompat;
import android.widget.CompoundButton;

import com.facebook.drawee.backends.pipeline.Fresco;


public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // This should be in the Application class so it only gets invoked once
    Fresco.initialize(this);

    // Play around with the below to see how it looks like in each theme
//     AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

    SwitchCompat nightModeToggle = new SwitchCompat(this, null);
    nightModeToggle.setChecked(true);
    nightModeToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        AppCompatDelegate.setDefaultNightMode(
            isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
      }
    });
    getSupportActionBar().setCustomView(nightModeToggle);
  }
}
