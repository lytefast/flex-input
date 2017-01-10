package com.lytefast.flexinput.adapters;

import android.content.ContentResolver;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * {@link RecyclerView.Adapter} that knows how to display files from the media store.
 *
 * @author Sam Shih
 */
public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {

  private final ArrayMap<File, Integer> selectedItemPositionMap;

  @Nullable private OnItemClickListener<File> onItemClickListener;
  private final List<File> files;


  public FileListAdapter(@NonNull File root) {
    files = flattenFileList(root);
    Collections.sort(files, new Comparator<File>() {
      @Override
      public int compare(final File o1, final File o2) {
        // Sort by newest first
        return Long.valueOf(o2.lastModified()).compareTo(o1.lastModified());
      }
    });

    this.selectedItemPositionMap = new ArrayMap<>(4);
  }

  @Override
  public FileListAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.view_file_item, parent, false);
    return new FileListAdapter.ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(final FileListAdapter.ViewHolder holder, final int position) {
    holder.bind(files.get(position));
  }

  @Override
  public int getItemCount() {
    return files.size();
  }


  public Set<File> getSelectedItems() {
    return selectedItemPositionMap.keySet();
  }

  public void clearSelectedItems() {
    ArrayList<Integer> oldSelection = new ArrayList<>(selectedItemPositionMap.values());
    for (int position: oldSelection) {
      notifyItemChanged(position);
    }
    selectedItemPositionMap.clear();
  }

  public void setOnItemClickListener(final OnItemClickListener<File> onItemClickListener) {
    this.onItemClickListener = onItemClickListener;
  }

  protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    @BindView(R2.id.thumb_iv) ImageView thumbIv;
    @BindView(R2.id.file_name_tv) TextView fileNameTv;
    @BindView(R2.id.file_path_tv) TextView filePathTV;

    private File file = null;


    public ViewHolder(final View itemView) {
      super(itemView);
      this.itemView.setOnClickListener(this);
      ButterKnife.bind(this, itemView);
    }

    public void bind(final File file) {
      this.file = file;
      setSelected(selectedItemPositionMap.containsKey(file));

      fileNameTv.setText(file.getName());
      filePathTV.setText(file.getPath());
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
        onItemClickListener.onItemClicked(file);
      }
      if (selectedItemPositionMap.remove(file) == null) {
        selectedItemPositionMap.put(file, getAdapterPosition());
        setSelected(true);
      } else {
        setSelected(false);
      }
    }
  }

  private static List<File> flattenFileList(File parentDir) {
    List<File> flattenedFileList = new ArrayList<>();
    Queue<File> files = new LinkedList<>();
    files.addAll(Arrays.asList(parentDir.listFiles()));
    while (!files.isEmpty()) {
      File file = files.remove();
      if (file.isDirectory()) {
        files.addAll(Arrays.asList(file.listFiles()));
      } else {
        flattenedFileList.add(file);
      }
    }
    return flattenedFileList;
  }
}