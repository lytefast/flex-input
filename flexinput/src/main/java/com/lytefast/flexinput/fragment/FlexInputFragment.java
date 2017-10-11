package com.lytefast.flexinput.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import com.lytefast.flexinput.FlexInputCoordinator;
import com.lytefast.flexinput.InputListener;
import com.lytefast.flexinput.R;
import com.lytefast.flexinput.adapters.AddContentPagerAdapter;
import com.lytefast.flexinput.adapters.AttachmentPreviewAdapter;
import com.lytefast.flexinput.managers.FileManager;
import com.lytefast.flexinput.managers.KeyboardManager;
import com.lytefast.flexinput.model.Attachment;
import com.lytefast.flexinput.utils.SelectionAggregator;
import com.lytefast.flexinput.utils.SelectionCoordinator;


/**
 * Main widget fragment that controls all aspects of the FlexInput widget.
 *
 * This is the controller which maintains all the interactions between the various components.
 *
 * @author Sam Shih
 */
public class FlexInputFragment extends Fragment
    implements FlexInputCoordinator<Object> {

  private static final String TAG = FlexInputFragment.class.getName();

  public static final String ADD_CONTENT_FRAG_TAG = "Add Content";
  public static final String EXTRA_ATTACHMENTS = "FlexInput.ATTACHMENTS";
  public static final String EXTRA_TEXT = "FlexInput.TEXT";

  private View attachmentPreviewContainer;
  private View attachmentClearButton;
  private LinearLayout inputContainer;
  private View emojiContainer;

  private RecyclerView attachmentPreviewList;

  private AppCompatEditText textEt;
  private AppCompatImageButton emojiBtn;
  private AppCompatImageButton sendBtn;
  private View addBtn;

  /**
   * Temporarily stores the UI attributes until we can apply them after inflation.
   */
  private Runnable initializeUiAttributes;
  private KeyboardManager keyboardManager;
  private InputListener inputListener;

  protected FileManager fileManager;
  protected AttachmentPreviewAdapter attachmentPreviewAdapter;
  protected AddContentPagerAdapter.PageSupplier[] pageSuppliers;

  private boolean isEnabled = true;


  public FlexInputFragment() {}

  //region Lifecycle Methods

  @Override
  public void onInflate(final Context context, final AttributeSet attrs, final Bundle savedInstanceState) {
    super.onInflate(context, attrs, savedInstanceState);

    initializeUiAttributes = new Runnable() {
      @Override
      public void run() {
        initAttributes(attrs);
      }
    };
    // Set this so we can capture SelectionCoordinators ASAP
    this.attachmentPreviewAdapter = initDefaultAttachmentPreviewAdapter();
  }

  private AttachmentPreviewAdapter initDefaultAttachmentPreviewAdapter() {
    AttachmentPreviewAdapter adapter = new AttachmentPreviewAdapter(getContext().getContentResolver());
    adapter.getSelectionAggregator()
        .addItemSelectionListener(new SelectionCoordinator.ItemSelectionListener() {
          @Override
          public void onItemSelected(final Object item) {
            updateUi();
          }

          @Override
          public void onItemUnselected(final Object item) {
            updateUi();
          }

          private void updateUi() {
            final View rootView = getView();
            if (rootView == null) {
              return;
            }
            rootView.post(new Runnable() {
              @Override
              public void run() {
                if (textEt != null) {
                  updateSendBtnEnableState(textEt.getText());
                }
                if (attachmentPreviewContainer != null) {
                  updateAttachmentPreviewContainer();
                }
              }
            });
          }
        });
    return adapter;
  }

  @Nullable
  @Override
  public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
                           @Nullable final Bundle savedInstanceState) {
    LinearLayout root = (LinearLayout) inflater.inflate(R.layout.flex_input_widget, container, false);

    attachmentPreviewContainer = root.findViewById(R.id.attachment_preview_container);
    attachmentClearButton = root.findViewById(R.id.attachment_clear_btn);
    attachmentClearButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        clearAttachments();
      }
    });
    inputContainer = root.findViewById(R.id.main_input_container);
    emojiContainer = root.findViewById(R.id.emoji_container);
    attachmentPreviewList = root.findViewById(R.id.attachment_preview_list);

    textEt = root.findViewById(R.id.text_input);
    bindTextInput(textEt);
    bindButtons(root);

    this.initializeUiAttributes.run();
    this.initializeUiAttributes = null;
    setAttachmentPreviewAdapter(new AttachmentPreviewAdapter(getContext().getContentResolver()));
    return root;
  }

  @Override
  public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
    if (savedInstanceState != null ) {
      ArrayList<Parcelable> savedAttachments =
          savedInstanceState.getParcelableArrayList(EXTRA_ATTACHMENTS);
      if (savedAttachments != null && savedAttachments.size() > 0) {
        attachmentPreviewAdapter.getSelectionAggregator().initFrom(savedAttachments);
      }

      String text = savedInstanceState.getString(EXTRA_TEXT);
      setText(text);
    }
  }

  public void setText(String text) {
    textEt.setText(text);
    if (!TextUtils.isEmpty(text)) {
      textEt.setSelection(text.length());
    }
  }

  @Override
  public void onSaveInstanceState(final Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelableArrayList(
        EXTRA_ATTACHMENTS, attachmentPreviewAdapter.getSelectionAggregator().getAttachments());
    outState.putString(EXTRA_TEXT, textEt.getText().toString());
  }

  @Override
  public void onPause() {
    hideEmojiTray();
    keyboardManager.requestHide();
    super.onPause();
  }

  private void bindButtons(final View root) {
    emojiBtn = root.findViewById(R.id.emoji_btn);
    emojiBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onEmojiToggle();
      }
    });
    sendBtn = root.findViewById(R.id.send_btn);
    sendBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onSend();
      }
    });
    addBtn = root.findViewById(R.id.add_btn);
    addBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onAddToggle();
      }
    });

    for (final View view : Arrays.asList(attachmentClearButton, addBtn, emojiBtn, sendBtn)) {
      view.setOnLongClickListener(new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
          return tooltipButton(view);
        }
      });
    }

    if (getChildFragmentManager().findFragmentById(R.id.emoji_container) != null) {
      this.emojiBtn.setVisibility(View.VISIBLE);
    }
  }

  private void bindTextInput(final AppCompatEditText editText) {
    editText.addTextChangedListener(new TextWatcher() {

      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
      }

      @Override
      public void afterTextChanged(Editable editable) {
        updateSendBtnEnableState(editable);
      }
    });
    editText.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        return onTextInputTouch(motionEvent);
      }
    });
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

      if (a.hasValue(R.styleable.FlexInput_previewBackground)) {
        Drawable backgroundDrawable = a.getDrawable(R.styleable.FlexInput_previewBackground);
        backgroundDrawable.setCallback(getView());
        attachmentPreviewContainer.setBackground(backgroundDrawable);
      }
    } finally {
      a.recycle();
    }
  }

  //endregion

  //region Functional Getters/Setters
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
    previewAdapter.getSelectionAggregator()
        .initFrom(attachmentPreviewAdapter.getSelectionAggregator());

    this.attachmentPreviewAdapter = previewAdapter;
    this.attachmentPreviewList.setAdapter(attachmentPreviewAdapter);
    return this;
  }

  public FlexInputFragment setFileManager(@NonNull final FileManager fileManager) {
    this.fileManager = fileManager;
    return this;
  }

  public FlexInputFragment setKeyboardManager(KeyboardManager keyboardManager) {
    this.keyboardManager = keyboardManager;
    return this;
  }


  /**
   * Set the add content pages. If no page suppliers are specified, the default set of pages is used.
   *
   * @param pageSuppliers ordered list of pages to be shown when the user tried to add content
   */
  public FlexInputFragment setContentPages(AddContentPagerAdapter.PageSupplier... pageSuppliers) {
    this.pageSuppliers = pageSuppliers;
    return this;
  }

  public AddContentPagerAdapter.PageSupplier[] getContentPages() {
    return pageSuppliers == null || pageSuppliers.length == 0 ?
        AddContentPagerAdapter.Companion.createDefaultPages() : pageSuppliers;
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
    customEditText.setFocusable(true);
    customEditText.setFocusableInTouchMode(true);

    inputContainer.post(new Runnable() {
      @Override
      public void run() {
        if (inputContainer == null) {
          return;  // This can happen if the user just exits immediately or the fragment resets.
        }

        Log.d(TAG, "Replacing EditText component");
        if (customEditText.getText().length() == 0) {
          final Editable prevText = textEt.getText();
          customEditText.setText(prevText);
          Log.d(TAG, "Replacing EditText component: text copied");
        }
        final int editTextIndex = inputContainer.indexOfChild(textEt);
        inputContainer.removeView(textEt);
        inputContainer.addView(customEditText, editTextIndex);

        customEditText.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        customEditText.requestLayout();

        Log.d(TAG, "Binding EditText hooks");
        bindTextInput(customEditText);
        updateSendBtnEnableState(customEditText.getText());
      }
    });
    return this;
  }

  public FlexInputFragment setEnabled(final boolean isEnabled) {
    this.isEnabled = isEnabled;

    for (int i = 0; i < inputContainer.getChildCount(); i++) {
      View child = inputContainer.getChildAt(i);
      child.setEnabled(isEnabled);
    }

    if (isEnabled) {
      updateSendBtnEnableState(textEt.getText());
    }
    return this;
  }

  public boolean isEnabled() {
    return this.isEnabled;
  }

  //endregion

  public void requestFocus() {
    textEt.requestFocus();
    if (emojiContainer.getVisibility() == View.VISIBLE) {
      return;
    }

    textEt.post(new Runnable() {
      @Override
      public void run() {
        keyboardManager.requestDisplay(textEt);
      }
    });
  }

  // region UI Event Handlers

  public void onSend() {
    boolean shouldClean = inputListener.onSend(
        textEt.getText(), attachmentPreviewAdapter.getSelectionAggregator().getAttachments());

    if (shouldClean) {
      textEt.setText("");
      clearAttachments();
    }
  }

  public void clearAttachments() {
    attachmentPreviewAdapter.clear();
    attachmentPreviewContainer.setVisibility(View.GONE);

    updateSendBtnEnableState(textEt.getText());
  }

  boolean tooltipButton(View view) {
    Toast.makeText(getContext(), view.getContentDescription(), Toast.LENGTH_SHORT).show();
    return true;
  }

  boolean onTextInputTouch(MotionEvent motionEvent) {
    switch (motionEvent.getAction()) {
      case MotionEvent.ACTION_UP:
        hideEmojiTray();
        break;
    }

    return false;  // Passthrough
  }

  public void onEmojiToggle() {
    if (emojiContainer.getVisibility() == View.VISIBLE) {
      hideEmojiTray();
      keyboardManager.requestDisplay(textEt);
    } else {
      showEmojiTray();
    }
  }

  void onAddToggle() {
    hideEmojiTray();
    keyboardManager.requestHide();  // Make sure the keyboard is hidden

    new Handler().post(new Runnable() {
      @Override
      public void run() {
        attachContentDialogFragment();
      }
    });
  }

  private void attachContentDialogFragment() {
    FragmentTransaction ft = getChildFragmentManager().beginTransaction();
    final AddContentDialogFragment dialogFrag = new AddContentDialogFragment();
    dialogFrag.show(ft, ADD_CONTENT_FRAG_TAG);
    getChildFragmentManager().executePendingTransactions();

    dialogFrag.getDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
      @Override
      public void onDismiss(final DialogInterface dialog) {
        if (dialogFrag.isAdded() && !dialogFrag.isDetached()) {
          dialogFrag.dismiss();
        }
        if (!FlexInputFragment.this.isAdded() || FlexInputFragment.this.isHidden()) {
          return;  // Nothing to do
        }
        requestFocus();
        updateAttachmentPreviewContainer();
      }
    });
  }

  // endregion

  public boolean hideEmojiTray() {
    boolean isVisible = emojiContainer.isShown();
    if (!isVisible) {
      return false;
    }
    emojiContainer.setVisibility(View.GONE);
    emojiBtn.setImageResource(R.drawable.ic_insert_emoticon_24dp);
    return true;
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

  public void updateSendBtnEnableState(final Editable message) {
    sendBtn.setEnabled(isEnabled
        && (message.length() > 0 || attachmentPreviewAdapter.getItemCount() > 0));
  }

  private void updateAttachmentPreviewContainer() {
    attachmentPreviewContainer.setVisibility(
        attachmentPreviewAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
  }

  // region FlexInputCoordinator methods

  @Override
  public void addExternalAttachment(final Attachment attachment) {
    final DialogFragment dialogFragment =
        (DialogFragment) getChildFragmentManager().findFragmentByTag(ADD_CONTENT_FRAG_TAG);

    // Create a temporary SelectionCoordinator to add attachment
    SelectionCoordinator<Object, Attachment> coord = new SelectionCoordinator<>();
    attachmentPreviewAdapter.getSelectionAggregator().registerSelectionCoordinator(coord);
    coord.selectItem(attachment, 0);
    coord.close();

    attachmentPreviewList.post(new Runnable() {
      @Override
      public void run() {
        if (dialogFragment != null && dialogFragment.isAdded()
            && !dialogFragment.isRemoving() && !dialogFragment.isDetached()) {
          try {
            dialogFragment.dismiss();
          } catch (IllegalStateException ignored) {
            Log.w(TAG, "could not dismiss add content dialog", ignored);
          }
        }
      }
    });
  }


  @Override
  public FileManager getFileManager() {
    return fileManager;
  }

  @Override
  public SelectionAggregator<Attachment<Object>> getSelectionAggregator() {
    return attachmentPreviewAdapter.getSelectionAggregator();
  }

  // endregion
}
