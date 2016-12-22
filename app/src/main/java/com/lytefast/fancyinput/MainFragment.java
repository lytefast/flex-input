package com.lytefast.fancyinput;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.plus.PlusOneButton;
import com.lytefast.fancyinput.widget.FancyInput;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


public class MainFragment extends Fragment {

  @BindView(R.id.fancy_input) FancyInput fancyInput;

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
    View view = inflater.inflate(R.layout.fragment_main, container, false);
    return view;
  }

  @Override
  public void onStart() {
    super.onStart();
    unbinder = ButterKnife.bind(this, getView());

    fancyInput.initContentPages(getFragmentManager());
  }

  @Override
  public void onStop() {
    unbinder.unbind();
    super.onStop();
  }
}
