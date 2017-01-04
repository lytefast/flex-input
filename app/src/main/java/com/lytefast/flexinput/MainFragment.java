package com.lytefast.flexinput;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.lytefast.flexinput.widget.FlexInput;
import com.lytefast.flexinput.widget.InputListener;
import com.lytefast.flexinput.widget.KeyboardManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


public class MainFragment extends Fragment {

  @BindView(R.id.fancy_input) FlexInput flexInput;

  private Unbinder unbinder;

  public MainFragment() {
    // Required empty public constructor
  }


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_message_main, container, false);
    return view;
  }

  @Override
  public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    unbinder = ButterKnife.bind(this, getView());

    final InputMethodManager imm =
        (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

    flexInput
        .initContentPages(getFragmentManager())
        .setInputListener(flexInputListener)
        .setKeyboardManager(new KeyboardManager() {
          @Override
          public void requestDisplay() {
            flexInput.requestFocus();
            imm.showSoftInput(flexInput, InputMethodManager.SHOW_IMPLICIT);
          }

          @Override
          public void requestHide() {
            imm.hideSoftInputFromWindow(flexInput.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
          }
        });
  }

  @Override
  public void onDestroyView() {
    unbinder.unbind();
    super.onDestroyView();
  }

  private final InputListener flexInputListener = new InputListener() {
    @Override
    public void onSend(final Editable data) {
      Toast.makeText(getContext(), "Text Sent: " + data.toString(), Toast.LENGTH_SHORT).show();
    }
  };
}
