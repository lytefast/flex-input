package com.lytefast.flexinput.adapters;

import android.content.ContentResolver;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.lytefast.flexinput.R;
import com.lytefast.flexinput.model.Attachment;
import com.lytefast.flexinput.model.Photo;
import com.lytefast.flexinput.utils.SelectionAggregator;
import com.lytefast.flexinput.utils.SelectionCoordinator;


/**
 * {@link RecyclerView.Adapter} which, given a list of attachments understands how to display them.
 * This can be extended to implement custom previews.
 *
 * @author Sam Shih
 */
public class AttachmentPreviewAdapter<T extends Attachment<?>>
    extends RecyclerView.Adapter<AttachmentPreviewAdapter.ViewHolder> {

  public static final String TAG = AttachmentPreviewAdapter.class.getCanonicalName();

  private final ContentResolver contentResolver;

  protected final SelectionAggregator<T> selectionAggregator;


  public AttachmentPreviewAdapter(final ContentResolver contentResolver) {
    this.contentResolver = contentResolver;
    this.selectionAggregator = new SelectionAggregator<>(this);
  }

  @Override
  public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.view_attachment_preview_item, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(final AttachmentPreviewAdapter.ViewHolder holder, final int position) {
    T item = selectionAggregator.get(position);
    holder.bind(item);
  }

  @Override
  public int getItemCount() {
    return selectionAggregator.getSize();
  }

  public SelectionAggregator<T> getSelectionAggregator() {
    return selectionAggregator;
  }

  public void clear() {
    final int oldItemCount = getItemCount();
    selectionAggregator.clear();
    notifyItemRangeRemoved(0, oldItemCount);
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
          // Make sure large images don't crash drawee
          // http://stackoverflow.com/questions/33676807/fresco-bitmap-too-large-to-be-uploaded-into-a-texture
          final int height = draweeView.getLayoutParams().height;
          ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(uri)
            .setRotationOptions(RotationOptions.autoRotate())
            .setResizeOptions(new ResizeOptions(height, height));

          DraweeController controller = Fresco.newDraweeControllerBuilder()
              .setOldController(draweeView.getController())
              .setImageRequest(imageRequestBuilder.build())
              .build();

          draweeView.setController(controller);
        } else {
          draweeView.setImageResource(R.drawable.ic_attach_file_24dp);
        }
      }

      itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
          // Let the child delete the item, and notify us
          selectionAggregator.unselectItem(item);
        }
      });
    }
  }
}
