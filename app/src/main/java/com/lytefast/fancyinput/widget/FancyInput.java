package com.lytefast.fancyinput.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.lytefast.fancyinput.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * TODO: document your custom view class.
 */
public class FancyInput extends RelativeLayout {
    private String mExampleString; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
    private Drawable mExampleDrawable;


    @BindView(R.id.main_input_container) View inputContainer;
    @BindView(R.id.add_content_container) View addContentContainer;
    @BindView(R.id.emoji_container) View emojiContainer;

    @BindView(R.id.text_input) AppCompatEditText textEt;
    @BindView(R.id.add_content_pager) ViewPager addPager;


    public FancyInput(Context context) {
        super(context);
        init(null, 0);
    }

    public FancyInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public FancyInput(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        initAttributes(attrs, defStyle);
        inflate(getContext(), R.layout.fancy_input_wrapper, this);
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
                attrs, R.styleable.FancyInput, defStyle, 0);

        mExampleString = a.getString(
                R.styleable.FancyInput_exampleString);
        mExampleColor = a.getColor(
                R.styleable.FancyInput_exampleColor,
                mExampleColor);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mExampleDimension = a.getDimension(
                R.styleable.FancyInput_exampleDimension,
                mExampleDimension);

        if (a.hasValue(R.styleable.FancyInput_exampleDrawable)) {
            mExampleDrawable = a.getDrawable(
                    R.styleable.FancyInput_exampleDrawable);
            mExampleDrawable.setCallback(this);
        }

        a.recycle();
    }

    /**
     * Gets the example string attribute value.
     *
     * @return The example string attribute value.
     */
    public String getExampleString() {
        return textEt.getText().toString();
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

    public void setPagerAdapter(final PagerAdapter pagerAdapter) {
        addPager.setAdapter(pagerAdapter);
        addPager.setOffscreenPageLimit(0);  // Don't preload anything as some are expensive
    }

    @OnClick(R.id.send_btn)
    void onSend() {
        // TODO: figure out a way to publish events
        Toast.makeText(getContext(), "Text Sent: " + textEt.getText().toString(), Toast.LENGTH_SHORT).show();
        textEt.setText("");
    }

    @OnClick(R.id.emoji_btn)
    void onEmojiToggle(AppCompatImageButton btn) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (emojiContainer.getVisibility() == VISIBLE) {
            emojiContainer.setVisibility(GONE);
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
            btn.setImageResource(R.drawable.ic_insert_emoticon_24dp);
        } else {
            emojiContainer.setVisibility(VISIBLE);
            imm.hideSoftInputFromWindow(this.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            btn.setImageResource(R.drawable.ic_keyboard_24dp);
        }

        addPager.setVisibility(GONE);
    }

    @OnClick({R.id.add_btn, R.id.add_content_keyboard_btn})
    void onAddToggle() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (addContentContainer.getVisibility() == VISIBLE) {
            addContentContainer.setVisibility(GONE);
            inputContainer.setVisibility(VISIBLE);
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
        } else {
            addContentContainer.setVisibility(VISIBLE);
            inputContainer.setVisibility(GONE);
            imm.hideSoftInputFromWindow(this.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        emojiContainer.setVisibility(GONE);
    }
}
