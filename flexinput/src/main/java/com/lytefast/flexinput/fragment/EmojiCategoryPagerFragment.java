package com.lytefast.flexinput.fragment;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.content.res.AppCompatResources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import com.lytefast.flexinput.R;
import com.lytefast.flexinput.model.Emoji;
import com.lytefast.flexinput.model.EmojiCategory;


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

  private TabLayout pageTabs;
  private ViewPager viewPager;

  @Nullable
  @Override
  public View onCreateView(@NonNull final LayoutInflater inflater,
                           @Nullable final ViewGroup container,
                           @Nullable final Bundle savedInstanceState) {
    View rootView = inflateView(inflater, container);

    viewPager = rootView.findViewById(R.id.view_pager);
    pageTabs = rootView.findViewById(R.id.page_tabs);
    pageTabs.setupWithViewPager(viewPager);

    initFrom(buildEmojiCategoryData());
    return rootView;
  }

  protected View inflateView(final LayoutInflater inflater, final @Nullable ViewGroup container) {
    return inflater.inflate(R.layout.fragment_paged_grid, container, false);
  }

  /**
   * @return a list where each {@link com.lytefast.flexinput.model.EmojiCategory} represents a tab.
   */
  public abstract List buildEmojiCategoryData();

  public void initFrom(final List<EmojiCategory> emojiCategories) {
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

  protected void setIcons(final List<EmojiCategory> emojiCategories) {
    ColorStateList iconColors =
        AppCompatResources.getColorStateList(getContext(), R.color.tab_color_selector);

    for (int i = 0; i < emojiCategories.size(); i++) {
      TabLayout.Tab tab = pageTabs.getTabAt(i)
          .setIcon(emojiCategories.get(i).getIcon());

      Drawable icon = tab.getIcon();
      if (icon != null) {
        icon = DrawableCompat.wrap(icon);
        DrawableCompat.setTintList(icon, iconColors);
      }
    }
  }
}
