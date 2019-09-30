package com.lytefast.flexinput.utils

import androidx.collection.ArrayMap
import androidx.recyclerview.widget.RecyclerView
import java.util.*


/**
 * Manages selection logic for [RecyclerView.Adapter]s.
 *
 * @param I The base acceptable type. Used as the type for all notifications. Usually [com.lytefast.flexinput.model.Attachment<Any>]
 * @param T The type that this class primarily handles.
 *
 * @author Sam Shih
 */
open class SelectionCoordinator<I, T : I>(
    /**
     * Maintains a mapping from the selected item to the position in the adapter.
     */
    @Suppress("MemberVisibilityCanPrivate")
    protected val selectedItemPositionMap: ArrayMap<T, Int> = ArrayMap(4),
    var itemSelectionListener: ItemSelectionListener<in I> = object : ItemSelectionListener<I> {
      override fun onItemSelected(item: I) {}
      override fun onItemUnselected(item: I) {}
      override fun unregister() {}
    }) {

  /**
   * The [RecyclerView.Adapter] that should be notified when selection changes occur.
   */
  protected var adapter: RecyclerView.Adapter<*>? = null

  fun bind(adapter: RecyclerView.Adapter<*>): SelectionCoordinator<I, T> {
    this.adapter = adapter
    return this
  }

  /**
   * @return List of positions which were previously selected. This should be used to update the
   * UI via [RecyclerView.Adapter.notifyItemChanged]
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
      selectedItemPositionMap[item] = position
    }
    return true
  }

  /**
   * Convenience method to toggle the selection state for the item.
   *
   * @param item     instance of the item to be toggled. This must have [.equals]
   *                 and [.hashCode] equivalancy for equal items.
   * @param position the position in the list where the item appears. Inspected only on adds and
   *                 used to notify the adapter on removals.
   *
   * @return True if the item was added. False otherwise.
   */
  fun toggleItem(item: T?, position: Int): Boolean {
    if (item == null) return false
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
   *                 and [.hashCode] equivalancy for equal items.
   * @param position the position in the list where the item appears
   */
  fun selectItem(item: T, position: Int) {
    selectedItemPositionMap.put(item, position)
    adapter?.notifyItemChanged(position, SelectionEvent(item, isSelected = true))
    itemSelectionListener.onItemSelected(item)
  }

  /**
   * Mark an item as unselected.
   *
   * @param item     instance of the item to be toggled. This must have [.equals]
   *                 and [.hashCode] equivalancy for equal items.
   *
   * @return True if the item was unselected. False otherwise.
   */
  fun unselectItem(item: I): Boolean {
    val removedItemPosition = selectedItemPositionMap.remove(item) ?: return false
    adapter?.notifyItemChanged(removedItemPosition, SelectionEvent(item, isSelected = false))
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
  fun restoreSelections(selectedItems: ArrayList<out I>) {
    if (adapter != null) {
      throw RestorationException("cannot restoreSelections after adapter set: prevents mismatches")
    }

    for (item in selectedItems) {
      @Suppress("UNCHECKED_CAST")
      (item as? T)?.also {
        selectedItemPositionMap.put(it, -1)
      }
    }
  }

  fun close() {
    itemSelectionListener.unregister()
  }

  class RestorationException internal constructor(msg: String) : Exception(msg)

  data class SelectionEvent<out T>(val item: T, val isSelected: Boolean)

  interface ItemSelectionListener<in I> {
    fun onItemSelected(item: I) {}
    fun onItemUnselected(item: I) {}

    /**
     * Signals that no new notifications are required. This should be called when the
     * [SelectionCoordinator] goes out of scope to clean up references.
     */
    fun unregister() {}
  }
}
