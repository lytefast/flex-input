package com.lytefast.flexinput.fragment;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lytefast.flexinput.R;
import com.lytefast.flexinput.R2;
import com.lytefast.flexinput.model.Emoji;
import com.lytefast.flexinput.utils.WidgetUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


/**
 * Default fragment to display various categories of unicode emojis.
 *
 * Any overriding classes should trigger
 * {@link com.lytefast.flexinput.fragment.FlexInputFragment#append(CharSequence)}
 * with an {@link Emoji} to ensure proper appending of emojis to the edit text.
 *
 * @author Sam Shih
 */

public abstract class EmojiCategoryPagerFragment extends Fragment {

  @BindView(R2.id.page_tabs) TabLayout pageTabs;
  @BindView(R2.id.view_pager) ViewPager viewPager;
  private Unbinder unbinder;

  @Nullable
  @Override
  public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
                           @Nullable final Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_paged_grid, container, false);
    unbinder = ButterKnife.bind(this, rootView);

    pageTabs.setupWithViewPager(viewPager);

    Emoji[] emojis = new Emoji[100];
    Emoji sampleEmoji = new Emoji("☺", new String[]{"smile", "happy"});
    for (int i = 0; i < 100; i++) {
      emojis[i] = sampleEmoji;
    }

    initFrom(buildEmojiCategoryData(emojis));
    return rootView;
  }

  public abstract List<Emoji.EmojiCategory> buildEmojiCategoryData(final Emoji[] emojis);

  @Override
  public void onDestroyView() {
    unbinder.unbind();
    super.onDestroyView();
  }

  public void initFrom(final List<Emoji.EmojiCategory> emojiCategories) {
    viewPager.setAdapter(new FragmentPagerAdapter(getFragmentManager()) {
      @Override
      public Fragment getItem(final int position) {
        return new EmojiGridFragment().with(emojiCategories.get(position));
      }

      @Override
      public int getCount() {
        return emojiCategories.size();
      }
    });

    setIcons(emojiCategories);
  }

  private void setIcons(final List<Emoji.EmojiCategory> emojiCategories) {

    ColorStateList iconColors = WidgetUtils.getColorStateList(getContext(),
                                                              R.color.tab_icon_color_selector);

    for (int i = 0; i < emojiCategories.size(); i++) {
      TabLayout.Tab tab = pageTabs.getTabAt(i)
          .setIcon(emojiCategories.get(i).icon);

      Drawable icon = tab.getIcon();
      if (icon != null) {
        icon = DrawableCompat.wrap(icon);
        DrawableCompat.setTintList(icon, iconColors);
      }
    }
  }
}
