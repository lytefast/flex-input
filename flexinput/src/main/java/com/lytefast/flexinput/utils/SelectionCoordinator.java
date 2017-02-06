package com.lytefast.flexinput.utils;

import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Manages selection logic for {@link android.support.v7.widget.RecyclerView.Adapter}s.
 *
 * @author Sam Shih
 */
public class SelectionCoordinator<T> {

  /**
   * Maintains a mapping from the selected item to the position in the adapter.
   */
  @SuppressWarnings("WeakerAccess")
  protected final ArrayMap<T, Integer> selectedItemPositionMap;
  @SuppressWarnings("WeakerAccess")
  protected ItemSelectionListener itemSelectionListener;
  /**
   * The {@link android.support.v7.widget.RecyclerView.Adapter} that should be notified when selection changes occur.
   */
  @SuppressWarnings("WeakerAccess")
  protected RecyclerView.Adapter<?> adapter;


  public SelectionCoordinator() {
    this.selectedItemPositionMap = new ArrayMap<>(4);
    this.itemSelectionListener = new ItemSelectionListener();
  }

  public SelectionCoordinator bind(RecyclerView.Adapter<?> adapter) {
    this.adapter = adapter;
    return this;
  }

  public void setItemSelectionListener(ItemSelectionListener listener) {
    this.itemSelectionListener = listener;
  }

  /**
   * @return List of positions which were previously selected. This should be used to update the
   * UI via {@link android.support.v7.widget.RecyclerView.Adapter#notifyItemChanged(int)}
   */
  public ArrayList<Integer> clear() {
    ArrayList<Integer> oldSelection = new ArrayList<>(selectedItemPositionMap.values());
    selectedItemPositionMap.clear();
    if (adapter != null) {
      for (int position : oldSelection) {
        adapter.notifyItemChanged(position);
      }
    }
    return oldSelection;
  }

  public boolean isSelected(T item, int position) {
    Integer knownPosition = selectedItemPositionMap.get(item);
    if (knownPosition == null) {
      return false;
    }

    if(position != knownPosition) {
      // Update the position as it might have changed
      // Or we restored the item from an external source.
      selectedItemPositionMap.put(item, position);
    }
    return true;
  }

  /**
   * Convience method to toggle the selection state for the item.
   *
   * @param item     instance of the item to be toggled. This must have {@link #equals(Object)}
   *                 and {@link #hashCode()} equivalancy for equal items.
   * @param position the position in the list where the item appears. Inspected only on adds and
   *                 used to notify the adapter on removals.
   *
   * @return True if the item was added. False otherwise.
   */
  public boolean toggleItem(T item, int position) {
    if (unselectItem(item)) {
      return false;
    }

    // Select item
    selectItem(item, position);
    return true;
  }

  /**
   * Mark an item as selected.
   *
   * @param item     instance of the item to be toggled. This must have {@link #equals(Object)}
   *                 and {@link #hashCode()} equivalancy for equal items.
   * @param position the position in the list where the item appears
   */
  public void selectItem(T item, final int position) {
    selectedItemPositionMap.put(item, position);
    if (adapter != null) {
      adapter.notifyItemChanged(position);
    }
    itemSelectionListener.onItemSelected(item);
  }

  /**
   * Mark an item as unselected.
   *
   * @param item     instance of the item to be toggled. This must have {@link #equals(Object)}
   *                 and {@link #hashCode()} equivalancy for equal items.
   *
   * @return True if the item was unselected. False otherwise.
   */
  public boolean unselectItem(T item) {
    final Integer removedItemPosition = selectedItemPositionMap.remove(item);
    if (removedItemPosition == null) {
      return false;
    }

    if (adapter != null) {
      adapter.notifyItemChanged(removedItemPosition);
    }
    itemSelectionListener.onItemUnselected(item);
    return true;
  }

  /**
   * Presets the selections of this {@link SelectionCoordinator} to values set by an external source.
   *
   * @param selectedItems list of items that have been selected by an external source
   *
   * @throws RestorationException if the adapter has already been set. This is thrown to prevent mismatches.
   */
  public void restoreSelections(ArrayList<T> selectedItems) throws RestorationException {
    if (adapter != null) {
      throw new RestorationException("cannot restoreSelections after adapter set: prevents mismatches");
    }

    for (T item : selectedItems) {
      selectedItemPositionMap.put(item, -1);
    }
  }

  public void close() {
    if (itemSelectionListener != null) {
      itemSelectionListener.unregister();
    }
  }

  public static class RestorationException extends Exception {
    RestorationException(String msg) {
      super(msg);
    }
  }

  public static class ItemSelectionListener<T> {
    public void onItemSelected(T item) {}
    public void onItemUnselected(T item) {}

    /**
     * Signals that no new notifications are required. This should be called when the
     * {@link SelectionCoordinator} goes out of scope to clean up references.
     */
    public void unregister() {};
  }
}
