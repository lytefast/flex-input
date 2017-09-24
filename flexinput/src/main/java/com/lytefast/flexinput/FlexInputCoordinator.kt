package com.lytefast.flexinput

import com.lytefast.flexinput.managers.FileManager
import com.lytefast.flexinput.model.Attachment
import com.lytefast.flexinput.utils.SelectionAggregator


/**
 * Defines methods used to co-ordinate events and setup needed for the FlexInput components to
 * function with eachother.
 *
 * @param <T> Type of item that can be selected.
 *
 * @author Sam Shih
*/
interface FlexInputCoordinator<T : Any> {
  val fileManager: FileManager
    get

  /**
   * Get the [SelectionAggregator] instance that manages all selectable items before sending.
   */
  val selectionAggregator: SelectionAggregator<Attachment<T>>

  fun addExternalAttachment(attachment: Attachment<T>)
}
