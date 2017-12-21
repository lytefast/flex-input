package com.lytefast.flexinput.fragment

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.Dialog
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatDialog
import android.support.v7.app.AppCompatDialogFragment
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import com.lytefast.flexinput.FlexInputCoordinator
import com.lytefast.flexinput.R
import com.lytefast.flexinput.adapters.AddContentPagerAdapter
import com.lytefast.flexinput.model.Attachment
import com.lytefast.flexinput.model.Attachment.Companion.toAttachment
import com.lytefast.flexinput.utils.SelectionAggregator
import com.lytefast.flexinput.utils.SelectionCoordinator


/**
 * Full screen dialog with a [ViewPager] as a bottom sheet.
 *
 * @author Sam Shih
 */
open class AddContentDialogFragment : AppCompatDialogFragment() {

  private var contentPager: ViewPager? = null
  private var contentTabs: TabLayout? = null
  private var actionButton: FloatingActionButton? = null
  private var launchButton: ImageView? = null

  private var selectionAggregator: SelectionAggregator<Attachment<Any>>? = null

  private val itemSelectionListener = object : SelectionCoordinator.ItemSelectionListener<Attachment<*>> {
    override fun onItemSelected(item: Attachment<*>) {
      updateActionButton()
    }

    override fun onItemUnselected(item: Attachment<*>) {
      updateActionButton()
    }

    override fun unregister() {}
  }

  protected open val allIntents: List<Intent>
    @TargetApi(Build.VERSION_CODES.KITKAT)
    get() {
      val packageManager = context?.packageManager ?: return emptyList()

      val mimetypes = arrayOf("text/*", "image/*", "video/*")
      val resolveInfos = packageManager
          .queryIntentActivities(
              Intent(Intent.ACTION_GET_CONTENT)
                  .setType("application/*")
                  .putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
                  .addCategory(Intent.CATEGORY_OPENABLE)
                  .addCategory(Intent.CATEGORY_DEFAULT)
                  .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true),
              0)

