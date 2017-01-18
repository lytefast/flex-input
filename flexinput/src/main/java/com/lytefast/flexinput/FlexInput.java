package com.lytefast.flexinput;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.lytefast.flexinput.adapters.AttachmentPreviewAdapter;
import com.lytefast.flexinput.emoji.Emoji;
import com.lytefast.flexinput.events.ClearAttachmentsEvent;
import com.lytefast.flexinput.events.ItemClickedEvent;
import com.lytefast.flexinput.fragment.CameraFragment;
import com.lytefast.flexinput.fragment.CameraFragment.PhotoTakenCallback;
import com.lytefast.flexinput.fragment.FilesFragment;
import com.lytefast.flexinput.fragment.PhotosFragment;
import com.lytefast.flexinput.managers.FileManager;
import com.lytefast.flexinput.managers.KeyboardManager;
import com.lytefast.flexinput.model.Attachment;
import com.lytefast.flexinput.utils.FileUtils;
import com.lytefast.flexinput.utils.WidgetUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import butterknife.OnTouch;


/**
 * Text, emoji, and media input field.
 *
 * @author Sam Shih
 */
public class FlexInput extends RelativeLayout {
  public static final int TAB_PHOTOS = 1;
  public static final int TAB_FILES = 0;
  public static final int TAB_CAMERA = 2;

  @BindView(R2.id.attachment_preview_container) View attachmentPreviewContainer;
  @BindView(R2.id.main_input_container) View inputContainer;
  @BindView(R2.id.add_content_container) View addContentContainer;
  @BindView(R2.id.emoji_container) View emojiContainer;

  @BindView(R2.id.attachment_preview_list) RecyclerView attachmentPreviewList;

  @BindView(R2.id.text_input) AppCompatEditText textEt;
  @BindView(R2.id.emoji_btn) AppCompatImageButton emojiBtn;
  @BindView(R2.id.add_content_pager) ViewPager addContentPager;
  @BindView(R2.id.add_content_tabs) TabLayout addContentTabs;

  private KeyboardManager keyboardManager;
  private InputListener inputListener;

  private FileManager fileManager;
  private AttachmentPreviewAdapter attachmentPreviewAdapter;


  public FlexInput(Context context) {
    super(context);
    init(null, 0);
  }

