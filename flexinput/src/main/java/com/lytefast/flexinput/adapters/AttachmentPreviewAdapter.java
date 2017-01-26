package com.lytefast.flexinput.adapters;

import android.content.ContentResolver;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.view.SimpleDraweeView;
import com.lytefast.flexinput.R;
import com.lytefast.flexinput.model.Attachment;
import com.lytefast.flexinput.model.Photo;

import java.util.ArrayList;
import java.util.List;


/**
 * {@link RecyclerView.Adapter} which, given a list of attachments understands how to display them.
 * This can be extended to implement custom previews.
 *
 * @author Sam Shih
 */
public class AttachmentPreviewAdapter extends RecyclerView.Adapter<AttachmentPreviewAdapter.ViewHolder> {

  private final ContentResolver contentResolver;

  protected final List<Attachment> attachments;


  public AttachmentPreviewAdapter(final ContentResolver contentResolver) {
    this.contentResolver = contentResolver;
    this.attachments = new ArrayList<>();
  }

  @Override
  public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.view_attachment_preview_item, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(final ViewHolder holder, final int position) {
    Attachment item = attachments.get(position);
    holder.bind(item);
  }

  @Override
  public int getItemCount() {
    return attachments.size();
  }

  public List<Attachment> getAttachments() {
    return attachments;
  }

  public void clear() {
    attachments.clear();
    notifyDataSetChanged();
  }

  public boolean toggleItem(Attachment item) {
    final int oldIndex = attachments.indexOf(item);
    final boolean wasRemoved = attachments.remove(item);

    if (wasRemoved) {
      notifyItemRemoved(oldIndex);
    } else {
      attachments.add(item);
      notifyItemInserted(attachments.size() - 1);
    }
    return wasRemoved;
  }

  class ViewHolder extends RecyclerView.ViewHolder {

    private final SimpleDraweeView draweeView;

    public ViewHolder(final View itemView) {
      super(itemView);
      this.draweeView = (SimpleDraweeView) itemView;
    }

    public void bind(Attachment item) {
      if (item instanceof Photo) {
        draweeView.setImageURI(((Photo) item).getThumbnailUri(contentResolver));
      } else {
        final Uri uri = item.getUri();
        if (uri != null) {
          draweeView.setImageURI(uri);
        } else {
          draweeView.setImageResource(R.drawable.ic_attach_file_24dp);
        }
      }
    }
  }
}
