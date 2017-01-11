package com.lytefast.flexinput.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lytefast.flexinput.R;
import com.lytefast.flexinput.R2;
import com.lytefast.flexinput.emoji.Emoji;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


/**
 * Default fragment to display various categories of unicode emojis.
 *
 * @author Sam Shih
 */

public class EmojiCategoryPagerFragment extends Fragment {

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

    initFrom(
        Arrays.asList(
            new Emoji.EmojiCategory("test", R.drawable.ic_audiotrack_light,
                Arrays.asList(emojis)
            ),
            new Emoji.EmojiCategory("test2", R.drawable.ic_tv_light,
                Arrays.asList(new Emoji("✊", new String[] {"fist"}))
            )
        )
    );
    return rootView;
  }

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

    for (int i = 0; i < emojiCategories.size(); i++) {
      pageTabs.getTabAt(i).setIcon(emojiCategories.get(i).icon);
    }
  }
}
