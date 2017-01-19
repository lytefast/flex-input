package com.lytefast.flexinput.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.lytefast.flexinput.InputListener;
import com.lytefast.flexinput.R;
import com.lytefast.flexinput.R2;
import com.lytefast.flexinput.adapters.AttachmentPreviewAdapter;
import com.lytefast.flexinput.FlexInputCoordinator;
import com.lytefast.flexinput.managers.FileManager;
import com.lytefast.flexinput.managers.KeyboardManager;
import com.lytefast.flexinput.model.Attachment;
import com.lytefast.flexinput.utils.SelectionCoordinator;
import com.lytefast.flexinput.utils.WidgetUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import butterknife.OnTouch;
import butterknife.Unbinder;


/**
 * @author Sam Shih
 */
public class FlexInputFragment extends Fragment implements FlexInputCoordinator {

  public static final int TAB_PHOTOS = 1;
  public static final int TAB_FILES = 0;
  public static final int TAB_CAMERA = 2;

  @BindView(R2.id.attachment_preview_container) View attachmentPreviewContainer;
  @BindView(R2.id.main_input_container) View inputContainer;
  @BindView(R2.id.add_content_container) View addContentContainer;
  @BindView(R2.id.emoji_container) View emojiContainer;

  @BindView(R2.id.attachment_preview_list)
  RecyclerView attachmentPreviewList;

  @BindView(R2.id.text_input) AppCompatEditText textEt;
  @BindView(R2.id.emoji_btn) AppCompatImageButton emojiBtn;
  @BindView(R2.id.add_content_pager) ViewPager addContentPager;
  @BindView(R2.id.add_content_tabs) TabLayout addContentTabs;

    /**
   * Temporarily stores the UI attributes until we can apply them after inflation.
   */
  private Runnable initializeUiAttributes;


  private KeyboardManager keyboardManager;
  private InputListener inputListener;

  private FileManager fileManager;
  private AttachmentPreviewAdapter attachmentPreviewAdapter;
  private final ArrayList<SelectionCoordinator<?>> selectionCoordinators = new ArrayList<>(4);
  private Unbinder unbinder;


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

    this.initializeUiAttributes.run();
    this.initializeUiAttributes = null;

    initContentPages();
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

  /**
   * Set the custom emoji {@link Fragment} for the input.
   *
   * Note that this should only be set once for the life of the containing fragment. Make sure to
   * check the <code>savedInstanceState</code> before creating and saving another fragment.
   *
   * @return
   */
  //region Functional Setters
  public FlexInputFragment setEmojiFragment(final Fragment emojiFragment) {
    getChildFragmentManager()
        .beginTransaction()
        .replace(R.id.emoji_container, emojiFragment)
        .commit();
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

  public FlexInputFragment initContentPages() {
    return initContentPages(new FragmentPagerAdapter(getChildFragmentManager()) {
      @Override
      public Fragment getItem(final int position) {
        switch (position) {
          default:
            return null;
          case TAB_FILES:
            return new FilesFragment();
          case TAB_PHOTOS:
            return new PhotosFragment();
          case TAB_CAMERA:
            return new CameraFragment();
        }
      }

      @Override
      public int getCount() {
        return 3;
      }
    });
  }

  public FlexInputFragment initContentPages(final FragmentPagerAdapter pagerAdapter) {
    addContentPager.setAdapter(pagerAdapter);
    synchronizeTabAndPagerEvents();
    initIconColors();
    return this;
  }

  public void synchronizeTabAndPagerEvents() {
    addContentTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
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

   private void initIconColors() {
    ColorStateList iconColors = WidgetUtils.getColorStateList(getContext(),
                                                              R.color.tab_icon_color_selector);

    for (int i = 0; i < addContentTabs.getTabCount(); i++) {
      TabLayout.Tab tab = addContentTabs.getTabAt(i);

      Drawable icon = tab.getIcon();
      if (icon != null) {
        icon = DrawableCompat.wrap(icon);
        DrawableCompat.setTintList(icon, iconColors);
      }
    }
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
    if (textEt.length() == 0) {
      return;  // Nothing to do here
    }

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
    if (addContentContainer.getVisibility() == View.VISIBLE) {
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

  @Override
  public void onAttachFragment(final Fragment childFragment) {
    super.onAttachFragment(childFragment);
  }


  public void append(CharSequence data) {
    // TODO figure out some way to allow custom spannables (e.g. fresco's DraweeSpan)
    textEt.getText().append(data);
  }

  public void handleAttachmentClick(Attachment item) {
    attachmentPreviewAdapter.toggleItem(item);
    attachmentPreviewContainer.setVisibility(
        attachmentPreviewAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
  }

  // region FlexInputController methods

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
