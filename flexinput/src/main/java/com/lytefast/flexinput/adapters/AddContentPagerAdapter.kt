package com.lytefast.flexinput.adapters

import android.content.Context
import android.content.res.ColorStateList
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.lytefast.flexinput.R
import com.lytefast.flexinput.adapters.AddContentPagerAdapter.PageSupplier
import com.lytefast.flexinput.fragment.CameraFragment
import com.lytefast.flexinput.fragment.FilesFragment
import com.lytefast.flexinput.fragment.PhotosFragment


/**
 * [FragmentPagerAdapter] for the "Add Content" [androidx.viewpager.widget.ViewPager]
 * allowing the user to set custom pages via a list of [PageSupplier]s.
 *
 * @author Sam Shih
 * @see .createDefaultPages
 */
class AddContentPagerAdapter(childFragmentManager: FragmentManager,
                             private vararg val pageSuppliers: PageSupplier)
  : FragmentPagerAdapter(childFragmentManager) {

  override fun getItem(position: Int): Fragment = pageSuppliers[position].createFragment()

  override fun getCount(): Int = pageSuppliers.size

  fun initTabs(context: Context, tabLayout: TabLayout) {
    val iconColors = AppCompatResources.getColorStateList(context, R.color.tab_color_selector)

    // Color the existing tabs
    for (i in 0 until tabLayout.tabCount) {
      tabLayout.getTabAt(i)?.setIconColor(iconColors)
    }

    // Create tabs for the adapter pages
    pageSuppliers
        .map {
          tabLayout.newTab()
              .setIcon(it.icon)
              .setIconColor(iconColors)
              .setContentDescription(it.contentDesc)
        }
        .forEach { tabLayout.addTab(it) }
  }

  private fun TabLayout.Tab.setIconColor(iconColors: ColorStateList) : TabLayout.Tab {
    this.icon?.let {
      val icon = DrawableCompat.wrap(it)
      DrawableCompat.setTintList(icon, iconColors)
      this.icon = icon
    }
    return this
  }

  abstract class PageSupplier(@DrawableRes val icon: Int, @StringRes val contentDesc: Int) {

    abstract fun createFragment(): Fragment
  }

  companion object {

    fun createDefaultPages(): Array<PageSupplier> {
      return arrayOf(
          object : PageSupplier(R.drawable.ic_image_24dp, R.string.attachment_photos) {
            override fun createFragment(): Fragment = PhotosFragment()
          },
          object : PageSupplier(R.drawable.ic_file_24dp, R.string.attachment_files) {
            override fun createFragment(): Fragment = FilesFragment()
          },
          object : PageSupplier(R.drawable.ic_add_a_photo_24dp, R.string.attachment_camera) {
            override fun createFragment(): Fragment = CameraFragment()
          })
    }
  }
}
