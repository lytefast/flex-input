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
import com.lytefast.flexinput.utils.SelectionCoordinator;

import java.util.ArrayList;
import java.util.List;


/**
 * {@link RecyclerView.Adapter} which, given a list of attachments understands how to display them.
 * This can be extended to implement custom previews.
 *
 * @author Sam Shih
 */
public class AttachmentPreviewAdapter<T extends Attachment<?>>
    extends RecyclerView.Adapter<AttachmentPreviewAdapter.ViewHolder> {

  private final ContentResolver contentResolver;
  private SelectionCoordinator.ItemSelectionListener itemSelectionListener;

  @SuppressWarnings("WeakerAccess")
  protected final List<T> attachments;
  @SuppressWarnings("WeakerAccess")
  protected final List<SelectionCoordinator<T>> childSelectionCoordinators;


  public AttachmentPreviewAdapter(final ContentResolver contentResolver) {
    this.contentResolver = contentResolver;
    this.attachments = new ArrayList<>();
    this.childSelectionCoordinators = new ArrayList<>(4);
  }

  @Override
  public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.view_attachment_preview_item, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(final AttachmentPreviewAdapter.ViewHolder holder, final int position) {
    T item = attachments.get(position);
    holder.bind(item);
  }

  @Override
  public int getItemCount() {
    return attachments.size();
  }

  public void setItemSelectionListener(
      SelectionCoordinator.ItemSelectionListener itemSelectionListener) {
    this.itemSelectionListener = itemSelectionListener;
  }

  public List<T> getAttachments() {
    return attachments;
  }

  public void clear() {
    final int oldItemCount = getItemCount();
    attachments.clear();
    notifyItemRangeRemoved(0, oldItemCount);

    for (SelectionCoordinator<?> coordinator : childSelectionCoordinators) {
      coordinator.clearSelectedItems();
    }
  }

  public boolean toggleItem(final T item) {
    final int oldIndex = attachments.indexOf(item);

    final boolean wasRemoved = attachments.remove(item);
    if (wasRemoved) {
      notifyItemRemoved(oldIndex);
      for (SelectionCoordinator<T> coordinator : childSelectionCoordinators) {
        if (coordinator.isSelected(item)) {
          coordinator.toggleItem(item, 0 /* not looked at for removals */);
        }
      }
      itemSelectionListener.onItemUnselected(item);
    } else {
      attachments.add(item);
      final int position = attachments.size() - 1;
      notifyItemInserted(position);
      itemSelectionListener.onItemSelected(item);
    }

    return wasRemoved;
  }

  public void addChildSelectionCoordinator(SelectionCoordinator<T>... childSelectionCoordinators) {
    for (SelectionCoordinator<T> child : childSelectionCoordinators) {
      this.childSelectionCoordinators.add(child);
      child.setItemSelectionListener(new SelectionCoordinator.ItemSelectionListener<T>() {
        @Override
        public void onItemSelected(T item) {
          toggleItem(item);
        }

        @Override
        public void onItemUnselected(T item) {
          toggleItem(item);
        }
      });
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {

    private final SimpleDraweeView draweeView;

    public ViewHolder(final View itemView) {
      super(itemView);
      this.draweeView = (SimpleDraweeView) itemView;
    }

    public void bind(final T item) {
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

//      itemView.setOnClickListener(new View.OnClickListener() {
//        @Override
//        public void onClick(final View v) {
//          toggleItem(item);
//        }
//      });
    }
  }
}
