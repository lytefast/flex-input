package com.lytefast.flexinput.adapters

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.AsyncQueryHandler
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.lytefast.flexinput.R
import com.lytefast.flexinput.model.Photo
import com.lytefast.flexinput.utils.SelectionCoordinator


/**
 * [RecyclerView.Adapter] that knows how to load photos from the media store.
 *
 * @author Sam Shih
 */
class PhotoCursorAdapter(private val contentResolver: ContentResolver,
                         selectionCoordinator: SelectionCoordinator<*, Photo>)
  : RecyclerView.Adapter<PhotoCursorAdapter.ViewHolder>() {
  private val selectionCoordinator: SelectionCoordinator<*, Photo> = selectionCoordinator.bind(this)
  private var cursor: Cursor? = null

  private var colId: Int = 0
  private var colData: Int = 0
  private var colName: Int = 0


  init {
    setHasStableIds(true)
  }

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
    loadPhotos()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoCursorAdapter.ViewHolder {
    val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.view_grid_image, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: PhotoCursorAdapter.ViewHolder, position: Int) {
    val photo = this[position]
    holder.bind(photo)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
    payloads
      .firstOrNull { it is SelectionCoordinator.SelectionEvent<*> }
      ?.let { it as? SelectionCoordinator.SelectionEvent<*> }
      ?.also {
        holder.setSelected(it.isSelected, isAnimationRequested = true)
        return
      }
    super.onBindViewHolder(holder, position, payloads)
  }

  override fun getItemCount(): Int = cursor?.count ?: 0

  override fun getItemId(position: Int): Long = this[position]?.id ?: -1

  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    cursor?.close()
    super.onDetachedFromRecyclerView(recyclerView)
  }

  fun loadPhotos() {
    class LoadQueryHandler : AsyncQueryHandler(contentResolver) {
      override fun onQueryComplete(token: Int, cookie: Any, cursor: Cursor?) {
        if (cursor == null) {
          return
        }
        this@PhotoCursorAdapter.apply {
          this.colId = cursor.getColumnIndex(MediaStore.Images.Media._ID)
          this.colData = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
          this.colName = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
          this.cursor = cursor
        }
        notifyDataSetChanged()
      }
    }

    LoadQueryHandler().startQuery(1, this,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME),
        null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC")
  }

  private operator fun get(position: Int): Photo? =
    cursor?.let {
      it.moveToPosition(position)
      val photoId = it.getLong(colId)
      val fileUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photoId.toString())
      Photo(
          id = photoId,
          uri = fileUri,
          displayName = it.getString(colName) ?: "img-$photoId",
          photoDataLocation = it.getString(colData))
    }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    private val shrinkAnim: AnimatorSet
    private val growAnim: AnimatorSet

    private val imageView: SimpleDraweeView = itemView.findViewById(R.id.content_iv)
    private val checkIndicator: SimpleDraweeView = itemView.findViewById(R.id.item_check_indicator)

    private var photo: Photo? = null


    init {
      this.itemView.setOnClickListener(this)

      //region Perf: Load animations once
      this.shrinkAnim = AnimatorInflater.loadAnimator(
          itemView.context, R.animator.selection_shrink) as AnimatorSet
      this.shrinkAnim.setTarget(imageView)

      this.growAnim = AnimatorInflater.loadAnimator(
          itemView.context, R.animator.selection_grow) as AnimatorSet
      this.growAnim.setTarget(imageView)
      //endregion
    }

    fun bind(photo: Photo?) {
      this.photo = photo

      val thumbnailUri = photo?.let {
        setSelected(selectionCoordinator.isSelected(photo, adapterPosition), false)
        it.getThumbnailUri(contentResolver)
      }

      imageView.setImageURI(thumbnailUri)
    }

    fun setSelected(isSelected: Boolean, isAnimationRequested: Boolean = true) {
      itemView.isSelected = isSelected

      fun scaleImage(animation: AnimatorSet) {
        animation.start()
        if (!isAnimationRequested) {
          animation.end()
        }
      }

      if (isSelected) {
        checkIndicator.visibility = View.VISIBLE
        if (imageView.scaleX == 1.0f) scaleImage(shrinkAnim)
      } else {
        checkIndicator.visibility = View.GONE
        if (imageView.scaleX != 1.0f) scaleImage(growAnim)
      }
    }

    override fun onClick(v: View) {
      selectionCoordinator.toggleItem(photo, adapterPosition)
    }
  }
}