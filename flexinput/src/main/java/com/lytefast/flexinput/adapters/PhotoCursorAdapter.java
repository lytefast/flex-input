package com.lytefast.flexinput.adapters;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.facebook.drawee.backends.pipeline.PipelineDraweeController;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.lytefast.flexinput.R;

import java.util.Set;


/**
 * {@link android.support.v7.widget.RecyclerView.Adapter} that knows how to load photos from the media store.
 */
public class PhotoCursorAdapter extends RecyclerView.Adapter<PhotoCursorAdapter.ViewHolder> {

  private final ContentResolver contentResolver;
  private Cursor cursor;

  private final int colId;
  private final int colData;
  private final int colName;

  private Set<Integer> selectedItems;

  @Nullable
  private OnItemClickListener<Photo> onItemClickListener;


  public PhotoCursorAdapter(ContentResolver contentResolver, @NonNull Cursor cursor) {
    this.contentResolver = contentResolver;
    this.cursor = cursor;

    this.colId = cursor.getColumnIndex(MediaStore.Images.Media._ID);
    this.colData = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
    this.colName = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);

    setHasStableIds(true);

    this.selectedItems = new ArraySet(4);
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
    public final SimpleDraweeView imageView;

    public final ClickListener clickListener;
    private final View checkIndicator;


    public ViewHolder(final View itemView) {
      super(itemView);
      this.clickListener = new ClickListener();
      this.itemView.setOnClickListener(clickListener);

      this.imageView = (SimpleDraweeView) itemView.findViewById(R.id.content_iv);
      this.checkIndicator = itemView.findViewById(R.id.item_check_indicator);
    }

    public void bind(final Photo photo, final int position) {
      clickListener.bind(photo, position);

      final Cursor cursor = contentResolver.query(
          MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
          new String[]{MediaStore.Images.Thumbnails._ID},
          MediaStore.Images.Thumbnails.IMAGE_ID + "= ? AND KIND = ?",
          new String[]{String.valueOf(photo.id), Integer.toString(MediaStore.Images.Thumbnails.MINI_KIND)},
          null);

      if (cursor == null || !cursor.moveToFirst()) {
        imageView.setImageURI((String) null);
        return;
      }
      try {
        final long thumbId = cursor.getLong(0);
        imageView.setImageURI(
            ContentUris.withAppendedId(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, thumbId));
      } finally {
        cursor.close();
      }
    }

    void setSelected(boolean isSelected) {
      if (isSelected) {
        itemView.setBackgroundResource(R.drawable.rect_rounded_highlight);
        checkIndicator.setVisibility(View.VISIBLE);
      }
    }
//  }


  protected class ClickListener implements View.OnClickListener {
    private Photo photo = null;
    private int position = -1;


    @Override
    public void onClick(final View v) {
      if (onItemClickListener != null) {
        onItemClickListener.onItemClicked(photo, position);
      }
      setSelected(selectedItems.contains(position));
    }

    public void bind(final Photo photo, final int position) {
      this.photo = photo;
      this.position = position;
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