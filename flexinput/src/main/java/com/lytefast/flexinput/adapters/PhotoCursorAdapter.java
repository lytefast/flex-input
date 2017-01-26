package com.lytefast.flexinput.adapters;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.view.SimpleDraweeView;
import com.lytefast.flexinput.R;
import com.lytefast.flexinput.model.Photo;
import com.lytefast.flexinput.utils.SelectionCoordinator;


/**
 * {@link android.support.v7.widget.RecyclerView.Adapter} that knows how to load photos from the media store.
 *
 * @author Sam Shih
 */
public class PhotoCursorAdapter extends RecyclerView.Adapter<PhotoCursorAdapter.ViewHolder> {

  private final ContentResolver contentResolver;
  private final SelectionCoordinator<Photo> selectionCoordinator;
  private Cursor cursor;

  private int colId;
  private int colData;
  private int colName;


  public PhotoCursorAdapter(ContentResolver contentResolver,
                            final SelectionCoordinator<Photo> selectionCoordinator) {
    this.contentResolver = contentResolver;
    this.selectionCoordinator = selectionCoordinator.bind(this);

    setHasStableIds(true);
  }

  @Override
  public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
    super.onAttachedToRecyclerView(recyclerView);
    loadPhotos();
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
    return getPhoto(position).getId();
  }

  @Override
  public void onDetachedFromRecyclerView(final RecyclerView recyclerView) {
    cursor.close();
    super.onDetachedFromRecyclerView(recyclerView);
  }

   // TODO consider moving this to a background thread
  @Nullable
  public Cursor loadPhotos() {
    this.cursor = contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        new String[]{
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME},
        null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");

    this.colId = cursor.getColumnIndex(MediaStore.Images.Media._ID);
    this.colData = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
    this.colName = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);

    notifyDataSetChanged();
    return cursor;
  }

  private Photo getPhoto(int position) {
    cursor.moveToPosition(position);
    final String photoId = Integer.toString(cursor.getInt(colId));
    final Uri fileUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photoId);
    return new Photo(
        cursor.getLong(colId), fileUri, cursor.getString(colName), cursor.getString(colData));
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
      setSelected(selectionCoordinator.isSelected(photo));

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
      setSelected(selectionCoordinator.toggleItem(photo, getAdapterPosition()));
    }
  }
}