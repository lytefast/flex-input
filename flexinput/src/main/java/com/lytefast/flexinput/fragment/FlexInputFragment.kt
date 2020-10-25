package com.lytefast.flexinput.fragment

import android.content.Context
import android.content.DialogInterface
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.children
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.lytefast.flexinput.FlexInputCoordinator
import com.lytefast.flexinput.InputListener
import com.lytefast.flexinput.R
import com.lytefast.flexinput.adapters.AddContentPagerAdapter.Companion.createDefaultPages
import com.lytefast.flexinput.adapters.AddContentPagerAdapter.PageSupplier
import com.lytefast.flexinput.adapters.AttachmentPreviewAdapter
import com.lytefast.flexinput.managers.FileManager
import com.lytefast.flexinput.managers.KeyboardManager
import com.lytefast.flexinput.model.Attachment
import com.lytefast.flexinput.model.Attachment.Companion.toAttachment
import com.lytefast.flexinput.utils.FlexInputEmojiStateChangeListener
import com.lytefast.flexinput.utils.SelectionAggregator
import com.lytefast.flexinput.utils.SelectionCoordinator
import com.lytefast.flexinput.utils.SelectionCoordinator.ItemSelectionListener
import com.lytefast.flexinput.widget.FlexEditText

/**
 * Main widget fragment that controls all aspects of the FlexInput widget.
 *
 *
 * This is the controller which maintains all the interactions between the various components.
 *
 * @author Sam Shih
 */
@Suppress("MemberVisibilityCanBePrivate")
open class FlexInputFragment : Fragment(), FlexInputCoordinator<Any> {
  private lateinit var attachmentPreviewContainer: View
  private lateinit var attachmentClearButton: View
  private lateinit var inputContainer: LinearLayout
  private lateinit var emojiContainer: View
  private lateinit var attachmentPreviewList: RecyclerView
  private lateinit var textEt: AppCompatEditText
  private lateinit var emojiBtn: AppCompatImageButton
  private lateinit var sendBtn: AppCompatImageButton

  // Keep here so we know it's available
  private var addBtn: View? = null

  /**
   * Temporarily stores the UI attributes until we can apply them after inflation.
   */
  private var initializeUiAttributes: Runnable? = null
  private var keyboardManager: KeyboardManager? = null
  private var inputListener: InputListener<Any>? = null

  @Suppress("MemberVisibilityCanBePrivate")
  protected lateinit var attachmentPreviewAdapter: AttachmentPreviewAdapter<Attachment<Any>>

  @Suppress("MemberVisibilityCanBePrivate")
  protected var pageSuppliers: Array<out PageSupplier>? = null
  var isEnabled = true
    private set

  protected lateinit var fileManager_: FileManager
  override val fileManager: FileManager
    get() = fileManager_

  //region Lifecycle Methods
  override fun onInflate(context: Context, attrs: AttributeSet, savedInstanceState: Bundle?) {
    super.onInflate(context, attrs, savedInstanceState)
    initializeUiAttributes = Runnable {
      val attrTypedArray = context.obtainStyledAttributes(attrs, R.styleable.FlexInput)
      try {
        initAttributes(attrTypedArray)
      } finally {
        attrTypedArray.recycle()
      }
    }
    // Set this so we can capture SelectionCoordinators ASAP
    attachmentPreviewAdapter = initDefaultAttachmentPreviewAdapter(context)
  }

