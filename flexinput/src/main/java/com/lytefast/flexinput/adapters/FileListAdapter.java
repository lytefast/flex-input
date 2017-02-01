package com.lytefast.flexinput.adapters;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
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
import com.lytefast.flexinput.model.Attachment;
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

  private final SelectionCoordinator<Attachment<File>> selectionCoordinator;

  private ContentResolver contentResolver;
  private List<Attachment<File>> files;


  public FileListAdapter(ContentResolver contentResolver,
                         final SelectionCoordinator<Attachment<File>> selectionCoordinator) {
    this.contentResolver = contentResolver;
    this.files = Collections.EMPTY_LIST;
    this.selectionCoordinator = selectionCoordinator.bind(this);
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

    Collections.sort(files, new Comparator<Attachment<File>>() {
      @Override
      public int compare(final Attachment<File> o1, final Attachment<File> o2) {
        // Sort by newest first
        return Long.valueOf(o2.getData().lastModified()).compareTo(o1.getData().lastModified());
      }
    });
    notifyDataSetChanged();
  }

  protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private final AnimatorSet shrinkAnim;
    private final AnimatorSet growAnim;

    @BindView(R2.id.thumb_iv) SimpleDraweeView thumbIv;
    @BindView(R2.id.type_iv) ImageView typeIv;
    @BindView(R2.id.file_name_tv) TextView fileNameTv;
    @BindView(R2.id.file_path_tv) TextView filePathTV;

    private Attachment<File> attachmentFile = null;


    public ViewHolder(final View itemView) {
      super(itemView);
      this.itemView.setClickable(true);
      this.itemView.setOnClickListener(this);
      ButterKnife.bind(this, itemView);

      //region Perf: Load animations once
      this.shrinkAnim = (AnimatorSet) AnimatorInflater.loadAnimator(
          itemView.getContext(), R.animator.selection_shrink);
      this.shrinkAnim.setTarget(thumbIv);

      this.growAnim = (AnimatorSet) AnimatorInflater.loadAnimator(
          itemView.getContext(), R.animator.selection_grow);
      this.growAnim.setTarget(thumbIv);
      //endregion
    }

    public void bind(final Attachment<File> fileAttachment) {
      this.attachmentFile = fileAttachment;
      setSelected(selectionCoordinator.isSelected(fileAttachment), false);

      final File file = fileAttachment.getData();
      fileNameTv.setText(file.getName());
      filePathTV.setText(file.getPath());

      // Set defaults
      thumbIv.setImageURI((Uri) null);
      typeIv.setVisibility(View.GONE);

      String mimeType = getMimeType(file);
      if (!TextUtils.isEmpty(mimeType)) {
        if (mimeType.startsWith("image")) {
          typeIv.setImageResource(R.drawable.ic_image_24dp);
          typeIv.setVisibility(View.VISIBLE);
          bindThumbIvWithImage(file);
        } else if (mimeType.startsWith("video")) {
          typeIv.setImageResource(R.drawable.ic_movie_24dp);
          typeIv.setVisibility(View.VISIBLE);
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

    void setSelected(boolean isSelected, boolean isAnimationRequested) {
      itemView.setSelected(isSelected);

      if (isSelected) {
        if (thumbIv.getScaleX() == 1.0f) {
          shrinkAnim.start();
          if (!isAnimationRequested) {
            shrinkAnim.end();
          }
        }
      } else {
        if (thumbIv.getScaleX() != 1.0f) {
          growAnim.start();
          if (!isAnimationRequested) {
            growAnim.end();
          }
        }
      }
    }

    @Override
    public void onClick(final View v) {
      setSelected(selectionCoordinator.toggleItem(attachmentFile, getAdapterPosition()), true);
    }
  }

  private static List<Attachment<File>> flattenFileList(File parentDir) {
    List<Attachment<File>> flattenedFileList = new ArrayList<>();
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