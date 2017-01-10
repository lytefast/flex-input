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
import android.widget.ImageView;
import android.widget.TextView;

import com.lytefast.flexinput.R;
import com.lytefast.flexinput.R2;
import com.lytefast.flexinput.model.Attachment;

import java.util.ArrayList;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * {@link RecyclerView.Adapter} that knows how to display files from the media store.
 *
 * @author Sam Shih
 */
public class FileCursorAdapter extends RecyclerView.Adapter<FileCursorAdapter.ViewHolder> {

  private final ContentResolver contentResolver;
  private Cursor cursor;

  private final int colId;
  private final int colData;
  private final int colName;

  private final ArrayMap<Attachment, Integer> selectedItemPositionMap;

  @Nullable private OnItemClickListener<Attachment> onItemClickListener;


  public FileCursorAdapter(ContentResolver contentResolver, @NonNull Cursor cursor) {
    this.contentResolver = contentResolver;
    this.cursor = cursor;

    this.colId = cursor.getColumnIndex(MediaStore.Images.Media._ID);
    this.colData = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
    this.colName = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);

    setHasStableIds(true);

    this.selectedItemPositionMap = new ArrayMap<>(4);
  }

  @Override
  public FileCursorAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.view_file_item, parent, false);
    return new FileCursorAdapter.ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(final FileCursorAdapter.ViewHolder holder, final int position) {
    Attachment Attachment = getAttachment(position);
    holder.bind(Attachment);
  }

  @Override
  public int getItemCount() {
    return cursor.getCount();
  }

  @Override
  public long getItemId(final int position) {
    return getAttachment(position).id;
  }

  @Override
  public void onDetachedFromRecyclerView(final RecyclerView recyclerView) {
    cursor.close();
    super.onDetachedFromRecyclerView(recyclerView);
  }

  private Attachment getAttachment(int position) {
    cursor.moveToPosition(position);
    return new Attachment(
        cursor.getLong(colId), Uri.parse(cursor.getString(colData)), cursor.getString(colName));
  }

  public Set<Attachment> getSelectedItems() {
    return selectedItemPositionMap.keySet();
  }

  public void clearSelectedItems() {
    ArrayList<Integer> oldSelection = new ArrayList<>(selectedItemPositionMap.values());
    for (int position: oldSelection) {
      notifyItemChanged(position);
    }
    selectedItemPositionMap.clear();
  }

  public void setOnItemClickListener(final OnItemClickListener<Attachment> onItemClickListener) {
    this.onItemClickListener = onItemClickListener;
  }

  protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    @BindView(R2.id.thumb_iv) ImageView thumbIv;
    @BindView(R2.id.file_name_tv) TextView fileNameTv;
    @BindView(R2.id.file_path_tv) TextView filePathTV;

    private Attachment attachment = null;


    public ViewHolder(final View itemView) {
      super(itemView);
      this.itemView.setOnClickListener(this);
      ButterKnife.bind(this, itemView);
    }

    public void bind(final Attachment attachment) {
      this.attachment = attachment;
      setSelected(selectedItemPositionMap.containsKey(attachment));

      fileNameTv.setText(attachment.displayName);
      filePathTV.setText(attachment.uri.getPath());
    }

    void setSelected(boolean isSelected) {
      thumbIv.setSelected(isSelected);
      if (isSelected) {
        itemView.setBackgroundResource(R.drawable.rect_rounded_highlight);
      } else {
        itemView.setBackgroundResource(0);
      }
    }

    @Override
    public void onClick(final View v) {
      if (onItemClickListener != null) {
        onItemClickListener.onItemClicked(attachment);
      }
      if (selectedItemPositionMap.remove(attachment) == null) {
        selectedItemPositionMap.put(attachment, getAdapterPosition());
        setSelected(true);
      } else {
        setSelected(false);
      }
    }
  }
}