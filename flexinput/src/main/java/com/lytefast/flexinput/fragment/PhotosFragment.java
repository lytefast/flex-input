package com.lytefast.flexinput.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lytefast.flexinput.R;
import com.lytefast.flexinput.R2;
import com.lytefast.flexinput.adapters.PhotoCursorAdapter;
import com.lytefast.flexinput.events.ClearAttachmentsEvent;
import com.lytefast.flexinput.events.ItemClickedEvent;
import com.lytefast.flexinput.model.Photo;
import com.lytefast.flexinput.utils.SelectionCoordinator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


/**
 * Fragment that displays the recent photos on the phone for selection.
 *
 * @author Sam Shih
 */
public class PhotosFragment extends Fragment {

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
  public void onStart() {
    super.onStart();
    EventBus.getDefault().register(this);
  }

  @Override
  public void onStop() {
    EventBus.getDefault().unregister(this);
    super.onStop();
  }

  @Override
  public void onDestroyView() {
    unbinder.unbind();
    super.onDestroyView();
  }

  //region Events
  @Subscribe
  void handleClearAttachmentEvent(ClearAttachmentsEvent evt) {
    selectionCoordinator.clearSelectedItems();
  }
  //endregion

  private final SelectionCoordinator<Photo> selectionCoordinator = new SelectionCoordinator<Photo>() {
    @Override
    public void onItemSelected(final Photo item) {
      EventBus.getDefault().post(new ItemClickedEvent<>(item));
      Log.d(getClass().getCanonicalName(), "Select[" + item.id + "]: " + item.displayName);
    }

    @Override
    public void onItemUnselected(final Photo item) {
      EventBus.getDefault().post(new ItemClickedEvent<>(item));
      Log.d(getClass().getCanonicalName(), "Remove[" + item.id + "]: " + item.displayName);
    }
  };
}