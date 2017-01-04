package com.lytefast.flexinput.fragment;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lytefast.flexinput.R;

import java.util.Arrays;
import java.util.List;


/**
 * A fragment representing a list of Items.
 */
public class RecyclerViewFragment extends Fragment {

  private static final List<?> DUMMY_ITEMS = Arrays.asList(1, 2, 3, 4);

  // TODO: Customize parameter initialization
  @SuppressWarnings("unused")
  public static RecyclerViewFragment newInstance(int columnCount) {
    RecyclerViewFragment fragment = new RecyclerViewFragment();
    return fragment;
  }

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public RecyclerViewFragment() {
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_recycler_view, container, false);

    // Set the adapter
    if (view instanceof RecyclerView) {
      Context context = view.getContext();
      RecyclerView recyclerView = (RecyclerView) view;
      recyclerView.setAdapter(new Adapter(DUMMY_ITEMS));
    }
    return view;
  }

}
