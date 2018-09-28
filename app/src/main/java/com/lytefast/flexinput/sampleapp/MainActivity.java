package com.lytefast.flexinput.sampleapp;


import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.lytefast.flexinput.adapters.EmptyListAdapter;


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

  @SuppressWarnings("unused")
  public static class TestEmptyListAdapter extends EmptyListAdapter {

    public TestEmptyListAdapter(@LayoutRes int itemLayoutId, @IdRes int actionBtnId, View.OnClickListener onClickListener) {
      super(itemLayoutId, actionBtnId, onClickListener);
    }

    @Override @NonNull
    public EmptyListAdapter.ViewHolder onCreateViewHolder(final @NonNull ViewGroup parent, final int viewType) {
      return super.onCreateViewHolder(parent, viewType);
    }
  }
}
