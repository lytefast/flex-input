package com.lytefast.flexinput.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.content.res.AppCompatResources;

import com.lytefast.flexinput.R;
import com.lytefast.flexinput.fragment.CameraFragment;
import com.lytefast.flexinput.fragment.FilesFragment;
import com.lytefast.flexinput.fragment.PhotosFragment;


/**
 * {@link FragmentPagerAdapter} for the "Add Content" {@link android.support.v4.view.ViewPager}
 * allowing the user to set custom pages via a list of {@link PageSupplier}s.
 *
 * @author Sam Shih
 * @see #createDefaultPages() for the default set of pages that are supported
 */
public class AddContentPagerAdapter extends FragmentPagerAdapter {

  private final PageSupplier[] pageSuppliers;


  public AddContentPagerAdapter(final FragmentManager childFragmentManager, PageSupplier... pageSuppliers) {
    super(childFragmentManager);
    this.pageSuppliers =
        (pageSuppliers == null || pageSuppliers.length == 0) ? createDefaultPages() : pageSuppliers;
  }

  @Override
  public Fragment getItem(final int position) {
    return pageSuppliers[position].createFragment();
  }

  @Override
  public int getCount() {
    return pageSuppliers.length;
  }

  public void initTabs(final Context context, final TabLayout tabLayout) {
    final ColorStateList iconColors =
        AppCompatResources.getColorStateList(context, R.color.tab_color_selector);

    // Color the existing tabs
    for (int i = 0; i < tabLayout.getTabCount(); i++) {
      setIconColor(iconColors, tabLayout.getTabAt(i));
    }

    // Create tabs for the adapter pages
    for (PageSupplier page : pageSuppliers) {
      TabLayout.Tab tab = tabLayout.newTab()
          .setIcon(page.icon)
          .setContentDescription(page.contentDesc);
      setIconColor(iconColors, tab);
      tabLayout.addTab(tab);
    }
  }

  private void setIconColor(final ColorStateList iconColors, final TabLayout.Tab tab) {
    Drawable icon = tab.getIcon();
    if (icon != null) {
      icon = DrawableCompat.wrap(icon);
      DrawableCompat.setTintList(icon, iconColors);
    }
  }

  public static abstract class PageSupplier {
    @DrawableRes protected final int icon;
    @StringRes protected final int contentDesc;

    public PageSupplier(@DrawableRes int icon, @StringRes final int contentDesc) {
      this.icon = icon;
      this.contentDesc = contentDesc;
    }

    public abstract Fragment createFragment();
  }

  public static PageSupplier[] createDefaultPages() {
    return new PageSupplier[]{
        new PageSupplier(R.drawable.ic_file_24dp, R.string.attachment_files) {
          @Override
          public Fragment createFragment() {
            return new FilesFragment();
          }
        },
        new PageSupplier(R.drawable.ic_image_24dp, R.string.attachment_photos) {
          @Override
          public Fragment createFragment() {
            return new PhotosFragment();
          }
        },
        new PageSupplier(R.drawable.ic_add_a_photo_24dp, R.string.attachment_camera) {
          @Override
          public Fragment createFragment() {
            return new CameraFragment();
          }
        }
    };
  }
}
