package com.lytefast.flexinput.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.lytefast.flexinput.R;
import com.lytefast.flexinput.R2;
import com.lytefast.flexinput.adapters.AddContentPagerAdapter;
import com.lytefast.flexinput.model.Attachment;
import com.lytefast.flexinput.utils.SelectionAggregator;
import com.lytefast.flexinput.utils.SelectionCoordinator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


/**
 * Full screen dialog with a {@link ViewPager} as a bottom sheet.
 *
 * @author Sam Shih
 */
public class AddContentDialogFragment extends AppCompatDialogFragment {

  @BindView(R2.id.content_pager) ViewPager contentPager;
  @BindView(R2.id.content_tabs) TabLayout contentTabs;
  @BindView(R2.id.action_btn) FloatingActionButton actionButton;
  private Unbinder unbinder;

  private SelectionAggregator<Attachment> selectionAggregator;


  @Override
  public Dialog onCreateDialog(final Bundle savedInstanceState) {
    AppCompatDialog dialog = new AppCompatDialog(getContext(), R.style.FlexInput_DialogWhenLarge) {
      @Override
      public void cancel() {
        super.cancel();
        AddContentDialogFragment.this.dismiss();
      }
    };

    dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.getWindow()
        .setWindowAnimations(android.support.design.R.style.Animation_AppCompat_Dialog);

    return dialog;
  }

  @Override
  public int show(final FragmentTransaction transaction, final String tag) {
    transaction.setCustomAnimations(
        android.support.design.R.anim.abc_grow_fade_in_from_bottom,
        android.support.design.R.anim.abc_shrink_fade_out_from_bottom);
    final int commitId = super.show(transaction, tag);
    return commitId;
  }

  @Override
  public void onStart() {
    super.onStart();
    animateIn();
  }

  @Override
  public void dismiss() {
    animateOut().setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(final Animation animation) {
      }

      @Override
      public void onAnimationEnd(final Animation animation) {
        AddContentDialogFragment.super.dismiss();
      }

      @Override
      public void onAnimationRepeat(final Animation animation) {
      }
    });
  }

  @Override
  public View onCreateView(final LayoutInflater inflater,
                           @Nullable final ViewGroup container,
                           @Nullable final Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.dialog_add_content_pager_with_fab, container, false);
    this.unbinder = ButterKnife.bind(this, root);

    Fragment parentFragment = getParentFragment();
    if (parentFragment instanceof FlexInputFragment) {
      final FlexInputFragment flexInputFragment = (FlexInputFragment) parentFragment;
      setTargetFragment(flexInputFragment, 0 /* result code unused */);
      initContentPages(
          new AddContentPagerAdapter(getChildFragmentManager(), flexInputFragment.getContentPages()));

      actionButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
          dismiss();
          flexInputFragment.onSend();
        }
      });

      this.selectionAggregator = flexInputFragment.getSelectionAggregator()
          .addItemSelectionListener(itemSelectionListener);
    }
    return root;
  }

  @Override
  public void onResume() {
    super.onResume();
    updateActionButton();
  }

  @Override
  public void onDestroyView() {
    unbinder.unbind();
    if (itemSelectionListener != null && selectionAggregator != null) {
      selectionAggregator.removeItemSelectionListener(itemSelectionListener);
    }
    super.onDestroyView();
  }

  @Override
  public void onSaveInstanceState(final Bundle outState) {
    setTargetFragment(null, -1);
    super.onSaveInstanceState(outState);
  }

  @OnClick(R2.id.content_root)
  void onContentRootClick() {
    if (isCancelable()) {  // TODO check setCanceledOnTouchOutside
      dismiss();
    }
  }

  protected AddContentDialogFragment initContentPages(@NonNull final AddContentPagerAdapter pagerAdapter) {
    pagerAdapter.initTabs(getContext(), contentTabs);
    contentPager.setAdapter(pagerAdapter);
    synchronizeTabAndPagerEvents();
    return this;
  }

  private void synchronizeTabAndPagerEvents() {
    contentTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
      /**
       * Special cases the first item (keyboard) by closing the pager and opening the keyboard on click.
       */
      @Override
      public void onTabSelected(final TabLayout.Tab tab) {
        int tabPosition = tab.getPosition();
        if (tabPosition == 0) {
          dismiss();
          return;
        }
        contentPager.setCurrentItem(tabPosition - 1);
      }

      @Override
      public void onTabUnselected(final TabLayout.Tab tab) {
      }

      @Override
      public void onTabReselected(final TabLayout.Tab tab) {
      }
    });

    contentPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
      }

      @Override
      public void onPageSelected(final int position) {
        contentTabs.getTabAt(position + 1).select();
      }

      @Override
      public void onPageScrollStateChanged(final int state) {
      }
    });
    // set the default to the first real tab
    contentTabs.getTabAt(1).select();
  }

  private SelectionCoordinator.ItemSelectionListener itemSelectionListener =
      new SelectionCoordinator.ItemSelectionListener() {
        @Override
        public void onItemSelected(final Object item) {
          updateActionButton();
        }

        @Override
        public void onItemUnselected(final Object item) {
          updateActionButton();
        }
      };


  //region Animation methods

  private Animation animateOut() {
    Animation animation = AnimationUtils.loadAnimation(
        getContext(), android.support.design.R.anim.abc_slide_out_bottom);
    animation.setDuration(getResources()
        .getInteger(android.support.design.R.integer.bottom_sheet_slide_duration));

    contentTabs.startAnimation(animation);
    contentPager.startAnimation(animation);
    return animation;
  }

  private Animation animateIn() {
    Animation animation = AnimationUtils.loadAnimation(
        getContext(), android.support.design.R.anim.abc_slide_in_bottom);
    animation.setDuration(getResources()
        .getInteger(android.support.design.R.integer.bottom_sheet_slide_duration));

    contentTabs.startAnimation(animation);
    contentPager.startAnimation(animation);
    return animation;
  }

  //endregion

  private void updateActionButton() {
    if (actionButton == null) {
      return;  // Fragment gone, nothing to do
    }

    if (selectionAggregator.getSize() > 0) {
      actionButton.show();
    } else {
      actionButton.hide();
    }
  }
}