  private fun initDefaultAttachmentPreviewAdapter(context: Context)
      : AttachmentPreviewAdapter<Attachment<Any>> {
    val adapter = AttachmentPreviewAdapter<Attachment<Any>>(context.contentResolver)
    adapter.selectionAggregator
        .addItemSelectionListener(object : ItemSelectionListener<Attachment<Any>> {
          override fun onItemSelected(item: Attachment<Any>) {
            updateUi()
          }

          override fun onItemUnselected(item: Attachment<Any>) {
            updateUi()
          }

          override fun unregister() {}
          private fun updateUi() {
            val rootView = view ?: return
            rootView.post {
              updateSendBtnEnableState(textEt.text)
              updateAttachmentPreviewContainer()
            }
          }
        })
    return adapter
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    val root = inflater.inflate(
        R.layout.flex_input_widget, container, false) as LinearLayout
    attachmentPreviewContainer = root.findViewById(R.id.attachment_preview_container)
    attachmentClearButton = root.findViewById<View>(R.id.attachment_clear_btn)
        .apply { setOnClickListener { clearAttachments() } }
    inputContainer = root.findViewById(R.id.main_input_container)
    emojiContainer = root.findViewById(R.id.emoji_container)
    attachmentPreviewList = root.findViewById(R.id.attachment_preview_list)
    textEt = root.findViewById(R.id.text_input)
    bindTextInput(textEt)
    bindButtons(root)
    initializeUiAttributes!!.run()
    initializeUiAttributes = null
    setAttachmentPreviewAdapter(AttachmentPreviewAdapter(requireContext().contentResolver))
    return root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    if (savedInstanceState != null) {
      val savedAttachments = savedInstanceState.getParcelableArrayList<Parcelable>(EXTRA_ATTACHMENTS)
      if (savedAttachments != null && savedAttachments.size > 0) {
        attachmentPreviewAdapter.selectionAggregator.initFrom(savedAttachments)
      }
      val text = savedInstanceState.getString(EXTRA_TEXT)
      setText(text)
    }
  }

  fun setText(text: String?) {
    textEt.setText(text)
    if (!TextUtils.isEmpty(text)) {
      textEt.setSelection(text!!.length)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putParcelableArrayList(
        EXTRA_ATTACHMENTS, attachmentPreviewAdapter.selectionAggregator.attachments)
    outState.putString(EXTRA_TEXT, textEt.text.toString())
  }

  override fun onPause() {
    hideEmojiTray()
    keyboardManager!!.requestHide()
    super.onPause()
  }

  private fun bindButtons(root: View) {
    emojiBtn = root.findViewById(R.id.emoji_btn)
    emojiBtn.setOnClickListener { onEmojiToggle() }
    sendBtn = root.findViewById(R.id.send_btn)
    sendBtn.setOnClickListener { onSend() }
    addBtn = root.findViewById(R.id.add_btn)
    addBtn?.setOnClickListener { onAddToggle() }

    arrayOf(attachmentClearButton, addBtn, emojiBtn, sendBtn)
        .filterNotNull()
        .forEach { view -> view.setOnLongClickListener { tooltipButton(it) } }

    if (childFragmentManager.findFragmentById(R.id.emoji_container) != null) {
      emojiBtn.visibility = View.VISIBLE
    }
  }

  private fun bindTextInput(editText: AppCompatEditText) {
    editText.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
      override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
      override fun afterTextChanged(editable: Editable) {
        updateSendBtnEnableState(editable)
      }
    })

    editText.setOnTouchListener { _, motionEvent -> onTextInputTouch(motionEvent) }

