package com.lytefast.flexinput.utils;

import android.support.v4.util.ArrayMap;

import java.util.ArrayList;
import java.util.Set;


/**
 * Manages selection logic for {@link android.support.v7.widget.RecyclerView.Adapter}s.
 *
 * @author Sam Shih
 */
public class SelectionCoordinator<T> {

  /**
   * Maintains a mapping from the selected item to the position in the adapter.
   */
  private final ArrayMap<T, Integer> selectedItemPositionMap;

  public SelectionCoordinator() {
    this.selectedItemPositionMap = new ArrayMap<>(4);
  }

  public Set<T> getSelectedItems() {
    return selectedItemPositionMap.keySet();
  }

  /**
   * @return List of positions which were previously selected. This should be used to update the
   * UI via {@link android.support.v7.widget.RecyclerView.Adapter#notifyItemChanged(int)}
   */
  public ArrayList<Integer> clearSelectedItems() {
    ArrayList<Integer> oldSelection = new ArrayList<>(selectedItemPositionMap.values());
    selectedItemPositionMap.clear();
    return oldSelection;
  }

  public boolean isSelected(T item) {
    return selectedItemPositionMap.containsKey(item);
  }

  /**
   * Toggle the selection state for the item.
   *
   * @return True if the item was added. False otherwise.
   */
  public boolean toggleItem(T item, int position) {
    if (selectedItemPositionMap.remove(item) == null) {
      selectedItemPositionMap.put(item, position);
      onItemSelected(item);
      return true;
    }
    onItemUnselected(item);
    return false;
  }

  public void onItemSelected(T item) {}
  public void onItemUnselected(T item) {}
}
