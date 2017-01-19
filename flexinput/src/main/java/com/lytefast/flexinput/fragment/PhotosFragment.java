package com.lytefast.flexinput.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lytefast.flexinput.R;
import com.lytefast.flexinput.R2;
import com.lytefast.flexinput.adapters.PhotoCursorAdapter;
import com.lytefast.flexinput.model.Photo;
import com.lytefast.flexinput.utils.SelectionCoordinator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


/**
 * Fragment that displays the recent photos on the phone for selection.
 *
 * @author Sam Shih
 */
public class PhotosFragment extends Fragment {


  private final SelectionCoordinator<Photo> selectionCoordinator = new SelectionCoordinator<>();

  @BindView(R2.id.swipeRefreshLayout) SwipeRefreshLayout swipeRefreshLayout;
  @BindView(R2.id.list) RecyclerView recyclerView;
  private Unbinder unbinder;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public PhotosFragment() {
  }

  @Override
  public void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Fragment parentFrag = getParentFragment();
    if (parentFrag instanceof FlexInputFragment) {
      FlexInputFragment flexInputFrag = (FlexInputFragment) parentFrag;
      flexInputFrag.addSelectionCoordinator(selectionCoordinator);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_recycler_view, container, false);
    unbinder = ButterKnife.bind(this, view);

    final PhotoCursorAdapter adapter = new PhotoCursorAdapter(
        getContext().getContentResolver(), selectionCoordinator);
    recyclerView.setAdapter(adapter);
    recyclerView.invalidateItemDecorations();

    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override
      public void onRefresh() {
        adapter.loadPhotos();
        swipeRefreshLayout.setRefreshing(false);
      }
    });
    return view;
  }

  @Override
  public void onDestroyView() {
    unbinder.unbind();
    super.onDestroyView();
  }
}