  public FlexInput(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs, 0);
  }

  public FlexInput(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(attrs, defStyle);
  }

  //region Initialization Methods
  private void init(AttributeSet attrs, int defStyle) {
    inflate(getContext(), R.layout.flex_input_widget, this);
    ButterKnife.bind(this);

    setFocusable(true);
    setFocusableInTouchMode(true);

    initAttributes(attrs, defStyle);
  }

  private void initAttributes(final AttributeSet attrs, final int defStyle) {
    final TypedArray a = getContext().obtainStyledAttributes(
        attrs, R.styleable.FlexInput, defStyle, 0);

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
        backgroundDrawable.setCallback(this);
        inputContainer.setBackground(backgroundDrawable);
      }

      if (a.hasValue(R.styleable.FlexInput_previewBackground)) {
        Drawable backgroundDrawable = a.getDrawable(R.styleable.FlexInput_previewBackground);
        backgroundDrawable.setCallback(this);
        attachmentPreviewContainer.setBackground(backgroundDrawable);
      }

      if (a.hasValue(R.styleable.FlexInput_tabsBackground)) {
        Drawable backgroundDrawable = a.getDrawable(R.styleable.FlexInput_tabsBackground);
        backgroundDrawable.setCallback(this);
        addContentTabs.setBackground(backgroundDrawable);
      }
    } finally {
      a.recycle();
    }
  }
  //endregion

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    EventBus.getDefault().register(this);
  }

  @Override
  protected void onDetachedFromWindow() {
    EventBus.getDefault().unregister(this);
    super.onDetachedFromWindow();
  }

  /**
   * Set the custom emoji {@link Fragment} for the input.
   *
   * Note that this should only be set once for the life of the containing fragment. Make sure to
   * check the <code>savedInstanceState</code> before creating and saving another fragment.
   *
   * @return
   */
  //region Functional Setters
  public FlexInput setEmojiFragment(
      final FragmentManager childFragmentManager, final Fragment emojiFragment) {
    childFragmentManager
        .beginTransaction()
        .replace(R.id.emoji_container, emojiFragment)
        .commit();
    return this;
  }

  public FlexInput setInputListener(@NonNull final InputListener inputListener) {
    this.inputListener = inputListener;
    return this;
  }

  /**
   * Set an {@link android.support.v7.widget.RecyclerView.Adapter} implementation that knows how render {@link Attachment}s.
   * If this is not set, no attachment preview will be shown.
   *
   * @param previewAdapter An adapter that knows how to display {@link Attachment}s
   *
   * @return the current instance of {@link FlexInput} for chaining commands
   *
   * @see AttachmentPreviewAdapter#AttachmentPreviewAdapter(ContentResolver) for a default implementation of attachment previews
   */
  public FlexInput setAttachmentPreviewAdapter(@NonNull final AttachmentPreviewAdapter previewAdapter) {
    this.attachmentPreviewAdapter = previewAdapter;
    this.attachmentPreviewList.setAdapter(attachmentPreviewAdapter);
    return this;
  }

  public FlexInput setFileManager(@NonNull final FileManager fileManager) {
    this.fileManager = fileManager;
    return this;
  }
  //endregion

  public FlexInput setKeyboardManager(KeyboardManager keyboardManager) {
    this.keyboardManager = keyboardManager;
    return this;
  }

  public FlexInput initContentPages(final FragmentManager fragmentManager) {
    return initContentPages(new FragmentPagerAdapter(fragmentManager) {
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
            return new CameraFragment()
                .setFileManager(fileManager)
                .setPhotoTakenCallback(cameraPhotoTakenCallback);
        }
      }

      @Override
      public int getCount() {
        return 3;
      }
    });
  }

  public FlexInput initContentPages(final FragmentPagerAdapter pagerAdapter) {
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
    EventBus.getDefault().post(new ClearAttachmentsEvent());
    attachmentPreviewAdapter.clear();
    attachmentPreviewContainer.setVisibility(GONE);
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
    if (emojiContainer.getVisibility() == VISIBLE) {
      hideEmojiTray();
      keyboardManager.requestDisplay();
    } else {
      showEmojiTray();
    }

    addContentPager.setVisibility(GONE);
  }

  @Override
  public boolean requestFocus(final int direction, final Rect previouslyFocusedRect) {
    boolean succeeded = super.requestFocus(direction, previouslyFocusedRect);
    post(new Runnable() {
      @Override
      public void run() {
        textEt.requestFocus();
      }
    });
    return succeeded;
  }

  @OnClick(R2.id.add_btn)
  void onAddToggle() {
    hideEmojiTray();
    if (addContentContainer.getVisibility() == VISIBLE) {
      addContentContainer.setVisibility(GONE);
      addContentPager.setVisibility(GONE);  // set this to force destroy fragments

      inputContainer.setVisibility(VISIBLE);
      keyboardManager.requestDisplay();
    } else {
      addContentContainer.setVisibility(VISIBLE);
      addContentPager.setVisibility(VISIBLE);
      addContentTabs.getTabAt(1).select(); // TODO: remember last saved tab selection

      inputContainer.setVisibility(GONE);
      keyboardManager.requestHide();  // Make sure the keyboard is hidden
    }
  }

  private void hideEmojiTray() {
    emojiContainer.setVisibility(GONE);
    emojiBtn.setImageResource(R.drawable.ic_insert_emoticon_24dp);
  }

  private void showEmojiTray() {
    emojiContainer.setVisibility(VISIBLE);
    keyboardManager.requestHide();
    emojiBtn.setImageResource(R.drawable.ic_keyboard_24dp);
  }

  private final PhotoTakenCallback cameraPhotoTakenCallback = new PhotoTakenCallback() {
    @Override
    public void onPhotoTaken(final File photoFile) {
      post(new Runnable() {
        @Override
        public void run() {
          onAddToggle();
          handleAttachmentClick(new ItemClickedEvent<>(FileUtils.toAttachment(photoFile)));
          // TODO invalidate photo picker
        }
      });
    }
  };


  //region Events

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void handleItemClick(ItemClickedEvent<?> event) {
    Object item = event.item;
    if (item instanceof Emoji) {
      handleEmojiClick((ItemClickedEvent<Emoji>) event);
    } else {
      handleAttachmentClick((ItemClickedEvent<Attachment>) event);
    }
  }

  public void handleEmojiClick(ItemClickedEvent<Emoji> event) {
    // TODO figure out some way to allow custom spannables (e.g. fresco's DraweeSpan)
    textEt.getText().append(event.item.strValue);
  }

  public void handleAttachmentClick(ItemClickedEvent<Attachment> event) {
    attachmentPreviewAdapter.toggleItem(event.item);
    attachmentPreviewContainer.setVisibility(
        attachmentPreviewAdapter.getItemCount() == 0 ? GONE : VISIBLE);
  }

  //endregion
}
