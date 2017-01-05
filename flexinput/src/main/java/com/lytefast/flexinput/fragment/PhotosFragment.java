package com.lytefast.flexinput.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.lytefast.flexinput.R;

import java.util.Arrays;
import java.util.List;


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

    // Set the adapter
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
      recyclerView.setAdapter(new PhotoCursorAdapter(getContext().getContentResolver(), cursor));
    }
  }

  private static class PhotoCursorAdapter extends RecyclerView.Adapter<PhotoCursorAdapter.ViewHolder> {

    private final ContentResolver contentResolver;

    private Cursor cursor;
    private final int colId;
    private final int colData;
    private final int colName;


    PhotoCursorAdapter(ContentResolver contentResolver, @NonNull Cursor cursor) {
      this.contentResolver = contentResolver;
      this.cursor = cursor;

      this.colId = cursor.getColumnIndex(MediaStore.Images.Media._ID);
      this.colData = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
      this.colName = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);

      setHasStableIds(true);
    }

    @Override
    public PhotoCursorAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
      View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.view_grid_image, parent, false);
      return new PhotoCursorAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final PhotoCursorAdapter.ViewHolder holder, final int position) {
      Photo photo = getPhoto(position);
      holder.bind(photo);
    }

    @Override
    public int getItemCount() {
      return cursor.getCount();
    }

    @Override
    public long getItemId(final int position) {
      return getPhoto(position).id;
    }

    @Override
    public void onDetachedFromRecyclerView(final RecyclerView recyclerView) {
      cursor.close();
      super.onDetachedFromRecyclerView(recyclerView);
    }

    public Photo getPhoto(int position) {
      cursor.moveToPosition(position);
      return new Photo(
          cursor.getLong(colId), Uri.parse(cursor.getString(colData)), cursor.getString(colName));
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
      public final ImageView imageView;

      public ViewHolder(final View itemView) {
        super(itemView);
        // TODO consider using fresco for perf reasons
        this.imageView = (ImageView) itemView.findViewById(R.id.content_iv);
      }

      public void bind(Photo photo) {
        Bitmap thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
            contentResolver,
            photo.id,
            MediaStore.Images.Thumbnails.MINI_KIND,
            null /* Options */);
        imageView.setImageBitmap(thumbnail);
      }
    }
  }

  public static class Photo {
    public final long id;
    public final Uri uri;
    public final String displayName;


    public Photo(final long id, final Uri uri, final String displayName) {
      this.id = id;
      this.uri = uri;
      this.displayName = displayName;
    }
  }
}
