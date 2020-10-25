package com.lytefast.flexinput.sampleapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.lytefast.flexinput.InputListener
import com.lytefast.flexinput.adapters.AddContentPagerAdapter.PageSupplier
import com.lytefast.flexinput.adapters.AttachmentPreviewAdapter
import com.lytefast.flexinput.adapters.EmptyListAdapter
import com.lytefast.flexinput.fragment.CameraFragment
import com.lytefast.flexinput.fragment.FilesFragment
import com.lytefast.flexinput.fragment.FlexInputFragment
import com.lytefast.flexinput.fragment.PhotosFragment
import com.lytefast.flexinput.managers.KeyboardManager
import com.lytefast.flexinput.managers.SimpleFileManager
import com.lytefast.flexinput.sampleapp.IntentUtil.consumeSendIntent

/**
 * Sample of how to use the [FlexInputFragment] component.
 *
 * @author Sam Shih
 */
class MainFragment : Fragment() {

  private lateinit var recyclerView: RecyclerView
  private lateinit var flexInput: FlexInputFragment
  private val msgAdapter: MessageAdapter = MessageAdapter()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.fragment_message_main, container, false)
    recyclerView = view.findViewById(R.id.message_list)
    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    recyclerView.adapter = msgAdapter
    flexInput = childFragmentManager.findFragmentById(R.id.flex_input) as FlexInputFragment

    if (savedInstanceState == null) {
      // Only create fragment on first load
      // UnicodeEmojiCategoryPagerFragment is a default implementation (see sample app)
      flexInput.setEmojiFragment(UnicodeEmojiCategoryPagerFragment())
    }
    flexInput
        .setContentPages()
        .setInputListener(flexInputListener)
        .setFileManager(SimpleFileManager("com.lytefast.flexinput.fileprovider", "FlexInput"))
        .setKeyboardManager(object : KeyboardManager {
          override fun requestDisplay(editText: EditText) {
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
          }

          override fun requestHide() {
            imm.hideSoftInputFromWindow(flexInput.view?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
          }
        })
    optionalFeatures()
    tryRiskyFeatures()
    consumeSendIntent()
  }

  private fun consumeSendIntent() {
    val intent = requireActivity().intent
    val attachment = intent.consumeSendIntent(requireContext().contentResolver)
    if (attachment != null) {
      flexInput.addExternalAttachment(attachment)
      // Look for text
      val text = intent.getStringExtra(Intent.EXTRA_TEXT)
      if (!text.isNullOrBlank()) {
        flexInput.setText(text)
        intent.removeExtra(Intent.EXTRA_TEXT)
      }
    }
  }

  private fun optionalFeatures() {
    flexInput // Can be extended to provide custom previews (e.g. larger preview images, onclick) etc.
        .setAttachmentPreviewAdapter(AttachmentPreviewAdapter(requireContext().contentResolver))
    val hasCustomPages = false
    if (hasCustomPages) {
      flexInput.setContentPages(*createContentPages())
    }
  }

  private fun tryRiskyFeatures() {
    val hasCustomEditText = false
    if (hasCustomEditText) {
      val inflater = LayoutInflater.from(context)
      val myEditText = inflater.inflate(
          R.layout.my_edit_text_view, flexInput.view as ViewGroup?, false) as AppCompatEditText
      flexInput.setEditTextComponent(myEditText)
    }
  }

  override fun onResume() {
    super.onResume()
    flexInput.requestFocus()
  }

  class CustomFilesFragment : FilesFragment() {
    override fun newPermissionsRequestAdapter(onClickListener: View.OnClickListener): EmptyListAdapter {
      return EmptyListAdapter(
          R.layout.custom_permission_storage, R.id.permissions_req_btn, onClickListener)
    }
  }

  /**
   * Main point of interaction between the [FlexInputFragment] widget and the client.
   */
  private val flexInputListener = InputListener { data, attachments ->
    if (data.isNotEmpty()) {
      msgAdapter.addMessage(MessageAdapter.Data(data, null))
    }
    for (i in attachments.indices) {
      msgAdapter.addMessage(MessageAdapter.Data(
          Editable.Factory.getInstance().newEditable(String.format("[%d] Attachment", i)),
          attachments[i]))
    }
    true
  }

  companion object {
    /**
     * Not necessary if the defaults are sufficient. Add to this array if custom pages needed.
     */
    private fun createContentPages(): Array<PageSupplier> {
      return arrayOf(
          object : PageSupplier(R.drawable.ic_image_24dp, R.string.attachment_photos) {
            override fun createFragment(): Fragment {
              return PhotosFragment()
            }
          },
          object : PageSupplier(R.drawable.ic_file_24dp, R.string.attachment_files) {
            override fun createFragment(): Fragment {
              return CustomFilesFragment()
            }
          },
          object : PageSupplier(R.drawable.ic_add_a_photo_24dp, R.string.attachment_camera) {
            override fun createFragment(): Fragment {
              return CameraFragment()
            }
          }
      )
    }
  }

}