package com.lytefast.flexinput.adapters

import android.support.annotation.IdRes
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


/**
 * Simple [android.support.v7.widget.RecyclerView.Adapter] which just renders one item entry.
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
