package com.lytefast.flexinput.events;

/**
 * Generic event that signals that an item was clicked.
 *
 * @author Sam Shih
 */
public class ItemClickedEvent<T> {
  public final T item;

  public ItemClickedEvent(final T item) {
    this.item = item;
  }
}
