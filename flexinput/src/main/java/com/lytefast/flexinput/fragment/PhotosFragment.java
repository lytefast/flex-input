package com.lytefast.flexinput.fragment;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.lytefast.flexinput.R;
import com.lytefast.flexinput.adapters.OnItemClickListener;
import com.lytefast.flexinput.adapters.PhotoCursorAdapter;


/**
 * Fragment that displays the recent photos on the phone for selection.
 */
public class PhotosFragment extends Fragment {

  private RecyclerView recyclerView;

  // TODO: Customize parameter initialization
  @SuppressWarnings("unused")
  public static PhotosFragment newInstance(int columnCount) {
    PhotosFragment fragment = new PhotosFragment();
    return fragment;
  }

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
      PhotoCursorAdapter adapter = new PhotoCursorAdapter(getContext().getContentResolver(), cursor);
      recyclerView.setAdapter(adapter);
      recyclerView.invalidateItemDecorations();

      adapter.setOnItemClickListener(new OnItemClickListener<PhotoCursorAdapter.Photo>() {
        @Override
        public void onItemClicked(final PhotoCursorAdapter.Photo item, final int position) {
          Toast.makeText(getContext(),
              "Toggle[" + position + "]: " + item.displayName, Toast.LENGTH_SHORT).show();
        }
      });
    }
  }
}