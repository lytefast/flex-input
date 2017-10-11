package com.lytefast.flexinput.utils

import android.os.Parcelable
import android.util.Log
import com.lytefast.flexinput.adapters.AttachmentPreviewAdapter
import com.lytefast.flexinput.model.Attachment
import java.util.*


/**
 * Managers a collection of [SelectionCoordinator]s, and aggregates the selections to produce
 * a insert ordered list.
 *
 * @author Sam Shih
 */
open class SelectionAggregator<T: Attachment<Any>>(
    val adapter: AttachmentPreviewAdapter<T>,
    val attachments: ArrayList<T> = arrayListOf(),
    protected val childSelectionCoordinators: ArrayList<SelectionCoordinator<T, *>> = ArrayList(4),
    protected var itemSelectionListeners: ArrayList<SelectionCoordinator.ItemSelectionListener<T>> = ArrayList(4)) {

  /**
   * This method allows this instance to take over responsibilities from an old [SelectionAggregator].
   *
   * @param old the previous [SelectionAggregator]
   *
   * @return `this` instance
   */
  fun initFrom(old: SelectionAggregator<T>?): SelectionAggregator<T> {
    if (old != null) {
      this.attachments.addAll(old.attachments)
      for (coordinator in old.childSelectionCoordinators) {
        registerSelectionCoordinatorInternal(coordinator)
      }
      this.itemSelectionListeners.addAll(old.itemSelectionListeners)
    }
    return this
  }

  fun initFrom(savedAttachments: ArrayList<in Parcelable>): SelectionAggregator<T> {
    savedAttachments
        .mapNotNull {
          @Suppress("UNCHECKED_CAST")
          it as? T
        }
        .forEach { toggleItemInternal(it) }
    return this
  }

  fun addItemSelectionListener(
      itemSelectionListener: SelectionCoordinator.ItemSelectionListener<T>): SelectionAggregator<T> {
    if (!itemSelectionListeners.contains(itemSelectionListener)) {
      itemSelectionListeners.add(itemSelectionListener)
    }
    return this
  }

  fun removeItemSelectionListener(
      itemSelectionListener: SelectionCoordinator.ItemSelectionListener<*>) {
    itemSelectionListeners.remove(itemSelectionListener)
  }

  val size: Int
    get() = attachments.size

  operator fun get(position: Int) = attachments[position]

  fun clear() {
    attachments.clear()
    childSelectionCoordinators.forEach { it.clear() }
  }

  /**
   * Convenience method to toggle the selection state for the item.
   *
   * @param item     instance of the item to be toggled. This must have [.equals]
   * and [.hashCode] equivalancy for equal items.
   *
   * @return True if the item was added. False otherwise.
   */
  protected fun toggleItemInternal(item: T): Boolean {
    val wasRemoved = removeItem(item)
    if (!wasRemoved) {
      addItem(item)
    }
    return wasRemoved
  }

  private fun addItem(item: T) {
    if (attachments.contains(item)) {
      return
    }

    attachments.add(item)
    val position = attachments.size - 1
    adapter.notifyItemInserted(position)

    itemSelectionListeners.forEach { it.onItemSelected(item) }
  }

  private fun removeItem(item: T): Boolean {
    val oldIndex = attachments.indexOf(item)
    val wasRemoved = attachments.remove(item)
    if (wasRemoved) {
      adapter.notifyItemRemoved(oldIndex)
    }

    itemSelectionListeners.forEach { it.onItemUnselected(item) }
    return wasRemoved
  }

  /**
   * Allows the aggregator to unselect an item.
   *
   * This is the only method provided as selection needs to have a position reference to work properly.
   *
   * @see SelectionCoordinator.selectItem
   */
  fun unselectItem(item: T) {
    // Let the child delete the item, and notify us
    childSelectionCoordinators.forEach { it.unselectItem(item) }
    removeItem(item)
  }

  /**
   * Notify that items from a collection are selectable.
   * This is the primary means to add items to the FlexInput message.
   *
   * @param selectionCoordinator instance that manages a collection of selectable items
   */
  fun registerSelectionCoordinator(selectionCoordinator: SelectionCoordinator<T, *>) {
    registerSelectionCoordinatorInternal(selectionCoordinator)
    try {
      selectionCoordinator.restoreSelections(attachments)
    } catch (e: SelectionCoordinator.RestorationException) {
      Log.d(TAG, "selections could not be synced", e)
    }

  }

  protected fun registerSelectionCoordinatorInternal(
      selectionCoordinator: SelectionCoordinator<T, *>) {
    selectionCoordinator.itemSelectionListener = object : SelectionCoordinator.ItemSelectionListener<T>() {
      override fun onItemSelected(item: T) {
        addItem(item)
      }

      override fun onItemUnselected(item: T) {
        removeItem(item)
      }

      override fun unregister() {
        childSelectionCoordinators.remove(selectionCoordinator)
      }
    }
    this.childSelectionCoordinators.add(selectionCoordinator)
  }

  companion object {
    val TAG = SelectionAggregator::class.java.canonicalName!!
  }
}
