package com.lytefast.flexinput.utils

import android.support.v4.util.ArrayMap
import android.support.v7.widget.RecyclerView
import java.util.*


/**
 * Manages selection logic for [android.support.v7.widget.RecyclerView.Adapter]s.
 *
 * @author Sam Shih
 */
open class SelectionCoordinator<T>(
    /**
     * Maintains a mapping from the selected item to the position in the adapter.
     */
    @Suppress("MemberVisibilityCanPrivate")
    protected val selectedItemPositionMap: ArrayMap<T, Int> = ArrayMap(4),
    var itemSelectionListener: ItemSelectionListener<T> = ItemSelectionListener()) {

  /**
   * The [android.support.v7.widget.RecyclerView.Adapter] that should be notified when selection changes occur.
   */
  protected var adapter: RecyclerView.Adapter<*>? = null

  fun bind(adapter: RecyclerView.Adapter<*>): SelectionCoordinator<T> {
    this.adapter = adapter
    return this
  }

  /**
   * @return List of positions which were previously selected. This should be used to update the
   * UI via [android.support.v7.widget.RecyclerView.Adapter.notifyItemChanged]
   */
  fun clear(): ArrayList<Int> {
    val oldSelection = ArrayList(selectedItemPositionMap.values)
    selectedItemPositionMap.clear()
    adapter?.apply {
      for (position in oldSelection) {
        notifyItemChanged(position)
      }
    }
    return oldSelection
  }

  fun isSelected(item: T, position: Int): Boolean {
    val knownPosition = selectedItemPositionMap[item] ?: return false

    if (position != knownPosition) {
      // Update the position as it might have changed
      // Or we restored the item from an external source.
      selectedItemPositionMap.put(item, position)
    }
    return true
  }

  /**
   * Convenience method to toggle the selection state for the item.
   *
   * @param item     instance of the item to be toggled. This must have [.equals]
   * and [.hashCode] equivalancy for equal items.
   * @param position the position in the list where the item appears. Inspected only on adds and
   * used to notify the adapter on removals.
   *
   * @return True if the item was added. False otherwise.
   */
  fun toggleItem(item: T, position: Int): Boolean {
    if (unselectItem(item)) {
      return false
    }

    // Select item
    selectItem(item, position)
    return true
  }

  /**
   * Mark an item as selected.
   *
   * @param item     instance of the item to be toggled. This must have [.equals]
   * and [.hashCode] equivalancy for equal items.
   * @param position the position in the list where the item appears
   */
  fun selectItem(item: T, position: Int) {
    selectedItemPositionMap.put(item, position)
    adapter?.notifyItemChanged(position)
    itemSelectionListener.onItemSelected(item)
  }

  /**
   * Mark an item as unselected.
   *
   * @param item     instance of the item to be toggled. This must have [.equals]
   * and [.hashCode] equivalancy for equal items.
   *
   * @return True if the item was unselected. False otherwise.
   */
  fun unselectItem(item: T): Boolean {
    val removedItemPosition = selectedItemPositionMap.remove(item) ?: return false
    adapter?.notifyItemChanged(removedItemPosition)
    itemSelectionListener.onItemUnselected(item)
    return true
  }

  /**
   * Presets the selections of this [SelectionCoordinator] to values set by an external source.
   *
   * @param selectedItems list of items that have been selected by an external source
   *
   * @throws RestorationException if the adapter has already been set. This is thrown to prevent mismatches.
   */
  @Throws(RestorationException::class)
  fun restoreSelections(selectedItems: ArrayList<T>) {
    if (adapter != null) {
      throw RestorationException("cannot restoreSelections after adapter set: prevents mismatches")
    }

    for (item in selectedItems) {
      selectedItemPositionMap.put(item, -1)
    }
  }

  fun close() {
    itemSelectionListener?.unregister()
  }

  class RestorationException internal constructor(msg: String) : Exception(msg)

  open class ItemSelectionListener<in T> {
    open fun onItemSelected(item: T) {}
    open fun onItemUnselected(item: T) {}

    /**
     * Signals that no new notifications are required. This should be called when the
     * [SelectionCoordinator] goes out of scope to clean up references.
     */
    open fun unregister() {}
  }
}
