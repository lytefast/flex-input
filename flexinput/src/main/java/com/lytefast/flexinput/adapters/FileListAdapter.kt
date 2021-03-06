package com.lytefast.flexinput.adapters

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.ContentResolver
import android.net.Uri
import android.os.AsyncTask
import android.provider.MediaStore
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.lytefast.flexinput.R
import com.lytefast.flexinput.model.Attachment
import com.lytefast.flexinput.utils.FileUtils.getFileSize
import com.lytefast.flexinput.utils.FileUtils.toAttachment
import com.lytefast.flexinput.utils.SelectionCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*


/**
 * [RecyclerView.Adapter] that knows how to display files from the media store.
 *
 * @author Sam Shih
 */
class FileListAdapter(private val contentResolver: ContentResolver,
                      selectionCoordinator: SelectionCoordinator<*, Attachment<File>>)
  : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

  private val selectionCoordinator: SelectionCoordinator<*, in Attachment<File>> =
      selectionCoordinator.bind(this)
  private var files: List<Attachment<File>> = listOf()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.view_file_item, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) =
      holder.bind(files[position])

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

  override fun getItemCount(): Int = files.size

  suspend fun load(root: File) = withContext(Dispatchers.IO) {
    val files = flattenFileList(root)

    files.sortWith(
        compareByDescending<Attachment<File>> { it.lastModified }
            .then(compareBy { it.uri })
    )

    withContext(Dispatchers.Main) {
      this@FileListAdapter.files = files
      this@FileListAdapter.notifyDataSetChanged()
    }
  }

  @Suppress("MemberVisibilityCanBePrivate")
  open inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val shrinkAnim: AnimatorSet
    private val growAnim: AnimatorSet

    protected var thumbIv: SimpleDraweeView = itemView.findViewById(R.id.thumb_iv)
    protected var typeIv: ImageView = itemView.findViewById(R.id.type_iv)
    protected var fileNameTv: TextView = itemView.findViewById(R.id.file_name_tv)
    protected var fileSubtitleTv: TextView = itemView.findViewById(R.id.file_subtitle_tv)

    private var attachmentFile: Attachment<File>? = null


    init {
      this.itemView.isClickable = true
      this.itemView.setOnClickListener {
        setSelected(selectionCoordinator.toggleItem(attachmentFile, adapterPosition), true)
      }

      //region Perf: Load animations once
      this.shrinkAnim = AnimatorInflater.loadAnimator(
          itemView.context, R.animator.selection_shrink) as AnimatorSet
      this.shrinkAnim.setTarget(thumbIv)

      this.growAnim = AnimatorInflater.loadAnimator(
          itemView.context, R.animator.selection_grow) as AnimatorSet
      this.growAnim.setTarget(thumbIv)
      //endregion
    }

    fun bind(fileAttachment: Attachment<File>) {
      this.attachmentFile = fileAttachment
      setSelected(selectionCoordinator.isSelected(fileAttachment, adapterPosition), false)

      val file = fileAttachment.data
      if (file != null) {
        fileNameTv.text = file.name
        fileSubtitleTv.text = file.getFileSize()
      } else {
        fileNameTv.text = null
        fileSubtitleTv.text = null
      }

      // Set defaults
      thumbIv.setImageURI(null as Uri?, thumbIv.context)
      typeIv.visibility = View.GONE

      val mimeType = file?.getMimeType()
      if (mimeType.isNullOrEmpty()) return

      thumbIv.setImageURI(Uri.fromFile(file), thumbIv.context)

      when {
        mimeType.startsWith("image") -> {
          typeIv.setImageResource(R.drawable.ic_image_24dp)
          typeIv.visibility = View.VISIBLE
          bindThumbIvWithImage(file)
        }
        mimeType.startsWith("video") -> {
          typeIv.setImageResource(R.drawable.ic_movie_24dp)
          typeIv.visibility = View.VISIBLE
          bindThumbIvWithImage(file)
        }
        mimeType.startsWith("audio") -> {
          typeIv.setImageResource(R.drawable.ic_audio_24dp)
          typeIv.visibility = View.VISIBLE
          bindThumbIvWithImage(file)
        }
      }
    }

    private fun bindThumbIvWithImage(file: File) {
      contentResolver.query(
          MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.MINI_THUMB_MAGIC),
          "${MediaStore.Images.Media.DATA}=?",
          arrayOf(file.path), null/* sortOrder */)
          ?.use { cursor ->
            if (!cursor.moveToFirst()) return
            val imageId = cursor.getLong(0)
            val thumbMagic = cursor.getLong(1)

            if (thumbMagic == 0L) {
              // Force thumbnail generation
              val genThumb = MediaStore.Images.Thumbnails.getThumbnail(
                  contentResolver, imageId, MediaStore.Images.Thumbnails.MINI_KIND, null)
              genThumb?.recycle()
            }

            contentResolver.query(
                MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Thumbnails._ID),
                "${MediaStore.Images.Thumbnails.IMAGE_ID}=?",
                arrayOf(java.lang.Long.toString(imageId)),
                null)
                ?.use { thumbnailCursor ->
                  if (!thumbnailCursor.moveToFirst()) return
                  val thumbId = thumbnailCursor.getString(0)

                  thumbIv.controller = Fresco.newDraweeControllerBuilder()
                      .setOldController(thumbIv.controller)
                      .setUri(Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, thumbId))
                      .setTapToRetryEnabled(true)
                      .build()
                }
          }
    }

    fun setSelected(isSelected: Boolean, isAnimationRequested: Boolean) {
      itemView.isSelected = isSelected

      fun scaleImage(animation: AnimatorSet) {
        animation.start()
        if (!isAnimationRequested) {
          animation.end()
        }
      }

      if (isSelected) {
        if (thumbIv.scaleX == 1.0f) scaleImage(shrinkAnim)
      } else {
        if (thumbIv.scaleX != 1.0f) scaleImage(growAnim)
      }
    }
  }

  private fun File.getMimeType(): String? {
    var type: String? = null
    val fileName = this.name
    val extension = fileName.substring(fileName.lastIndexOf('.') + 1)
    if (!TextUtils.isEmpty(extension)) {
      type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
    return type
  }

  private fun flattenFileList(parentDir: File): MutableList<Attachment<File>> {
    fun File.getFileList() = listFiles()?.asSequence() ?: emptySequence()

    val flattenedFileList = ArrayList<Attachment<File>>()
    val files = LinkedList<File>()

    files.addAll(parentDir.getFileList())
    while (!files.isEmpty()) {
      val file = files.remove()
      if (file.isHidden) {
        continue
      }

      if (file.isDirectory) {
        files.addAll(file.getFileList())
      } else {
        flattenedFileList.add(file.toAttachment())
      }
    }
    return flattenedFileList
  }

  private val Attachment<File>.lastModified
    get() = data?.lastModified() ?: 0





  private class FileLoaderTask(val adapter: FileListAdapter) : AsyncTask<File, Boolean, List<Attachment<File>>>() {

    override fun doInBackground(vararg rootFiles: File): List<Attachment<File>> {
      val files = flattenFileList(rootFiles[0])

      files.sortWith(
          compareByDescending<Attachment<File>> { it.lastModified }
              .then(compareBy { it.uri })
      )
      return files
    }

    override fun onPostExecute(files: List<Attachment<File>>) {
      this.adapter.files = files
      this.adapter.notifyDataSetChanged()
    }

    private fun flattenFileList(parentDir: File): MutableList<Attachment<File>> {
      fun File.getFileList() = listFiles()?.asSequence() ?: emptySequence()

      val flattenedFileList = ArrayList<Attachment<File>>()
      val files = LinkedList<File>()

      files.addAll(parentDir.getFileList())
      while (!files.isEmpty()) {
        val file = files.remove()
        if (file.isHidden) {
          continue
        }

        if (file.isDirectory) {
          files.addAll(file.getFileList())
        } else {
          flattenedFileList.add(file.toAttachment())
        }
      }
      return flattenedFileList
    }

    private val Attachment<File>.lastModified
      get() = data?.lastModified() ?: 0
  }
}