package com.lytefast.flexinput.sampleapp

import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.SimpleDraweeView
import com.lytefast.flexinput.model.Attachment
import java.util.*

/**
 * Simple string message display adapter.
 *
 * @author Sam Shih
 */
class MessageAdapter : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
  private val msgList: MutableList<Data> = ArrayList()
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.message_row, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(msgList[position], position)
  }

  override fun getItemCount(): Int {
    return msgList.size
  }

  fun addMessage(msg: Data) {
    msgList.add(msg)
    notifyItemInserted(msgList.size - 1)
  }

  class Data(val editable: Editable, val attachment: Attachment<*>?)

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val indexTv: TextView = itemView.findViewById(R.id.index_tv)
    private val messageTv: TextView = itemView.findViewById(R.id.message_tv)
    private val attachmentTv: TextView = itemView.findViewById(R.id.attachment_tv)
    private val imageView: SimpleDraweeView = itemView.findViewById(R.id.attachment_iv)

    fun bind(data: Data, index: Int) {
      indexTv.text = index.toString()
      messageTv.text = data.editable
      if (data.attachment != null) {
        imageView.visibility = View.VISIBLE
        val uri = data.attachment.uri
        val controller: DraweeController = Fresco.newDraweeControllerBuilder()
            .setUri(uri)
            .setAutoPlayAnimations(true)
            .build()
        imageView.controller = controller
        attachmentTv.visibility = View.VISIBLE
        attachmentTv.text = data.attachment.displayName
      } else {
        imageView.visibility = View.GONE
        attachmentTv.visibility = View.GONE
      }
    }
  }
}