      val intents = resolveInfos.mapTo(ArrayList(resolveInfos.size + 1)) { resolveInfo ->
        val componentName = ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)
        Intent(Intent.ACTION_GET_CONTENT)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setComponent(componentName)
            .setPackage(resolveInfo.activityInfo.packageName)
      }

      if (!intents.any { it.`package` == GOOGLE_DRIVE_PACKAGE }) {
        googleDriveIntent?.also { intents.add(it) }
      }
      return intents
    }

  /**
   * HACK: sigh. If you want to open up google drive file picker without pulling in the
   * google play drive libraries, this is the only way. For some reason gDrive doesn't
   * register as a when you try to perform a normal Intent.ACTION_PICK with any sort of filters.
   * It could be that google wants people to rely on the DocumentsProvider and system file picker.
   * However the system file picker doesn't auto handle virutal files so this is a workaround.
   *
   * @return Intent to open google drive file picker. Null if not found.
   */
  private val googleDriveIntent: Intent?
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    get() {
      val context = context ?: return null
      val resolveInfos = context.packageManager
          .queryIntentActivities(
              Intent(Intent.ACTION_PICK)
                  .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true),
              0)

      for (resolveInfo in resolveInfos) {
        val componentName = ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)

        if (resolveInfo.activityInfo.name == "$GOOGLE_DRIVE_PACKAGE.app.PickActivity") {
          return Intent(Intent.ACTION_PICK)
              .setComponent(componentName)
              .setPackage(resolveInfo.activityInfo.packageName)
        }
      }
      return null
    }

  private val launcherString: CharSequence
    get() {
      val value = TypedValue()
      val dialogTheme = dialog?.context?.theme
      val customString =
          if (dialogTheme != null
              && dialogTheme.resolveAttribute(R.attr.flexInputAddContentLauncherTitle, value, true)) {
            value.string
          } else {
            null
          }

      return when {
        customString.isNullOrBlank() -> getString(R.string.choose_an_application)
        else -> customString!!
      }
    }

  @SuppressLint("PrivateResource")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = object : AppCompatDialog(context, R.style.FlexInput_DialogWhenLarge) {
      override fun show() {
        super.show()
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
      }
    }
    dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

    dialog.window?.apply {
      setWindowAnimations(android.support.design.R.style.Animation_AppCompat_Dialog)
      setBackgroundDrawableResource(android.R.color.transparent)
    }
    return dialog
  }

  @SuppressLint("PrivateResource")
  override fun show(transaction: FragmentTransaction, tag: String): Int {
    transaction.setCustomAnimations(
        android.support.design.R.anim.abc_grow_fade_in_from_bottom,
        android.support.design.R.anim.abc_shrink_fade_out_from_bottom)
    return super.show(transaction, tag)
  }

  override fun onStart() {
    super.onStart()
    animateIn()
  }

  override fun onCreateView(inflater: LayoutInflater,
                            container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    val root = inflater.inflate(R.layout.dialog_add_content_pager_with_fab, container, false)
    root?.apply {
      setOnClickListener { onContentRootClick() }

      contentPager = findViewById(R.id.content_pager)
      contentTabs = findViewById(R.id.content_tabs)
      actionButton = findViewById(R.id.action_btn)
      launchButton = findViewById(R.id.launch_btn)
      launchButton?.setOnClickListener { launchFileChooser() }
    }

    val flexInputFragment = parentFragment
    if (flexInputFragment is FlexInputFragment) {
      initContentPages(
          AddContentPagerAdapter(childFragmentManager, *flexInputFragment.contentPages))

      actionButton?.setOnClickListener {
        dismissWithAnimation()
        flexInputFragment.onSend()
      }

      this.selectionAggregator = flexInputFragment.selectionAggregator
          .addItemSelectionListener(itemSelectionListener)
    }

    return root
  }

  override fun onResume() {
    super.onResume()
    actionButton?.post { updateActionButton() }
  }

  override fun onDestroyView() {
    selectionAggregator?.removeItemSelectionListener(itemSelectionListener)
    super.onDestroyView()
  }

  fun dismissWithAnimation() {
    animateOut().setAnimationListener(object : Animation.AnimationListener {
      override fun onAnimationStart(animation: Animation) {}

      override fun onAnimationEnd(animation: Animation) {
        dismiss()
      }

      override fun onAnimationRepeat(animation: Animation) {}
    })
  }

  fun onContentRootClick() {
    if (isCancelable) {  // TODO check setCanceledOnTouchOutside
      dismissWithAnimation()
    }
  }

  protected open fun initContentPages(pagerAdapter: AddContentPagerAdapter): AddContentDialogFragment {
    context?.let { context ->
      contentTabs?.also {
        pagerAdapter.initTabs(context, it)
        contentPager?.adapter = pagerAdapter
        synchronizeTabAndPagerEvents()
      }
    }
    return this
  }

  private fun synchronizeTabAndPagerEvents() {
    contentTabs?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      /**
       * Special cases the first item (keyboard) by closing the pager and opening the keyboard on click.
       */
      override fun onTabSelected(tab: TabLayout.Tab) {
        val tabPosition = tab.position
        if (tabPosition == 0) {
          dismissWithAnimation()
          return
        }
        contentPager?.currentItem = tabPosition - 1
      }

      override fun onTabUnselected(tab: TabLayout.Tab) {}

      override fun onTabReselected(tab: TabLayout.Tab) {}
    })

    contentPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
      override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

      override fun onPageSelected(position: Int) {
        contentTabs?.getTabAt(position + 1)?.select()
      }

      override fun onPageScrollStateChanged(state: Int) {}
    })
    // set the default to the first real tab
    contentTabs?.getTabAt(1)?.select()
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
  open fun launchFileChooser() {
    val imagePickerIntent = Intent(Intent.ACTION_PICK)
        .setType("image/*")
        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

    val chooserIntent = Intent.createChooser(imagePickerIntent, launcherString)
        .putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toTypedArray())
    startActivityForResult(chooserIntent, REQUEST_FILES)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
    super.onActivityResult(requestCode, resultCode, intentData)
    if (REQUEST_FILES != requestCode || Activity.RESULT_CANCELED == resultCode) {
      return
    }

    if (Activity.RESULT_OK != resultCode || intentData == null) {
      Toast.makeText(context, "Error loading files", Toast.LENGTH_SHORT).show()
      return
    }

    val contentResolver = context?.contentResolver ?: return
    val clipData = intentData.clipData

    @Suppress("UNCHECKED_CAST")
    val flexInputCoordinator = parentFragment as FlexInputCoordinator<Any>
    if (clipData == null) {
      val uri = intentData.data
      uri?.also { flexInputCoordinator.addExternalAttachment(it.toAttachment(contentResolver)) }
    } else {
      (0 until clipData.itemCount)
          .map { clipData.getItemAt(it).uri }
          .forEach { flexInputCoordinator.addExternalAttachment(it.toAttachment(contentResolver)) }
    }
  }

  //region Animation methods

  @SuppressLint("PrivateResource")
  private fun animateOut(): Animation {
    val animation = AnimationUtils.loadAnimation(
        context, android.support.design.R.anim.design_bottom_sheet_slide_out)
    animation.duration = resources
        .getInteger(android.support.design.R.integer.bottom_sheet_slide_duration).toLong()
    animation.setInterpolator(context, android.R.anim.accelerate_decelerate_interpolator)

    actionButton?.hide()
    contentTabs?.startAnimation(animation)
    contentPager?.startAnimation(animation)
    launchButton?.startAnimation(animation)

    return animation
  }

  @SuppressLint("PrivateResource")
  private fun animateIn(): Animation {
    val animation = AnimationUtils.loadAnimation(
        context, android.support.design.R.anim.design_bottom_sheet_slide_in)
    animation.duration = resources
        .getInteger(android.support.design.R.integer.bottom_sheet_slide_duration).toLong()
    animation.setInterpolator(context, android.R.anim.accelerate_decelerate_interpolator)

    contentTabs?.startAnimation(animation)
    contentPager?.startAnimation(animation)
    launchButton?.startAnimation(animation)
    return animation
  }

  //endregion

  private fun updateActionButton() {
    actionButton?.post {
      val numSelected = selectionAggregator?.size ?: 0
      if (numSelected > 0) {
        actionButton?.show()
      } else {
        actionButton?.hide()
      }
    }
  }

  companion object {

    val REQUEST_FILES = 5968
    val GOOGLE_DRIVE_PACKAGE = "com.google.android.apps.docs"
  }
}
