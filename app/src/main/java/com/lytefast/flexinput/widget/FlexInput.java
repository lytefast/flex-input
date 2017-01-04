package com.lytefast.flexinput.widget;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.lytefast.flexinput.R;
import com.lytefast.flexinput.fragment.RecyclerViewFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * Text, emoji, and media input field.
 */
public class FlexInput extends RelativeLayout {
  private String mExampleString; // TODO: use a default from R.string...
  private int mExampleColor = Color.RED; // TODO: use a default from R.color...
  private float mExampleDimension = 0; // TODO: use a default from R.dimen...
  private Drawable mExampleDrawable;


  @BindView(R.id.main_input_container) View inputContainer;
  @BindView(R.id.add_content_container) View addContentContainer;
  @BindView(R.id.emoji_container) View emojiContainer;

  @BindView(R.id.text_input) AppCompatEditText textEt;
  @BindView(R.id.emoji_btn) AppCompatImageButton emojiBtn;
  @BindView(R.id.add_content_pager) ViewPager addContentPager;
  @BindView(R.id.add_content_tabs) TabLayout addContentTabs;
  private KeyboardManager keyboardManager;


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
    initAttributes(attrs, defStyle);
    inflate(getContext(), R.layout.fancy_input_wrapper, this);

    setFocusable(true);
    setFocusableInTouchMode(true);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    ButterKnife.bind(this);

    if (!TextUtils.isEmpty(mExampleString)) {
      textEt.setText(mExampleString);
    }
  }

  private void initAttributes(final AttributeSet attrs, final int defStyle) {
    final TypedArray a = getContext().obtainStyledAttributes(
        attrs, R.styleable.FlexInput, defStyle, 0);

    mExampleString = a.getString(
        R.styleable.FlexInput_exampleString);
    mExampleColor = a.getColor(
        R.styleable.FlexInput_exampleColor,
        mExampleColor);
    // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
    // values that should fall on pixel boundaries.
    mExampleDimension = a.getDimension(
        R.styleable.FlexInput_exampleDimension,
        mExampleDimension);

    if (a.hasValue(R.styleable.FlexInput_exampleDrawable)) {
      mExampleDrawable = a.getDrawable(
          R.styleable.FlexInput_exampleDrawable);
      mExampleDrawable.setCallback(this);
    }

    a.recycle();
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

  @OnClick(R.id.send_btn)
  void onSend() {
    // TODO: figure out a way to publish events
    Toast.makeText(getContext(), "Text Sent: " + textEt.getText().toString(), Toast.LENGTH_SHORT).show();
    textEt.setText("");
  }

  @OnClick(R.id.text_input)
  void onTextInputTouch() {
    hideEmojiTray();
  }

  @OnClick(R.id.emoji_btn)
  void onEmojiToggle() {
    if (emojiContainer.getVisibility() == VISIBLE) {
      hideEmojiTray();
    } else {
      showEmojiTray();
    }

    addContentPager.setVisibility(GONE);
  }

  @OnClick(R.id.add_btn)
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
    keyboardManager.requestDisplay();
    emojiBtn.setImageResource(R.drawable.ic_insert_emoticon_24dp);
  }

  private void showEmojiTray() {
    emojiContainer.setVisibility(VISIBLE);
    keyboardManager.requestHide();
    emojiBtn.setImageResource(R.drawable.ic_keyboard_24dp);
  }
}
