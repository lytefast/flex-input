package com.lytefast.flexinput;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.lytefast.flexinput.fragment.RecyclerViewFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;


/**
 * Text, emoji, and media input field.
 */
public class FlexInput extends RelativeLayout {
  @BindView(R2.id.main_input_container) View inputContainer;
  @BindView(R2.id.add_content_container) View addContentContainer;
  @BindView(R2.id.emoji_container) View emojiContainer;

  @BindView(R2.id.text_input) AppCompatEditText textEt;
  @BindView(R2.id.emoji_btn) AppCompatImageButton emojiBtn;
  @BindView(R2.id.add_content_pager) ViewPager addContentPager;
  @BindView(R2.id.add_content_tabs) TabLayout addContentTabs;

  private KeyboardManager keyboardManager;
  private InputListener inputListener;


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

  private void init(AttributeSet attrs, int defStyle) {
    inflate(getContext(), R.layout.fancy_input_wrapper, this);
    ButterKnife.bind(this);

    initAttributes(attrs, defStyle);

    setFocusable(true);
    setFocusableInTouchMode(true);
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

      if (a.hasValue(R.styleable.FlexInput_tabsBackground)) {
        Drawable backgroundDrawable = a.getDrawable(R.styleable.FlexInput_tabsBackground);
        backgroundDrawable.setCallback(this);
        addContentTabs.setBackground(backgroundDrawable);
      }
    } finally {
      a.recycle();
    }
  }

  public FlexInput setInputListener(@NonNull final InputListener inputListener) {
    this.inputListener = inputListener;
    return this;
  }

  /**
   * Gets the example string attribute value.
   *
   * @return The example string attribute value.
   */
  public Editable getExampleString() {
    return textEt.getText();
  }

  /**
   * Sets the view's example string attribute value. In the example view, this string
   * is the text to draw.
   *
   * @param exampleString The example string attribute value to use.
   */
  public void setExampleString(final String exampleString) {
    textEt.setText(exampleString);
  }

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
          case 0:
          case 1:
          case 2:
            return new RecyclerViewFragment();
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
    addContentPager.setOffscreenPageLimit(0);  // Don't preload anything as some are expensive
    synchronizeTabAndPagerEvents();
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

  @OnClick(R2.id.send_btn)
  void onSend() {
    if (textEt.length() == 0) {
      return;  // Nothing to do here
    }
    inputListener.onSend(textEt.getText());
    textEt.setText("");
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

  @OnClick(R2.id.add_btn)
  void onAddToggle() {
    hideEmojiTray();
    if (addContentContainer.getVisibility() == VISIBLE) {
      addContentContainer.setVisibility(GONE);
      addContentPager.setVisibility(GONE);  // set this to force destroy fragments

      inputContainer.requestFocus();
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

  void onAddFile() {
    // TODO: open file browser
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
}
