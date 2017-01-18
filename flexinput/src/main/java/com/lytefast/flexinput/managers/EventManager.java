package com.lytefast.flexinput.managers;

import com.lytefast.flexinput.model.Attachment;

import java.util.concurrent.Callable;


/**
 * Defines an interface that {@link com.lytefast.flexinput.FlexInput} will use to communicate internally.
 * This allows the client to specify whatever event bus system they prefer.
 *
 * @author Sam Shih
 */
public interface EventManager {

  <T> Bus<T> register(Object target, Class<T> clazz);

  void postOnItemClicked(Attachment item);
  void postOnClearAttachments();

  interface Bus<T> {

    void unregister();

   EventManager.Bus<T> using(EventCallback<T> c);
  }

  interface EventCallback<T> {
    void onEvent(T value);
  }
}
