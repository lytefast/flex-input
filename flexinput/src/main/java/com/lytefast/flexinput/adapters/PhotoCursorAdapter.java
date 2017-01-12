package com.lytefast.flexinput.adapters;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.view.SimpleDraweeView;
import com.lytefast.flexinput.R;
import com.lytefast.flexinput.model.Photo;

import java.util.ArrayList;
import java.util.Set;


/**
 * {@link android.support.v7.widget.RecyclerView.Adapter} that knows how to load photos from the media store.
 *
 * @author Sam Shih
 */
public class PhotoCursorAdapter extends RecyclerView.Adapter<PhotoCursorAdapter.ViewHolder> {

  private final ContentResolver contentResolver;
  private Cursor cursor;

  private final int colId;
  private final int colData;
  private final int colName;

  private final ArrayMap<Photo, Integer> selectedItemPositionMap;

  @Nullable private OnItemClickListener<Photo> onItemClickListener;


  public PhotoCursorAdapter(ContentResolver contentResolver, @NonNull Cursor cursor) {
    this.contentResolver = contentResolver;
    this.cursor = cursor;

    this.colId = cursor.getColumnIndex(MediaStore.Images.Media._ID);
    this.colData = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
    this.colName = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);

    setHasStableIds(true);

    this.selectedItemPositionMap = new ArrayMap<>(4);
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

  private Photo getPhoto(int position) {
    cursor.moveToPosition(position);
    return new Photo(
        cursor.getLong(colId), Uri.parse(cursor.getString(colData)), cursor.getString(colName));
  }

  public Set<Photo> getSelectedItems() {
    return selectedItemPositionMap.keySet();
  }

  public void clearSelectedItems() {
    ArrayList<Integer> oldSelection = new ArrayList<>(selectedItemPositionMap.values());
    selectedItemPositionMap.clear();
    for (int position: oldSelection) {
      notifyItemChanged(position);
    }
  }

  public void setOnItemClickListener(final OnItemClickListener<Photo> onItemClickListener) {
    this.onItemClickListener = onItemClickListener;
  }

  protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public final SimpleDraweeView imageView;
    private final View checkIndicator;

    private Photo photo = null;


    public ViewHolder(final View itemView) {
      super(itemView);
      this.itemView.setOnClickListener(this);

      this.imageView = (SimpleDraweeView) itemView.findViewById(R.id.content_iv);
      this.checkIndicator = itemView.findViewById(R.id.item_check_indicator);
    }

    public void bind(final Photo photo) {
      this.photo = photo;
      setSelected(selectedItemPositionMap.containsKey(photo));

      Uri thumbnailUri = photo.getThumbnailUri(contentResolver);
      if (thumbnailUri == null) {
        imageView.setImageURI((String) null);
      } else {
        imageView.setImageURI(thumbnailUri);
      }
    }

    void setSelected(boolean isSelected) {
      imageView.setSelected(isSelected);
      if (isSelected) {
        itemView.setBackgroundResource(R.drawable.rect_rounded_highlight);
        checkIndicator.setVisibility(View.VISIBLE);
      } else {
        itemView.setBackgroundResource(0);
        checkIndicator.setVisibility(View.GONE);
      }
    }

    @Override
    public void onClick(final View v) {
      if (onItemClickListener != null) {
        onItemClickListener.onItemClicked(photo);
      }
      if (selectedItemPositionMap.remove(photo) == null) {
        selectedItemPositionMap.put(photo, getAdapterPosition());
        setSelected(true);
      } else {
        setSelected(false);
      }
    }
  }
}