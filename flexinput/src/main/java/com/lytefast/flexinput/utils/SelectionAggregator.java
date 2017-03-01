package com.lytefast.flexinput.utils;

import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.lytefast.flexinput.adapters.AttachmentPreviewAdapter;
import com.lytefast.flexinput.model.Attachment;

import java.util.ArrayList;


/**
 * Managers a collection of {@link SelectionCoordinator}s, and aggregates the selections to produce
 * a insert ordered list.
 *
 * @author Sam Shih
 */
public class SelectionAggregator<T extends Attachment> {

  public static final String TAG = SelectionAggregator.class.getCanonicalName();

  @SuppressWarnings("WeakerAccess")
  protected final ArrayList<T> attachments;
  @SuppressWarnings("WeakerAccess")
  protected final ArrayList<SelectionCoordinator<T>> childSelectionCoordinators;

  @SuppressWarnings("WeakerAccess")
  protected ArrayList<SelectionCoordinator.ItemSelectionListener> itemSelectionListeners;

  private final RecyclerView.Adapter adapter;


  public SelectionAggregator(final AttachmentPreviewAdapter<T> adapter) {
    this.adapter = adapter;
    this.attachments = new ArrayList<>();
    this.childSelectionCoordinators = new ArrayList<>(4);
    this.itemSelectionListeners = new ArrayList<>(4);
  }

  /**
   * This method allows this instance to take over responsibilities from an old {@link SelectionAggregator}.
   *
   * @param old the previous {@link SelectionAggregator}
   *
   * @return <code>this</code> instance
   */
  public SelectionAggregator<T> initFrom(final SelectionAggregator<T> old) {
    if (old != null) {
      this.attachments.addAll(old.attachments);
      for (SelectionCoordinator<T> coordinator : old.childSelectionCoordinators) {
        registerSelectionCoordinatorInternal(coordinator);
      }
      this.itemSelectionListeners.addAll(old.itemSelectionListeners);
    }
    return this;
  }

  public SelectionAggregator<T> initFrom(final ArrayList<Parcelable> savedAttachments) {
    for (Parcelable p : savedAttachments) {
      T attachment = (T) p;
      toggleItemInternal(attachment);
    }
    return this;
  }

  public SelectionAggregator<T> addItemSelectionListener(
      final SelectionCoordinator.ItemSelectionListener itemSelectionListener) {
    if (!itemSelectionListeners.contains(itemSelectionListener)) {
      itemSelectionListeners.add(itemSelectionListener);
    }
    return this;
  }

  public void removeItemSelectionListener(
      final SelectionCoordinator.ItemSelectionListener itemSelectionListener) {
    itemSelectionListeners.remove(itemSelectionListener);
  }

  //region Attachment property getters

  public ArrayList<T> getAttachments() {
    return attachments;
  }

  public int getSize() {
    return attachments.size();
  }

  public T get(int position) {
    return attachments.get(position);
  }

  //endregion

  public void clear() {
    attachments.clear();
    for (SelectionCoordinator<?> coordinator : childSelectionCoordinators) {
      coordinator.clear();
    }
  }

  /**
   * Convenience method to toggle the selection state for the item.
   *
   * @param item     instance of the item to be toggled. This must have {@link #equals(Object)}
   *                 and {@link #hashCode()} equivalancy for equal items.
   *
   * @return True if the item was added. False otherwise.
   */
  public boolean toggleItemInternal(final T item) {
    final boolean wasRemoved = removeItem(item);
    if (!wasRemoved) {
      addItem(item);
    }
    return wasRemoved;
  }

  private void addItem(final T item) {
    if (attachments.contains(item)) {
      return;
    }

    attachments.add(item);
    final int position = attachments.size() - 1;
    adapter.notifyItemInserted(position);

    for (SelectionCoordinator.ItemSelectionListener l : itemSelectionListeners) {
      l.onItemSelected(item);
    }
  }

  private boolean removeItem(final T item) {
    final int oldIndex = attachments.indexOf(item);
    final boolean wasRemoved = attachments.remove(item);
    if (wasRemoved) {
      adapter.notifyItemRemoved(oldIndex);
    }

    for (SelectionCoordinator.ItemSelectionListener l : itemSelectionListeners) {
      l.onItemUnselected(item);
    }
    return wasRemoved;
  }

  /**
   * Allows the aggregator to unselect an item.
   * <p>
   * This is the only method provided as selection needs to have a position reference to work properly.
   *
   * @see SelectionCoordinator#selectItem(Object, int)
   */
  public void unselectItem(final T item) {
    // Let the child delete the item, and notify us
    for (SelectionCoordinator<T> coordinator : childSelectionCoordinators) {
      coordinator.unselectItem(item);
    }
    removeItem(item);
  }

  /**
   * Notify that items from a collection are selectable.
   * This is the primary means to add items to the FlexInput message.
   *
   * @param selectionCoordinator instance that manages a collection of selectable items
   */
  public void registerSelectionCoordinator(final SelectionCoordinator<T> selectionCoordinator) {
    registerSelectionCoordinatorInternal(selectionCoordinator);
    try {
      selectionCoordinator.restoreSelections(attachments);
    } catch (SelectionCoordinator.RestorationException e) {
      Log.d(TAG, "selections could not be synced", e);
    }
  }

  protected void registerSelectionCoordinatorInternal(
      final SelectionCoordinator<T> selectionCoordinator) {
    selectionCoordinator.setItemSelectionListener(new SelectionCoordinator.ItemSelectionListener<T>() {
      @Override
      public void onItemSelected(T item) {
        addItem(item);
      }

      @Override
      public void onItemUnselected(T item) {
        removeItem(item);
      }

      @Override
      public void unregister() {
        childSelectionCoordinators.remove(selectionCoordinator);
      }
    });
    this.childSelectionCoordinators.add(selectionCoordinator);
  }
}
