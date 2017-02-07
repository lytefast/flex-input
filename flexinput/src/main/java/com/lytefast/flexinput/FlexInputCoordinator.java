package com.lytefast.flexinput;

import com.lytefast.flexinput.managers.FileManager;
import com.lytefast.flexinput.model.Attachment;
import com.lytefast.flexinput.utils.SelectionAggregator;
import com.lytefast.flexinput.utils.SelectionCoordinator;

import java.util.List;


/**
 * Defines methods used to co-ordinate events and setup needed for the FlexInput components to
 * function with eachother.
 *
 * @param <T> Type of item that can be selected.
 *
 * @author Sam Shih
 */
public interface FlexInputCoordinator<T extends Attachment> {
  FileManager getFileManager();

  /**
   * Get the {@link SelectionAggregator} instance that manages all selectable items before sending.
   */
  SelectionAggregator<T> getSelectionAggregator();

  void onPhotoTaken(T photo);
}
