package com.lytefast.flexinput.adapters

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.AsyncQueryHandler
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.drawable.FadeDrawable
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import com.lytefast.flexinput.R
import com.lytefast.flexinput.model.Photo
import com.lytefast.flexinput.utils.SelectionCoordinator
import kotlinx.coroutines.*
import androidx.core.content.ContextCompat


/**
 * [RecyclerView.Adapter] that knows how to load photos from the media store.
 *
 * @author Sam Shih
 */
class PhotoCursorAdapter(private val contentResolver: ContentResolver,
                         selectionCoordinator: SelectionCoordinator<*, Photo>,
                         val thumbnailWidth: Int,
                         val thumbnailHeight: Int)
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

    emptyColorDrawable = ColorDrawable(recyclerView.context.themeColor(R.attr.flexInputDialogBackground))

    shrinkAnim = AnimatorInflater.loadAnimator(recyclerView.context, R.animator.selection_shrink) as AnimatorSet
    growAnim = AnimatorInflater.loadAnimator(recyclerView.context, R.animator.selection_grow) as AnimatorSet

    loadPhotos()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.view_grid_image, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
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

  override fun onViewRecycled(holder: ViewHolder) {
    super.onViewRecycled(holder)
    holder.onViewRecycled()
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
    private val imageView: SimpleDraweeView = itemView.findViewById(R.id.content_iv)
    private val checkIndicator: SimpleDraweeView = itemView.findViewById(R.id.item_check_indicator)

    private var photo: Photo? = null
    private var loadThumbnailJob: Job? = null
    private var thumbnailBitmap: Bitmap? = null
    private var thumbnailDrawable: BitmapDrawable? = null
    private var holderFadeDrawable: FadeDrawable? = null

    init {
      this.itemView.setOnClickListener(this)
    }

    fun bind(photo: Photo?) {
      shrinkAnim?.setTarget(imageView)
      growAnim?.setTarget(imageView)

      this.photo = photo

      if (isAndroidQ()) {
        clear()

        imageView.hierarchy.setPlaceholderImage(emptyColorDrawable, ScalingUtils.ScaleType.CENTER)

        // Ensure this executes on the main thread for UI interaction (as opposed to IO)
        loadThumbnailJob = GlobalScope.launch(context = Dispatchers.Main) {
          thumbnailBitmap = getThumbnailAsync()
          thumbnailDrawable = BitmapDrawable(imageView.resources, thumbnailBitmap)
          val fadeDrawable = FadeDrawable(arrayOf(emptyColorDrawable, thumbnailDrawable))

          fadeDrawable.transitionDuration = 300
          val roundingParams = RoundingParams.fromCornersRadius(imageView.context.dpToPixels(4f))
          roundingParams.overlayColor = imageView.context.themeColor(R.attr.flexInputDialogBackground)
          imageView.hierarchy.roundingParams = roundingParams
          imageView.hierarchy.setPlaceholderImage(fadeDrawable, ScalingUtils.ScaleType.CENTER_CROP)
          fadeDrawable.fadeToLayer(1)
          holderFadeDrawable = fadeDrawable
        }
      } else {
        val thumbnailUri = photo?.let {
          setSelected(selectionCoordinator.isSelected(photo, adapterPosition), false)
          it.getThumbnailUri(contentResolver)
        }

        imageView.setImageURI(thumbnailUri, imageView.context)
      }
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
        if (imageView.scaleX == 1.0f) shrinkAnim?.let { scaleImage(it) }
      } else {
        checkIndicator.visibility = View.GONE
        if (imageView.scaleX != 1.0f) growAnim?.let { scaleImage(it) }
      }
    }

    override fun onClick(v: View) {
      selectionCoordinator.toggleItem(photo, adapterPosition)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun getThumbnailAsync() = withContext(Dispatchers.IO) {
      photo?.getThumbnailQ(contentResolver, thumbnailWidth, thumbnailHeight)
    }

    fun onViewRecycled() {
      clear()
    }

    private fun clear() {
      if (isAndroidQ()) {
        loadThumbnailJob?.cancel()

        thumbnailDrawable?.bitmap?.recycle()
        thumbnailDrawable = null
        thumbnailBitmap?.recycle()
        thumbnailBitmap = null
        val fadeBitmapDrawable = holderFadeDrawable?.getDrawable(1) as? BitmapDrawable
        fadeBitmapDrawable?.bitmap?.recycle()
        holderFadeDrawable = null
      }
    }
  }

  companion object {
    private var emptyColorDrawable: Drawable? = null

    private var shrinkAnim: AnimatorSet? = null
    private var growAnim: AnimatorSet? = null

    fun Context.dpToPixels(dipValue: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, resources.displayMetrics)

    fun isAndroidQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun Context.themeColor(themeAttributeId: Int): Int {
      val outValue = TypedValue()
      val wasResolved = theme.resolveAttribute(themeAttributeId, outValue, true)
      return if (wasResolved) {
        ContextCompat.getColor(this, outValue.resourceId)
      } else {
        0
      }
    }
  }
}