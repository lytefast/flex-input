package com.lytefast.flexinput.adapters

import android.content.ContentResolver
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.lytefast.flexinput.R
import com.lytefast.flexinput.model.Attachment
import com.lytefast.flexinput.model.Photo
import com.lytefast.flexinput.utils.SelectionAggregator


typealias SelectionAggregatorProvider<T> = (AttachmentPreviewAdapter<T>) -> SelectionAggregator<T>

/**
 * [RecyclerView.Adapter] which, given a list of attachments understands how to display them.
 * This can be extended to implement custom previews.
 *
 * @author Sam Shih
 */
class AttachmentPreviewAdapter<T : Attachment<Any>>
@JvmOverloads constructor(private val contentResolver: ContentResolver,
                          selectionAggregatorProvider: SelectionAggregatorProvider<T>? = null)
  : RecyclerView.Adapter<AttachmentPreviewAdapter<T>.ViewHolder>() {

  val selectionAggregator: SelectionAggregator<T> =
      selectionAggregatorProvider?.invoke(this) ?: SelectionAggregator(this)


  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.view_attachment_preview_item, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val item = selectionAggregator.get(position)
    holder.bind(item)
  }

  override fun getItemCount(): Int = selectionAggregator.size

  fun clear() {
    val oldItemCount = itemCount
    selectionAggregator.clear()
    notifyItemRangeRemoved(0, oldItemCount)
  }


  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val draweeView: SimpleDraweeView = itemView as SimpleDraweeView

    fun bind(item: T) {
      when (item) {
        is Photo -> draweeView.setImageURI(item.getThumbnailUri(contentResolver))
        else -> {
          // Make sure large images don't crash drawee
          // http://stackoverflow.com/questions/33676807/fresco-bitmap-too-large-to-be-uploaded-into-a-texture
          val height = draweeView.layoutParams.height
          val imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(item.uri)
              .setRotationOptions(RotationOptions.autoRotate())
              .setResizeOptions(ResizeOptions(height, height))

          val controller = Fresco.newDraweeControllerBuilder()
              .setOldController(draweeView.controller)
              .setAutoPlayAnimations(true)
              .setImageRequest(imageRequestBuilder.build())
              .build()

          draweeView.controller = controller
        }
      }

      itemView.setOnClickListener {
        // Let the child delete the item, and notify us
        selectionAggregator.unselectItem(item)
      }
    }
  }
}
