package com.lytefast.flexinput.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
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

import org.jetbrains.annotations.NotNull;

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
import com.lytefast.flexinput.utils.FlexInputEmojiStateChangeListener;
import com.lytefast.flexinput.utils.SelectionAggregator;
import com.lytefast.flexinput.utils.SelectionCoordinator;
import com.lytefast.flexinput.widget.FlexEditText;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;


/**
 * Main widget fragment that controls all aspects of the FlexInput widget.
 * <p>
 * This is the controller which maintains all the interactions between the various components.
 *
 * @author Sam Shih
 */
@SuppressWarnings("UnusedReturnValue")
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
  @SuppressWarnings("FieldCanBeLocal")  // Keep here so we know it's available
  private View addBtn;

  /**
   * Temporarily stores the UI attributes until we can apply them after inflation.
   */
  private Runnable initializeUiAttributes;
  private KeyboardManager keyboardManager;
  private InputListener inputListener;

  protected FileManager fileManager;
  protected AttachmentPreviewAdapter<Attachment<Object>> attachmentPreviewAdapter;
  protected AddContentPagerAdapter.PageSupplier[] pageSuppliers;

  private boolean isEnabled = true;


  //region Lifecycle Methods

  @Override
  public void onInflate(final Context context, final AttributeSet attrs, final Bundle savedInstanceState) {
    super.onInflate(context, attrs, savedInstanceState);

    initializeUiAttributes = new Runnable() {
      @Override
      public void run() {
        final TypedArray attrTypedArray = context.obtainStyledAttributes(attrs, R.styleable.FlexInput);
        try {
          initAttributes(attrTypedArray);
        } finally {
          attrTypedArray.recycle();
        }
      }
    };
    // Set this so we can capture SelectionCoordinators ASAP
    this.attachmentPreviewAdapter = initDefaultAttachmentPreviewAdapter(context);
  }

  private AttachmentPreviewAdapter<Attachment<Object>> initDefaultAttachmentPreviewAdapter(
      @NonNull final Context context) {
    AttachmentPreviewAdapter<Attachment<Object>> adapter =
        new AttachmentPreviewAdapter<>(context.getContentResolver());
    adapter.getSelectionAggregator()
        .addItemSelectionListener(new SelectionCoordinator.ItemSelectionListener<Attachment<Object>>() {
          @Override
          public void onItemSelected(final Attachment<Object> item) {
            updateUi();
          }

          @Override
          public void onItemUnselected(final Attachment<Object> item) {
            updateUi();
          }

          @Override
          public void unregister() {
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
  public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container,
                           @Nullable final Bundle savedInstanceState) {
    LinearLayout root = (LinearLayout) inflater.inflate(
        R.layout.flex_input_widget, container, false);

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
  public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
    if (savedInstanceState != null) {
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
  public void onSaveInstanceState(@NonNull final Bundle outState) {
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

    if (editText instanceof FlexEditText) {
      FlexEditText flexEt = (FlexEditText) editText;
      if (flexEt.getInputContentHandler() == null) {
        // Set a default
        flexEt.setInputContentHandler(new Function1<InputContentInfoCompat, Unit>() {
          @Override
          public Unit invoke(final InputContentInfoCompat inputContentInfoCompat) {
            addExternalAttachment(Attachment.toAttachment(
                inputContentInfoCompat, getContext().getContentResolver(), true, "unknown"));
            return null;
          }
        });
      }
    }
  }

  private void initAttributes(final TypedArray typedArray) {
    final CharSequence hintText = typedArray.getText(R.styleable.FlexInput_hint);
    if (!TextUtils.isEmpty(hintText)) {
      textEt.setHint(hintText);
    }

    if (typedArray.hasValue(R.styleable.FlexInput_hintColor)) {
      @ColorInt final int hintColor = typedArray.getColor(R.styleable.FlexInput_hintColor, Color.LTGRAY);
      textEt.setHintTextColor(hintColor);
    }

    final Drawable backgroundDrawable = typedArray.getDrawable(R.styleable.FlexInput_previewBackground);
    if (backgroundDrawable != null) {
      backgroundDrawable.setCallback(getView());
      attachmentPreviewContainer.setBackground(backgroundDrawable);
    }
  }

  //endregion

  //region Functional Getters/Setters

  /**
   * Set the custom emoji {@link Fragment} for the input.
   * <p>
   * Note that this should only be set once for the life of the containing fragment. Make sure to
   * check the <code>savedInstanceState</code> before creating and saving another fragment.
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
   * Set an {@link RecyclerView.Adapter} implementation that knows how render {@link Attachment}s.
   * If this is not set, no attachment preview will be shown.
   *
   * @param previewAdapter An adapter that knows how to display {@link Attachment}s
   *
   * @return the current instance of {@link FlexInputFragment} for chaining commands
   * @see AttachmentPreviewAdapter#AttachmentPreviewAdapter(ContentResolver) for a default implementation of attachment previews
   */
  public FlexInputFragment setAttachmentPreviewAdapter(@NonNull final AttachmentPreviewAdapter<Attachment<Object>> previewAdapter) {
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
   * <p>
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
        FlexInputFragment.this.textEt = customEditText;

        final LinearLayout.LayoutParams params =
            customEditText.getLayoutParams() instanceof LinearLayout.LayoutParams
                ? (LinearLayout.LayoutParams) customEditText.getLayoutParams()
                : new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        customEditText.setLayoutParams(params);
        customEditText.requestLayout();

        Log.d(TAG, "Binding EditText hooks");
        bindTextInput(customEditText);
        updateSendBtnEnableState(customEditText.getText());
      }
    });
    return this;
  }

  @SuppressWarnings("unused")
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

  @SuppressWarnings("unused")
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

    try {
      attachContentDialogFragment();
    } catch (Exception e) {
      Log.d(TAG, "Could not open AddContentDialogFragment", e);
    }
  }

  private void attachContentDialogFragment() {
    final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
    final AddContentDialogFragment dialogFrag = new AddContentDialogFragment();
    dialogFrag.show(ft, ADD_CONTENT_FRAG_TAG);
    getChildFragmentManager().executePendingTransactions();

    dialogFrag.getDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
      @Override
      public void onDismiss(final DialogInterface dialog) {
        if (dialogFrag.isAdded() && !dialogFrag.isDetached()) {
          dialogFrag.dismissAllowingStateLoss();
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

  @SuppressWarnings("unused")
  public boolean hideEmojiTray() {
    boolean isVisible = emojiContainer.isShown();
    if (!isVisible) {
      return false;
    }
    emojiContainer.setVisibility(View.GONE);
    emojiBtn.setImageResource(R.drawable.ic_insert_emoticon_24dp);
    onEmojiStateChange(false);
    return true;
  }

  public void showEmojiTray() {
    emojiContainer.setVisibility(View.VISIBLE);
    keyboardManager.requestHide();
    emojiBtn.setImageResource(R.drawable.ic_keyboard_24dp);
    onEmojiStateChange(true);
  }

  protected void onEmojiStateChange(boolean isActive) {
    final Fragment fragment = getChildFragmentManager().findFragmentById(R.id.emoji_container);
    if (fragment != null && fragment instanceof FlexInputEmojiStateChangeListener) {
      ((FlexInputEmojiStateChangeListener) fragment).isShown(isActive);
    }
  }

  public void append(CharSequence data) {
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
  @SuppressWarnings("unchecked")
  public void addExternalAttachment(@NotNull final Attachment<?> attachment) {
    final DialogFragment dialogFragment =
        (DialogFragment) getChildFragmentManager().findFragmentByTag(ADD_CONTENT_FRAG_TAG);

    // Create a temporary SelectionCoordinator to add attachment
    SelectionCoordinator<Attachment<Object>, Attachment<Object>> coord = new SelectionCoordinator<>();
    attachmentPreviewAdapter.getSelectionAggregator().registerSelectionCoordinator(coord);
    coord.selectItem((Attachment<Object>) attachment, 0);
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
  @NotNull
  public FileManager getFileManager() {
    return fileManager;
  }

  @Override
  @NonNull
  public SelectionAggregator<Attachment<Object>> getSelectionAggregator() {
    return attachmentPreviewAdapter.getSelectionAggregator();
  }

  // endregion
}
