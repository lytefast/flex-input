package com.lytefast.flexinput.adapters;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.lytefast.flexinput.R;


/**
 * {@link android.support.v7.widget.RecyclerView.Adapter} that knows how to load photos from the media store.
 */
public class PhotoCursorAdapter extends RecyclerView.Adapter<PhotoCursorAdapter.ViewHolder> {

  private final ContentResolver contentResolver;

  private Cursor cursor;
  private final int colId;
  private final int colData;
  private final int colName;

  @Nullable private OnItemClickListener<Photo> onItemClickListener;


  public PhotoCursorAdapter(ContentResolver contentResolver, @NonNull Cursor cursor) {
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
    holder.bind(photo, position);
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

  private Photo getPhoto(int position) {
    cursor.moveToPosition(position);
    return new Photo(
        cursor.getLong(colId), Uri.parse(cursor.getString(colData)), cursor.getString(colName));
  }

  public void setOnItemClickListener(final OnItemClickListener<Photo> onItemClickListener) {
    this.onItemClickListener = onItemClickListener;
  }

  protected class ViewHolder extends RecyclerView.ViewHolder {
    public final ImageView imageView;

    public final PhotoCursorAdapter.ClickListener clickListener;


    public ViewHolder(final View itemView) {
      super(itemView);
      this.clickListener = new PhotoCursorAdapter.ClickListener();

      // TODO consider using fresco for perf reasons
      this.imageView = (ImageView) itemView.findViewById(R.id.content_iv);
      this.imageView.setOnClickListener(clickListener);
    }

    public void bind(final Photo photo, final int position) {
      clickListener.bind(photo, position);

      Bitmap thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
          contentResolver,
          photo.id,
          MediaStore.Images.Thumbnails.MINI_KIND,
          null /* Options */);
      imageView.setImageBitmap(thumbnail);
    }
  }

  protected class ClickListener implements View.OnClickListener {
    private Photo photo = null;
    private int position = -1;


    @Override
    public void onClick(final View v) {
      if (onItemClickListener != null) {
        onItemClickListener.onItemClicked(photo, position);
      }
    }

    public void bind(final Photo photo, final int position) {
      this.photo = photo;
      this.position = position;
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