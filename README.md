# flex-input
Android text/emoji/media input field

<img src="/images/keyboard.png" width="240">
  
<details>
 <summary>Image Examples</summary>
  ![Keybard Entry with attachment preview](/images/keyboard.png?raw=true "Keybard Entry with attachment preview")
  ![Emoji Entry](/images/emojiPicker.png?raw=true "Emoji Entry")

  ![Files tab](/images/tabFiles.png?raw=true "Files tab")
  ![Photos tab](/images/tabPhotos.png?raw=true "Photos tab")
  ![Camera tab](/images/tabCamera.png?raw=true "Camera tab")
</details>

# Dependencies
- This is currently using the still in development [cameraview](https://github.com/google/cameraview/)
  - at the current moment the cameraview project is just a symlink to the [camereaview/library project](https://github.com/google/cameraview/tree/master/library)
  - you must do the same (include as a local project module dependency) in you project
- [Butterknife](http://jakewharton.github.io/butterknife/)
- [Fresco](http://frescolib.org/)

# Work in Progress
- [x] Handle permission requests and denials

# Usage
For more details refer to the sample app included in this project.

## XML
To use the widget, you can just include it in your layout `my_layout.xml`:
```xml
<fragment
    android:id="@+id/flex_input"
    android:name="com.lytefast.flexinput.fragment.FlexInputFragment"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:focusable="true"
    android:focusableInTouchMode="true"

    app:inputBackground="@drawable/rect_rounded_highlight_alpha_20"
    app:previewBackground="@drawable/rect_rounded_highlight_alpha_20"
    app:tabsBackground="@drawable/content_tab_background"
    app:hint="@string/msg_hint"
    app:hintColor="@color/colorHint"/>
```
### Appearance styles
The widget tries to reuse as much of the app style as possible: icon colors are set via the `colorButtonNormal`, `colorControlActivated`, and `colorControlHighlight` style attributes.

See [`colors.xml`](flexinput/src/main/res/values/colors.xml):
```xml
<!--These are used within FlexInput. Override to customize.-->
<!-- Defaults for these values are style attr and listed beside the overrides-->
<color name="flexInputIconColor">@color/colorPrimary</color>  <!-- colorButtonNormal -->
<color name="flexInputIconColorActivated">@color/colorPrimaryDark</color>  <!-- colorControlActivated -->
<color name="flexInputIconColorDisabled">#c03f4a54</color>  <!-- colorControlHighlight -->
```
Or the same attributes in [`styles.xml`](flexinput/src/main/res/values/styles.xml).
<details>
 <summary>Sample overrides of interest</summary>
  ```
  <style name="FlexInput.Main" parent="AppTheme">
    <item name="android:background">@color/app_input_background_color</item>
  </style>
  <style name="FlexInput.AddContent.TabLayout" parent="FlexInput.AddContent.Base.TabLayout">
    <item name="android:background">@drawable/rounded_rect</item>
  </style>
  ```
</details>

There are also `styles.xml` overrides that you can provide. All styles are prefixed by `FlexInput`. See [`styles.xml`](flexinput/src/main/res/values/styles.xml) for the full set of styles.

Addtionally there are special `app` attributes that you may set to customize the appearance of the widget.
- `inputBackground` defines the background for the text input row
- `previewBackground` defines the background for the attachment preview row

## Setup
Now you need to add some hooks and adapters to make sure everything works. Don't worry there are some default implementations that can just be dropped in.

In `MyFragment.java`:
```java
@Override
public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
  super.onViewCreated(view, savedInstanceState);

  final InputMethodManager imm =
      (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

  flexInput = (FlexInputFragment) getChildFragmentManager().findFragmentById(R.id.flex_input);
  if (savedInstanceState == null) {
    // Only create fragment on first load
    // UnicodeEmojiCategoryPagerFragment is a default implementation (see sample app)
    flexInput.setEmojiFragment(new UnicodeEmojiCategoryPagerFragment());
  }

  flexInput
      .setContentPages(/* You can add custom PageSuppliers here */)
      .setInputListener(flexInputListener)
      .setFileManager(new SimpleFileManager("com.lytefast.flexinput.fileprovider", "FlexInput"))
      .setKeyboardManager(new KeyboardManager() {
        @Override
        public void requestDisplay() {
          flexInput.requestFocus();
          imm.showSoftInput(flexInput.getView(), InputMethodManager.SHOW_IMPLICIT);
        }

        @Override
        public void requestHide() {
          imm.hideSoftInputFromWindow(flexInput.getView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
      });
}

/**
 * Main point of interaction between the {@link FlexInput} widget and the client. 
 */
private final InputListener flexInputListener = new InputListener() {
  @Override
  public void onSend(final Editable data, List<? extends Attachment> attachments) {

    Log.d("SAMPLE", "User sent: " + data.toString());

    for (int i = 0; i < attachments.size(); i++) {
      Log.d("SAMPLE", String.format("[%d] Attachment - %s", i, attachments.get(i).displayName)));
    }
  }
};
```

That's it! Now you have the output string and the attachments (in order) from the user.

# Optional Features
```java
private void optionalFeatures() {
  flexInput
      // Can be extended to provide custom previews (e.g. larger preview images, onclick) etc.
      .setAttachmentPreviewAdapter(new AttachmentPreviewAdapter(getContext().getContentResolver()));
}
```

# Power Overrides (Risky)

Additionaly there are some ways to integrate easier with your apps. However these features could potentially cause unforeseen problems. Last warning!

Using a custom EditText:
```java
private void tryRiskyFeatures() {
  final boolean hasCustomEditText = true;
  if (hasCustomEditText) {
    LayoutInflater inflater = LayoutInflater.from(getContext());
    AppCompatEditText myEditText = (AppCompatEditText) inflater.inflate(
        R.layout.my_edit_text_view, (ViewGroup) flexInput.getView(), false);
    flexInput.setEditTextComponent(myEditText);
  }
}
```

You can trigger the feature via the sample app to experiment.
