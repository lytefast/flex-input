package com.lytefast.flexinput.sampleapp;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
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

    View view = getLayoutInflater().inflate(R.layout.actionbar_layout, null);
    getSupportActionBar().setDisplayShowCustomEnabled(true);
    getSupportActionBar().setCustomView(view);

    SwitchCompat nightModeToggle = view.findViewById(R.id.toggle);
    nightModeToggle.setChecked(true);
    nightModeToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        AppCompatDelegate.setDefaultNightMode(
            isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
      }
    });
  }
}
