package com.lytefast.flexinput.rxjava;

import android.support.annotation.NonNull;

import com.lytefast.flexinput.events.ClearAttachmentsEvent;
import com.lytefast.flexinput.events.ItemClickedEvent;
import com.lytefast.flexinput.managers.EventManager;
import com.lytefast.flexinput.model.Attachment;

import java.util.HashMap;
import java.util.Map;

import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;


/**
 * @author Sam Shih
 */
public class EventRxJavaManager implements EventManager {

  private final Map<Class<?>, SerializedSubject<?, ?>> publishSubjectMap;

  public EventRxJavaManager() {
    publishSubjectMap = new HashMap<>();
  }

  @Override
  public <T> Bus<T> register(final Object target, final Class<T> clazz) {
    SerializedSubject<T, T> rxBus = getOrCreate(clazz);
    return new Bus<>(rxBus);
  }

  @NonNull
  private <T> SerializedSubject<T, T> getOrCreate(final Class<T> clazz) {
    SerializedSubject<T, T> rxBus = (SerializedSubject<T, T>) publishSubjectMap.get(clazz);
    if (rxBus == null) {
      rxBus = new SerializedSubject<>(PublishSubject.<T>create());
      publishSubjectMap.put(clazz, rxBus);
    }
    return rxBus;
  }

  @Override
  public <T> void postOnItemClicked(final T item) {
    getOrCreate(ItemClickedEvent.class)
        .onNext(new ItemClickedEvent(item));
  }

  @Override
  public void postOnClearAttachments() {
    getOrCreate(ClearAttachmentsEvent.class)
        .onNext(new ClearAttachmentsEvent());
  }

  class Bus<T> implements EventManager.Bus<T> {

    private final SerializedSubject<T, T> rxBus;
    private Subscription subs;

    public Bus(SerializedSubject<T, T> rxBus) {
      this.rxBus = rxBus;
    }

    @Override
    public void unregister() {
      if (subs != null && !subs.isUnsubscribed()) {
        subs.unsubscribe();
      }
    }

    @Override
    public Bus<T> using(@NonNull final EventCallback<T> c) {
      Subscription subscription = rxBus.subscribe(new Action1<T>() {
        @Override
        public void call(final T t) {
          c.onEvent(t);
        }
      });

      this.subs = subscription;
      return this;
    }
  }
}
