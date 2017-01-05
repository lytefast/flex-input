package com.lytefast.flexinput.adapters;

/**
 * Notified when an item in a {@link android.support.v7.widget.RecyclerView} is clicked.
 * 
 * @param <T> The type of item data stored by the {@link android.support.v7.widget.RecyclerView.Adapter}
 *           
 */
public interface OnItemClickListener<T> {
  void onItemClicked(T item, int position);
}
