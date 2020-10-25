package com.lytefast.flexinput.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.lytefast.flexinput.R
import com.lytefast.flexinput.model.EmojiCategory

/**
 * Default fragment to display various categories of unicode emojis.
 *
 *
 * Any overriding classes should trigger [FlexInputFragment.append]
 * with an [Emoji] to ensure proper appending of emojis to the edit text.
 *
 * @author Sam Shih
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class EmojiCategoryPagerFragment : Fragment() {

  private lateinit var pageTabs: TabLayout
  private lateinit var viewPager: ViewPager

  override fun onCreateView(inflater: LayoutInflater,
                            container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    val rootView = inflater.inflate(R.layout.fragment_paged_grid, container, false)
    viewPager = rootView.findViewById(R.id.view_pager)
    pageTabs = rootView.findViewById(R.id.page_tabs)
    pageTabs.setupWithViewPager(viewPager)

    initFrom(buildEmojiCategoryData())
    return rootView
  }

  /**
   * @return a list where each [EmojiCategory] represents a tab.
   */
  abstract fun buildEmojiCategoryData(): List<EmojiCategory>

  fun initFrom(emojiCategories: List<EmojiCategory>) {
    viewPager.adapter = object : FragmentPagerAdapter(parentFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
      override fun getItem(position: Int): Fragment {
        return EmojiGridFragment().with(emojiCategories[position])
      }

      override fun getCount(): Int {
        return emojiCategories.size
      }
    }
    setIcons(emojiCategories)
  }

  protected fun setIcons(emojiCategories: List<EmojiCategory>) {
    val iconColors = AppCompatResources.getColorStateList(requireContext(), R.color.tab_color_selector)
    for (i in emojiCategories.indices) {
      var icon = pageTabs.getTabAt(i)?.setIcon(emojiCategories[i].icon)?.icon ?: continue

      icon = DrawableCompat.wrap(icon)
      DrawableCompat.setTintList(icon, iconColors)
    }
  }
}