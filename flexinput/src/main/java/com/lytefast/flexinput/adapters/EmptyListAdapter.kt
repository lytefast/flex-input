package com.lytefast.flexinput.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView


/**
 * Simple [RecyclerView.Adapter] which just renders one item entry.
 *
 * @author Sam Shih
 */
open class EmptyListAdapter(
    @LayoutRes private val itemLayoutId: Int,
    @IdRes private val actionBtnId: Int,
    private val onClickListener: View.OnClickListener) : RecyclerView.Adapter<EmptyListAdapter.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context)
        .inflate(itemLayoutId, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.actionBtn.setOnClickListener(onClickListener)
  }

  override fun getItemCount(): Int = 1

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    internal val actionBtn: View = itemView.findViewById(actionBtnId)
  }
}
