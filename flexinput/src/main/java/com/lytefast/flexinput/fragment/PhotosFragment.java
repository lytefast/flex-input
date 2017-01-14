package com.lytefast.flexinput.fragment;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lytefast.flexinput.R;
import com.lytefast.flexinput.adapters.PhotoCursorAdapter;
import com.lytefast.flexinput.events.ClearAttachmentsEvent;
import com.lytefast.flexinput.events.ItemClickedEvent;
import com.lytefast.flexinput.model.Photo;
import com.lytefast.flexinput.utils.SelectionCoordinator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;


/**
 * Fragment that displays the recent photos on the phone for selection.
 *
 * @author Sam Shih
 */
public class PhotosFragment extends Fragment {

  private RecyclerView recyclerView;

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

    if (view instanceof RecyclerView) {
      recyclerView = (RecyclerView) view;
      DividerItemDecoration bottomPadding =
          new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
      recyclerView.addItemDecoration(bottomPadding);
    }
    return view;
  }

  @Override
  public void onStart() {
    super.onStart();
    loadPhotos();
    EventBus.getDefault().register(this);
  }

  @Override
  public void onStop() {
    EventBus.getDefault().unregister(this);
    super.onStop();
  }

  // TODO consider moving this to a background thread
  private void loadPhotos() {
    final ContentResolver contentResolver = getContext().getContentResolver();
    final Cursor cursor = contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        new String[]{
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME},
        null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");
    if (cursor != null) {
      final PhotoCursorAdapter adapter = new PhotoCursorAdapter(
          getContext().getContentResolver(), cursor, selectionCoordinator);
      recyclerView.setAdapter(adapter);
      recyclerView.invalidateItemDecorations();
    }
  }

  @Subscribe
  void handleClearAttachmentEvent(ClearAttachmentsEvent evt) {
    selectionCoordinator.clearSelectedItems();
  }

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