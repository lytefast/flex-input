package com.lytefast.flexinput.eventbus;

import com.lytefast.flexinput.events.ClearAttachmentsEvent;
import com.lytefast.flexinput.events.ItemClickedEvent;
import com.lytefast.flexinput.managers.EventManager;
import com.lytefast.flexinput.model.Attachment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;


/**
 * @author Sam Shih
 */
public class EventBusManager implements EventManager {
  @Override
  public <T> EventManager.Bus<T> register(final Object target, final Class<T> clazz) {
    EventManager.Bus<T> bus;
    if (ItemClickedEvent.class.equals(clazz)) {
      return (EventManager.Bus<T>) new ItemClickedBus();
    } else if (ClearAttachmentsEvent.class.equals(clazz)) {
      return (EventManager.Bus<T>) new ClearAttachmentBus();
    }
    return null;
  }

  @Override
  public <T> void postOnItemClicked(final T item) {
    EventBus.getDefault().post(new ItemClickedEvent<>(item));
  }

  @Override
  public void postOnClearAttachments() {
    EventBus.getDefault().post(new ClearAttachmentsEvent());
  }

  public static class Bus<T> implements EventManager.Bus<T> {

    protected EventCallback<T> c;

    public Bus() {
      EventBus.getDefault().register(this);
    }

    @Override
    public void unregister() {
      EventBus.getDefault().unregister(this);
    }

    @Override
    public EventManager.Bus<T> using(final EventCallback<T> c) {
      this.c = c;
      return this;
    }
  }

  public static class ClearAttachmentBus extends Bus<ClearAttachmentsEvent> {
    @Subscribe
    public void onClearAttachmentClick(ClearAttachmentsEvent event) {
      this.c.onEvent(event);
    }
  }

  public static class ItemClickedBus extends Bus<ItemClickedEvent> {
    @Subscribe
    public void onItemClicked(ItemClickedEvent event) {
      this.c.onEvent(event);
    }
  }

}
