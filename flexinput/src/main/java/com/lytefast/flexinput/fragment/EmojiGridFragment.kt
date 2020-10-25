package com.lytefast.flexinput.fragment

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lytefast.flexinput.R
import com.lytefast.flexinput.model.Emoji
import com.lytefast.flexinput.model.EmojiCategory

/**
 * Displays an assortment of Emojis for input.
 *
 * @author Sam Shih
 */
class EmojiGridFragment : Fragment() {

  private lateinit var emojiCategory: EmojiCategory
  private var flexInputFrag: FlexInputFragment? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val parentFrag = parentFragment
    if (parentFrag is FlexInputFragment) {
      flexInputFrag = parentFrag
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    val emojiGrid = RecyclerView(inflater.context, null, R.style.FlexInput_Emoji_Page)

    val numFittedColumns = calculateNumOfColumns(resources)
    emojiGrid.layoutManager = GridLayoutManager(context, numFittedColumns)
    emojiGrid.adapter = Adapter()

    if (savedInstanceState != null) {
      emojiCategory = savedInstanceState.getParcelable(EMOJI_CATEGORY)!!
    }
    return emojiGrid
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    if (this::emojiCategory.isInitialized) {
      outState.putParcelable(EMOJI_CATEGORY, emojiCategory)
    }
  }

  fun with(emojiCategory: EmojiCategory) = this.apply {
    this.emojiCategory = emojiCategory
  }

  private inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      val view = LayoutInflater.from(
          parent.context).inflate(R.layout.view_emoji_item, parent, false)
      return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      val emoji = emojiCategory.emojis[position]
      holder.bind(emoji)
    }

    override fun getItemCount(): Int {
      return emojiCategory.emojis.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      private val textView: TextView = itemView as TextView

      fun bind(emoji: Emoji) {
        textView.text = emoji.strValue
        textView.contentDescription = emoji.aliases[0]
        itemView.setOnLongClickListener {
          Toast.makeText(context, emoji.aliases[0], Toast.LENGTH_SHORT).show()
          true
        }
        itemView.setOnClickListener { flexInputFrag?.append(emoji.strValue) }
      }

    }
  }

  companion object {
    const val EMOJI_CATEGORY = "emoji_category"
    fun calculateNumOfColumns(resources: Resources): Int {
      val displayMetrics = resources.displayMetrics
      return (displayMetrics.widthPixels / resources.getDimension(R.dimen.emoji_grid_item_size)).toInt()
    }
  }
}