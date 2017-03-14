package com.lytefast.flexinput.adapters;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.AsyncQueryHandler;
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
    if (cursor == null) {
      return 0;
    }
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

  @Nullable
  public void loadPhotos() {
    final AsyncQueryHandler asyncQueryHandler = new AsyncQueryHandler(contentResolver) {
      @Override
      protected void onQueryComplete(final int token, final Object cookie, final Cursor cursor) {
        PhotoCursorAdapter.this.colId = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        PhotoCursorAdapter.this.colData = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
        PhotoCursorAdapter.this.colName = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);

        PhotoCursorAdapter.this.cursor = cursor;
        notifyDataSetChanged();
      }
    };
    asyncQueryHandler.startQuery(1, this,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        new String[]{
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME},
        null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");
  }

  private Photo getPhoto(int position) {
    cursor.moveToPosition(position);
    final String photoId = Integer.toString(cursor.getInt(colId));
    final Uri fileUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photoId);
    return new Photo(
        cursor.getLong(colId), fileUri, cursor.getString(colName), cursor.getString(colData));
  }

  protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private final AnimatorSet shrinkAnim;
    private final AnimatorSet growAnim;

    public final SimpleDraweeView imageView;
    private final View checkIndicator;

    private Photo photo = null;


    public ViewHolder(final View itemView) {
      super(itemView);
      this.itemView.setOnClickListener(this);

      this.imageView = (SimpleDraweeView) itemView.findViewById(R.id.content_iv);
      this.checkIndicator = itemView.findViewById(R.id.item_check_indicator);

      //region Perf: Load animations once
      this.shrinkAnim = (AnimatorSet) AnimatorInflater.loadAnimator(
          itemView.getContext(), R.animator.selection_shrink);
      this.shrinkAnim.setTarget(imageView);

      this.growAnim = (AnimatorSet) AnimatorInflater.loadAnimator(
          itemView.getContext(), R.animator.selection_grow);
      this.growAnim.setTarget(imageView);
      //endregion
    }

    public void bind(final Photo photo) {
      this.photo = photo;
      setSelected(selectionCoordinator.isSelected(photo, getAdapterPosition()), false);

      Uri thumbnailUri = photo.getThumbnailUri(contentResolver);
      if (thumbnailUri == null) {
        imageView.setImageURI((String) null);
      } else {
        imageView.setImageURI(thumbnailUri);
      }
    }

    void setSelected(boolean isSelected, boolean isAnimationRequested) {
      itemView.setSelected(isSelected);

      if (isSelected) {
        checkIndicator.setVisibility(View.VISIBLE);
        if (imageView.getScaleX() == 1.0f) {
          shrinkAnim.start();
          if (!isAnimationRequested) {
            shrinkAnim.end();
          }
        }
      } else {
        checkIndicator.setVisibility(View.GONE);
        if (imageView.getScaleX() != 1.0f) {
          growAnim.start();
          if (!isAnimationRequested) {
            growAnim.end();
          }
        }
      }
    }

    @Override
    public void onClick(final View v) {
      setSelected(selectionCoordinator.toggleItem(photo, getAdapterPosition()), true);
    }
  }
}