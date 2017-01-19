package com.lytefast.flexinput.adapters;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.lytefast.flexinput.R;
import com.lytefast.flexinput.R2;
import com.lytefast.flexinput.model.Generic;
import com.lytefast.flexinput.utils.FileUtils;
import com.lytefast.flexinput.utils.SelectionCoordinator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * {@link RecyclerView.Adapter} that knows how to display files from the media store.
 *
 * @author Sam Shih
 */
public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {

  private final SelectionCoordinator<Generic<File>> selectionCoordinator;

  private ContentResolver contentResolver;
  private List<Generic<File>> files;


  public FileListAdapter(ContentResolver contentResolver,
                         final SelectionCoordinator<Generic<File>> selectionCoordinator) {
    this.contentResolver = contentResolver;
    this.files = Collections.EMPTY_LIST;
    this.selectionCoordinator = selectionCoordinator.bind(this);

    Collections.sort(files, new Comparator<Generic<File>>() {
      @Override
      public int compare(final Generic<File> o1, final Generic<File> o2) {
        // Sort by newest first
        return Long.valueOf(o2.rawData.lastModified()).compareTo(o1.rawData.lastModified());
      }
    });
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

  public void load(File root) {
    this.files = flattenFileList(root);
    notifyDataSetChanged();
  }

  protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    @BindView(R2.id.thumb_iv) SimpleDraweeView thumbIv;
    @BindView(R2.id.type_iv) ImageView typeIv;
    @BindView(R2.id.file_name_tv) TextView fileNameTv;
    @BindView(R2.id.file_path_tv) TextView filePathTV;

    private Generic<File> genericFile = null;


    public ViewHolder(final View itemView) {
      super(itemView);
      this.itemView.setOnClickListener(this);
      ButterKnife.bind(this, itemView);
    }

    public void bind(final Generic<File> genericFile) {
      this.genericFile = genericFile;
      setSelected(selectionCoordinator.isSelected(genericFile));

      final File file = genericFile.rawData;
      fileNameTv.setText(file.getName());
      filePathTV.setText(file.getPath());

      thumbIv.setImageResource(0);
      typeIv.setImageResource(R.drawable.ic_file_24dp);

      String mimeType = getMimeType(file);
      if (!TextUtils.isEmpty(mimeType)) {
        if (mimeType.startsWith("image")) {
          typeIv.setImageResource(R.drawable.ic_image_24dp);
          bindThumbIvWithImage(file);
        } else if (mimeType.startsWith("video")) {
          typeIv.setImageResource(R.drawable.ic_movie_24dp);
          thumbIv.setImageURI(FileUtils.toUri(file));
        }
      }
    }

    private void bindThumbIvWithImage(final File file) {
      Cursor c = contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        new String[]{MediaStore.Images.Media._ID},
        MediaStore.Images.Media.DATA + "=?",
        new String[]{file.getPath()},
        null /* sortOrder */);

      if (c == null || !c.moveToFirst()) {
        return;
      }
      final long imageId = c.getLong(0);
      Bitmap thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
          contentResolver, imageId, MediaStore.Images.Thumbnails.MINI_KIND, null);
      thumbIv.setImageBitmap(thumbnail);
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
      setSelected(selectionCoordinator.toggleItem(genericFile, getAdapterPosition()));
    }
  }

  private static List<Generic<File>> flattenFileList(File parentDir) {
    List<Generic<File>> flattenedFileList = new ArrayList<>();
    Queue<File> files = new LinkedList<>();
    files.addAll(Arrays.asList(parentDir.listFiles()));
    while (!files.isEmpty()) {
      File file = files.remove();
      if (file.isHidden()) {
        continue;
      }

      if (file.isDirectory()) {
        files.addAll(Arrays.asList(file.listFiles()));
      } else {
        flattenedFileList.add(FileUtils.toAttachment(file));
      }
    }
    return flattenedFileList;
  }

  @Nullable
  private static String getMimeType(File file) {
    String type = null;
    String fileName = file.getName();
    String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
    if (extension != null) {
      type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }
    return type;
  }
}