    if (editText is FlexEditText) {
      val flexEt = editText
      if (flexEt.inputContentHandler == null) {
        // Set a default
        flexEt.inputContentHandler = { inputContentInfoCompat: InputContentInfoCompat ->
          addExternalAttachment(inputContentInfoCompat.toAttachment(requireContext().contentResolver, true, "unknown"))
        }
      }
    }
  }

  private fun initAttributes(typedArray: TypedArray) {
    val hintText = typedArray.getText(R.styleable.FlexInput_hint)
    if (!TextUtils.isEmpty(hintText)) {
      textEt.hint = hintText
    }
    if (typedArray.hasValue(R.styleable.FlexInput_hintColor)) {
      @ColorInt val hintColor = typedArray.getColor(R.styleable.FlexInput_hintColor, Color.LTGRAY)
      textEt.setHintTextColor(hintColor)
    }
    val backgroundDrawable = typedArray.getDrawable(R.styleable.FlexInput_previewBackground)
    if (backgroundDrawable != null) {
      backgroundDrawable.callback = view
      attachmentPreviewContainer.background = backgroundDrawable
    }
  }
  //endregion

  //region Functional Getters/Setters
  /**
   * Set the custom emoji [Fragment] for the input.
   *
   * Note that this should only be set once for the life of the containing fragment. Make sure to
   * check the `savedInstanceState` before creating and saving another fragment.
   */
  fun setEmojiFragment(emojiFragment: Fragment?) = this.apply {
    childFragmentManager
        .beginTransaction()
        .replace(R.id.emoji_container, emojiFragment!!)
        .commit()
    emojiBtn.visibility = View.VISIBLE
  }

  fun setInputListener(inputListener: InputListener<Any>) = this.apply {
    this.inputListener = inputListener
  }

  /**
   * Set an [RecyclerView.Adapter] implementation that knows how render [Attachment]s.
   * If this is not set, no attachment preview will be shown.
   *
   * @param previewAdapter An adapter that knows how to display [Attachment]s
   *
   * @return the current instance of [FlexInputFragment] for chaining commands
   * @see AttachmentPreviewAdapter
   */
  fun setAttachmentPreviewAdapter(previewAdapter: AttachmentPreviewAdapter<Attachment<Any>>): FlexInputFragment {
    previewAdapter.selectionAggregator
        .initFrom(attachmentPreviewAdapter.selectionAggregator)
    attachmentPreviewAdapter = previewAdapter
    attachmentPreviewList.adapter = attachmentPreviewAdapter
    return this
  }

  fun setFileManager(fileManager: FileManager) = this.apply {
    this.fileManager_ = fileManager
  }

  fun setKeyboardManager(keyboardManager: KeyboardManager?) = this.apply {
    this.keyboardManager = keyboardManager
  }

  /**
   * Set the add content pages. If no page suppliers are specified, the default set of pages is used.
   *
   * @param pageSuppliers ordered list of pages to be shown when the user tried to add content
   */
  fun setContentPages(vararg pageSuppliers: PageSupplier) = this.apply {
    this.pageSuppliers = pageSuppliers.asList().toTypedArray()
  }

  val contentPages: Array<out PageSupplier>
    get() = pageSuppliers?.takeIf { it.isNotEmpty() } ?: createDefaultPages()

  /**
   * Allows overriding the default [AppCompatEditText] to a custom component.
   *
   *
   * Use at your own risk.
   *
   * @param customEditText the custom [AppCompatEditText] which you wish to use instead.
   */
  fun setEditTextComponent(customEditText: AppCompatEditText) = this.apply {
    customEditText.id = R.id.text_input
    customEditText.isFocusable = true
    customEditText.isFocusableInTouchMode = true

    inputContainer.post {
      Log.d(TAG, "Replacing EditText component")

      if (customEditText.text.isNullOrEmpty()) {
        val prevText = textEt.text
        customEditText.text = prevText
        Log.d(TAG, "Replacing EditText component: text copied")
      }

      val editTextIndex = inputContainer.indexOfChild(textEt)
      inputContainer.removeView(textEt)
      inputContainer.addView(customEditText, editTextIndex)

      textEt = customEditText
      val params = when (customEditText.layoutParams) {
        is LinearLayout.LayoutParams -> customEditText.layoutParams as LinearLayout.LayoutParams
        else -> LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
      }
      customEditText.layoutParams = params
      customEditText.requestLayout()

      Log.d(TAG, "Binding EditText hooks")
      bindTextInput(customEditText)
      updateSendBtnEnableState(customEditText.text)
    }
  }

  fun setEnabled(isEnabled: Boolean): FlexInputFragment {
    this.isEnabled = isEnabled
    inputContainer.children.forEach { child ->
      child.isEnabled = isEnabled
    }
    if (isEnabled) {
      updateSendBtnEnableState(textEt.text)
    }
    return this
  }

  //endregion

  fun requestFocus() {
    textEt.requestFocus()
    if (emojiContainer.isVisible) {
      return
    }
    textEt.post { keyboardManager?.requestDisplay(textEt) }
  }

  // region UI Event Handlers
  fun onSend() {
    val shouldClean = inputListener!!.onSend(
        textEt.text, attachmentPreviewAdapter.selectionAggregator.attachments)
    if (shouldClean) {
      textEt.setText("")
      clearAttachments()
    }
  }

  fun clearAttachments() {
    attachmentPreviewAdapter.clear()
    attachmentPreviewContainer.visibility = View.GONE
    updateSendBtnEnableState(textEt.text)
  }

  fun tooltipButton(view: View): Boolean {
    Toast.makeText(context, view.contentDescription, Toast.LENGTH_SHORT).show()
    return true
  }

  fun onTextInputTouch(motionEvent: MotionEvent): Boolean {
    when (motionEvent.action) {
      MotionEvent.ACTION_UP -> hideEmojiTray()
    }
    return false // Passthrough
  }

  fun onEmojiToggle() {
    if (emojiContainer.isVisible) {
      hideEmojiTray()
      keyboardManager?.requestDisplay(textEt)
    } else {
      showEmojiTray()
    }
  }

  fun onAddToggle() {
    hideEmojiTray()
    keyboardManager?.requestHide() // Make sure the keyboard is hidden
    try {
      attachContentDialogFragment()
    } catch (e: Exception) {
      Log.d(TAG, "Could not open AddContentDialogFragment", e)
    }
  }

  private fun attachContentDialogFragment() {
    val ft = childFragmentManager.beginTransaction()
    val dialogFrag = AddContentDialogFragment()
    dialogFrag.show(ft, ADD_CONTENT_FRAG_TAG)
    childFragmentManager.executePendingTransactions()
    dialogFrag.dialog!!.setOnDismissListener(DialogInterface.OnDismissListener {
      if (dialogFrag.isAdded && !dialogFrag.isDetached) {
        dialogFrag.dismissAllowingStateLoss()
      }
      if (!this@FlexInputFragment.isAdded || this@FlexInputFragment.isHidden) {
        return@OnDismissListener  // Nothing to do
      }
      requestFocus()
      updateAttachmentPreviewContainer()
    })
  }

  // endregion
  fun hideEmojiTray(): Boolean {
    val isVisible = emojiContainer.isShown
    if (!isVisible) {
      return false
    }
    emojiContainer.visibility = View.GONE
    emojiBtn.setImageResource(R.drawable.ic_insert_emoticon_24dp)
    onEmojiStateChange(false)
    return true
  }

  fun showEmojiTray() {
    emojiContainer.visibility = View.VISIBLE
    keyboardManager?.requestHide()
    emojiBtn.setImageResource(R.drawable.ic_keyboard_24dp)
    onEmojiStateChange(true)
  }

  protected fun onEmojiStateChange(isActive: Boolean) {
    val fragment = childFragmentManager.findFragmentById(R.id.emoji_container)
    if (fragment != null && fragment is FlexInputEmojiStateChangeListener) {
      (fragment as FlexInputEmojiStateChangeListener).isShown(isActive)
    }
  }

  fun append(data: CharSequence?) {
    textEt.text!!.append(data)
  }

  fun updateSendBtnEnableState(message: Editable?) {
    sendBtn.isEnabled = (isEnabled
        && (!message.isNullOrEmpty() || attachmentPreviewAdapter.itemCount > 0))
  }

  private fun updateAttachmentPreviewContainer() {
    attachmentPreviewContainer.visibility = when {
      attachmentPreviewAdapter.itemCount > 0 -> View.VISIBLE
      else -> View.GONE
    }
  }

  // region FlexInputCoordinator methods
  override fun addExternalAttachment(attachment: Attachment<Any>) {
    // Create a temporary SelectionCoordinator to add attachment
    val coord = SelectionCoordinator<Attachment<Any>, Attachment<Any>>()
    attachmentPreviewAdapter.selectionAggregator.registerSelectionCoordinator(coord)
    coord.selectItem(attachment, 0)
    coord.close()

    lifecycleScope.launchWhenResumed {
      val dialogFragment = childFragmentManager.findFragmentByTag(ADD_CONTENT_FRAG_TAG) as? DialogFragment
      if (dialogFragment != null && dialogFragment.isAdded
          && !dialogFragment.isRemoving && !dialogFragment.isDetached) {
        try {
          dialogFragment.dismiss()
        } catch (ignored: IllegalStateException) {
          Log.w(TAG, "could not dismiss add content dialog", ignored)
        }
      }
    }
  }

  // endregion

  override val selectionAggregator: SelectionAggregator<Attachment<Any>>
    get() = attachmentPreviewAdapter.selectionAggregator

  companion object {
    private val TAG = FlexInputFragment::class.java.name
    const val ADD_CONTENT_FRAG_TAG = "Add Content"
    const val EXTRA_ATTACHMENTS = "FlexInput.ATTACHMENTS"
    const val EXTRA_TEXT = "FlexInput.TEXT"
  }
}