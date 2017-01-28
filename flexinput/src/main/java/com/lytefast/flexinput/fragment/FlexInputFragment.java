package com.lytefast.flexinput.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.lytefast.flexinput.FlexInputCoordinator;
import com.lytefast.flexinput.InputListener;
import com.lytefast.flexinput.R;
import com.lytefast.flexinput.R2;
import com.lytefast.flexinput.adapters.AddContentPagerAdapter;
import com.lytefast.flexinput.adapters.AttachmentPreviewAdapter;
import com.lytefast.flexinput.managers.FileManager;
import com.lytefast.flexinput.managers.KeyboardManager;
import com.lytefast.flexinput.model.Attachment;
import com.lytefast.flexinput.utils.SelectionCoordinator;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import butterknife.OnTextChanged;
import butterknife.OnTouch;
import butterknife.Unbinder;


/**
 * Main widget fragment that controls all aspects of the FlexInput widget.
 *
 * This is the controller which maintains all the interactions between the various components.
 *
 * @author Sam Shih
 */
public class FlexInputFragment extends Fragment
    implements FlexInputCoordinator {

  @BindView(R2.id.attachment_preview_container) View attachmentPreviewContainer;
  @BindView(R2.id.main_input_container) LinearLayout inputContainer;
  @BindView(R2.id.add_content_container) View addContentContainer;
  @BindView(R2.id.emoji_container) View emojiContainer;

  @BindView(R2.id.attachment_preview_list) RecyclerView attachmentPreviewList;

  @BindView(R2.id.text_input) AppCompatEditText textEt;
  @BindView(R2.id.emoji_btn) AppCompatImageButton emojiBtn;
  @BindView(R2.id.send_btn) AppCompatImageButton sendBtn;
  @BindView(R2.id.add_content_pager) ViewPager addContentPager;
  @BindView(R2.id.add_content_tabs) TabLayout addContentTabs;

  private Unbinder unbinder;

  /**
   * Temporarily stores the UI attributes until we can apply them after inflation.
   */
  private Runnable initializeUiAttributes;
  private KeyboardManager keyboardManager;
  private InputListener inputListener;

  private FileManager fileManager;
  private AttachmentPreviewAdapter attachmentPreviewAdapter;
  private final ArrayList<SelectionCoordinator<?>> selectionCoordinators = new ArrayList<>(4);


  public FlexInputFragment() {}


  //region Initialization Methods
  @Override
  public void onInflate(final Context context, final AttributeSet attrs, final Bundle savedInstanceState) {
    super.onInflate(context, attrs, savedInstanceState);

    initializeUiAttributes = new Runnable() {
      @Override
      public void run() {
        initAttributes(attrs);
      }
    };
  }

  @Nullable
  @Override
  public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.flex_input_widget, container, false);
    this.unbinder = ButterKnife.bind(this, root);

    if (getChildFragmentManager().findFragmentById(R.id.emoji_container) != null) {
      this.emojiBtn.setVisibility(View.VISIBLE);
    }

    this.initializeUiAttributes.run();
    this.initializeUiAttributes = null;

    setAttachmentPreviewAdapter(new AttachmentPreviewAdapter(getContext().getContentResolver()));
    return root;
  }

  @Override
  public void onDestroyView() {
    this.unbinder.unbind();
    super.onDestroyView();
  }

  private void initAttributes(final AttributeSet attrs) {
    final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.FlexInput);

    try {
      final CharSequence hintText = a.getText(R.styleable.FlexInput_hint);
      if (!TextUtils.isEmpty(hintText)) {
        textEt.setHint(hintText);
      }

      if (a.hasValue(R.styleable.FlexInput_hintColor)) {
        @ColorInt final int hintColor = a.getColor(R.styleable.FlexInput_hintColor, Color.LTGRAY);
        textEt.setHintTextColor(hintColor);
      }

      if (a.hasValue(R.styleable.FlexInput_inputBackground)) {
        Drawable backgroundDrawable = a.getDrawable(R.styleable.FlexInput_inputBackground);
        backgroundDrawable.setCallback(getView());
        inputContainer.setBackground(backgroundDrawable);
      }

      if (a.hasValue(R.styleable.FlexInput_previewBackground)) {
        Drawable backgroundDrawable = a.getDrawable(R.styleable.FlexInput_previewBackground);
        backgroundDrawable.setCallback(getView());
        attachmentPreviewContainer.setBackground(backgroundDrawable);
      }

      if (a.hasValue(R.styleable.FlexInput_tabsBackground)) {
        Drawable backgroundDrawable = a.getDrawable(R.styleable.FlexInput_tabsBackground);
        backgroundDrawable.setCallback(getView());
        addContentTabs.setBackground(backgroundDrawable);
      }
    } finally {
      a.recycle();
    }
  }
  //endregion

  //region Functional Setters
  /**
   * Set the custom emoji {@link Fragment} for the input.
   *
   * Note that this should only be set once for the life of the containing fragment. Make sure to
   * check the <code>savedInstanceState</code> before creating and saving another fragment.
   *
   * @return
   */
  public FlexInputFragment setEmojiFragment(final Fragment emojiFragment) {
    getChildFragmentManager()
        .beginTransaction()
        .replace(R.id.emoji_container, emojiFragment)
        .commit();

    emojiBtn.setVisibility(View.VISIBLE);
    return this;
  }

  public FlexInputFragment setInputListener(@NonNull final InputListener inputListener) {
    this.inputListener = inputListener;
    return this;
  }

  /**
   * Set an {@link android.support.v7.widget.RecyclerView.Adapter} implementation that knows how render {@link Attachment}s.
   * If this is not set, no attachment preview will be shown.
   *
   * @param previewAdapter An adapter that knows how to display {@link Attachment}s
   *
   * @return the current instance of {@link FlexInputFragment} for chaining commands
   *
   * @see AttachmentPreviewAdapter#AttachmentPreviewAdapter(ContentResolver) for a default implementation of attachment previews
   */
  public FlexInputFragment setAttachmentPreviewAdapter(@NonNull final AttachmentPreviewAdapter previewAdapter) {
    this.attachmentPreviewAdapter = previewAdapter;
    this.attachmentPreviewList.setAdapter(attachmentPreviewAdapter);
    return this;
  }

  public FlexInputFragment setFileManager(@NonNull final FileManager fileManager) {
    this.fileManager = fileManager;
    return this;
  }
  //endregion

  public FlexInputFragment setKeyboardManager(KeyboardManager keyboardManager) {
    this.keyboardManager = keyboardManager;
    return this;
  }

  public FlexInputFragment initContentPages(AddContentPagerAdapter.PageSupplier... pageSuppliers) {
    return initContentPages(new AddContentPagerAdapter(getChildFragmentManager(), pageSuppliers));
  }

  public FlexInputFragment initContentPages(final AddContentPagerAdapter pagerAdapter) {
    addContentPager.setAdapter(pagerAdapter);
    pagerAdapter.initTabs(getContext(), addContentTabs);
    synchronizeTabAndPagerEvents();
    return this;
  }

  public void synchronizeTabAndPagerEvents() {
    addContentTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
      /**
       * Special cases the first item (keyboard) by closing the pager and opening the keyboard on click.
       */
      @Override
      public void onTabSelected(final TabLayout.Tab tab) {
        int tabPosition = tab.getPosition();
        if (tabPosition == 0) {
          onAddToggle();
          return;
        }
        addContentPager.setCurrentItem(tabPosition - 1);
      }

      @Override
      public void onTabUnselected(final TabLayout.Tab tab) { }

      @Override
      public void onTabReselected(final TabLayout.Tab tab) { }
    });

    addContentPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) { }

      @Override
      public void onPageSelected(final int position) {
        addContentTabs.getTabAt(position + 1).select();
      }

      @Override
      public void onPageScrollStateChanged(final int state) { }
    });
  }

  /**
   * Allows overriding the default {@link AppCompatEditText} to a custom component.
   *
   * Use at your own risk.
   *
   * @param customEditText the custom {@link AppCompatEditText} which you wish to use instead.
   */
  public FlexInputFragment setEditTextComponent(final AppCompatEditText customEditText) {
    customEditText.setId(R.id.text_input);

    inputContainer.post(new Runnable() {
      @Override
      public void run() {
        final int editTextIndex = inputContainer.indexOfChild(textEt);
        inputContainer.removeView(textEt);
        inputContainer.addView(customEditText, editTextIndex);

        customEditText.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        customEditText.requestLayout();

        // Rebind Butterknife to make sure hooks work
        unbinder.unbind();
        unbinder = ButterKnife.bind(FlexInputFragment.this, getView());

        updateSendBtnEnableState(customEditText.getText());
      }
    });
    return this;
  }

  public void requestFocus() {
    getView().post(new Runnable() {
      @Override
      public void run() {
        textEt.requestFocus();
      }
    });
  }

  // region UI Event Handlers

  @OnClick(R2.id.send_btn)
  void onSend() {
    inputListener.onSend(textEt.getText(), attachmentPreviewAdapter.getAttachments());
    textEt.setText("");
    clearAttachments();
  }

  @OnClick(R2.id.attachment_clear_btn)
  void clearAttachments() {
    attachmentPreviewAdapter.clear();
    attachmentPreviewContainer.setVisibility(View.GONE);

    for (SelectionCoordinator<?> coordinators : selectionCoordinators) {
      coordinators.clearSelectedItems();
    }
    updateSendBtnEnableState(textEt.getText());
  }

  @OnLongClick(R2.id.add_btn)
  boolean tooltipHandlerAddContent() {
    Toast.makeText(getContext(), R.string.add_content, Toast.LENGTH_SHORT).show();
    return true;
  }

  @OnLongClick(R2.id.attachment_clear_btn)
  boolean tooltipHandlerAttachmentClear() {
    Toast.makeText(getContext(), R.string.clear_attachments, Toast.LENGTH_SHORT).show();
    return true;
  }

  @OnLongClick(R2.id.emoji_btn)
  boolean tooltipHandlerEmoji() {
    Toast.makeText(getContext(), R.string.emoji_keyboard_toggle, Toast.LENGTH_SHORT).show();
    return true;
  }

  @OnLongClick(R2.id.send_btn)
  boolean tooltipHandlerSend() {
    Toast.makeText(getContext(), R.string.send_contents, Toast.LENGTH_SHORT).show();
    return true;
  }

  @OnTouch(R2.id.text_input)
  boolean onTextInputTouch(MotionEvent motionEvent) {
    switch (motionEvent.getAction()) {
      case MotionEvent.ACTION_UP:
        hideEmojiTray();
        keyboardManager.requestDisplay();
        break;
    }

    return false;  // Passthrough
  }

  @OnClick(R2.id.emoji_btn)
  void onEmojiToggle() {
    if (emojiContainer.getVisibility() == View.VISIBLE) {
      hideEmojiTray();
      keyboardManager.requestDisplay();
    } else {
      showEmojiTray();
    }

    addContentPager.setVisibility(View.GONE);
  }

  @OnClick(R2.id.add_btn)
  void onAddToggle() {
    hideEmojiTray();
    if (addContentContainer.isShown()) {
      addContentContainer.setVisibility(View.GONE);
      addContentPager.setVisibility(View.GONE);  // set this to force destroy fragments

      inputContainer.setVisibility(View.VISIBLE);
      keyboardManager.requestDisplay();
    } else {
      addContentContainer.setVisibility(View.VISIBLE);
      addContentPager.setVisibility(View.VISIBLE);
      addContentTabs.getTabAt(1).select(); // TODO: remember last saved tab selection

      inputContainer.setVisibility(View.GONE);
      keyboardManager.requestHide();  // Make sure the keyboard is hidden
    }

    updateAttachmentPreviewContainer();
  }

  @OnTextChanged(value = R2.id.text_input, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
  void onTextChanged(Editable after) {
    updateSendBtnEnableState(after);
  }

  // endregion

  private void hideEmojiTray() {
    emojiContainer.setVisibility(View.GONE);
    emojiBtn.setImageResource(R.drawable.ic_insert_emoticon_24dp);
  }

  private void showEmojiTray() {
    emojiContainer.setVisibility(View.VISIBLE);
    keyboardManager.requestHide();
    emojiBtn.setImageResource(R.drawable.ic_keyboard_24dp);
  }

  public void append(CharSequence data) {
    // TODO figure out some way to allow custom spannables (e.g. fresco's DraweeSpan)
    textEt.getText().append(data);
  }

  public void handleAttachmentClick(Attachment item) {
    attachmentPreviewAdapter.toggleItem(item);

    updateSendBtnEnableState(textEt.getText());
    updateAttachmentPreviewContainer();
  }

  private void updateSendBtnEnableState(final Editable message) {
    sendBtn.setEnabled(message.length() > 0 || attachmentPreviewAdapter.getItemCount() > 0);
  }

  private void updateAttachmentPreviewContainer() {
    int shouldShow =
        attachmentPreviewAdapter.getItemCount() == 0 || addContentContainer.isShown()
        ? View.GONE : View.VISIBLE;

    attachmentPreviewContainer.setVisibility(
        shouldShow);
  }

  // region FlexInputCoordinator methods

  @Override
  public <T extends Attachment> void onPhotoTaken(final T photo) {
    getView().post(new Runnable() {
      @Override
      public void run() {
        onAddToggle();
        handleAttachmentClick(photo);
        // TODO invalidate photo picker
      }
    });
  }

  @Override
  public FileManager getFileManager() {
    return fileManager;
  }

  @Override
  public <T extends Attachment> void addSelectionCoordinator(SelectionCoordinator<T> coordinator) {
    this.selectionCoordinators.add(coordinator);
    coordinator.setItemSelectionListener(new SelectionCoordinator.ItemSelectionListener<T>() {
      @Override
      public void onItemSelected(T item) {
        handleAttachmentClick(item);
      }

      @Override
      public void onItemUnselected(T item) {
        handleAttachmentClick(item);
      }
    });
  }

  // endregion
}
