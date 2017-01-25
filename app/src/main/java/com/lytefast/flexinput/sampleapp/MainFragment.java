package com.lytefast.flexinput.sampleapp;


import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.lytefast.flexinput.InputListener;
import com.lytefast.flexinput.adapters.AttachmentPreviewAdapter;
import com.lytefast.flexinput.fragment.FlexInputFragment;
import com.lytefast.flexinput.managers.KeyboardManager;
import com.lytefast.flexinput.managers.PermissionsManager;
import com.lytefast.flexinput.managers.SimpleFileManager;
import com.lytefast.flexinput.model.Attachment;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


/**
 * Sample of how to use the {@link FlexInputFragment} component.
 *
 * @author Sam Shih
 */
public class MainFragment extends Fragment implements PermissionsManager {

  private static final int MY_PERMISSIONS_REQUEST_CODE = 9999;

  @BindView(R.id.message_list) RecyclerView recyclerView;
  private FlexInputFragment flexInput;

  private Unbinder unbinder;
  private MessageAdapter msgAdapter;
  private PermissionsResultCallback permissionRequestCallback;

  public MainFragment() {
    this.msgAdapter = new MessageAdapter();
  }


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_message_main, container, false);
    return view;
  }

  @Override
  public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    unbinder = ButterKnife.bind(this, getView());

    final InputMethodManager imm =
        (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

    recyclerView.setAdapter(msgAdapter);

    flexInput = (FlexInputFragment) getChildFragmentManager().findFragmentById(R.id.flex_input);
    if (savedInstanceState == null) {
      // Only create fragment on first load
      // UnicodeEmojiCategoryPagerFragment is a default implementation (see sample app)
      flexInput.setEmojiFragment(new UnicodeEmojiCategoryPagerFragment());
    }

    flexInput
        .setPermissionsManager(this)
        .initContentPages(/* You can add custom PageSuppliers here */)
        // Can be extended to provide custom previews (e.g. larger preview images, onclick) etc.
        .setAttachmentPreviewAdapter(new AttachmentPreviewAdapter(getContext().getContentResolver()))
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

    tryRiskyFeatures();
  }

  private void tryRiskyFeatures() {
    final boolean hasCustomEditText = false;
    if (hasCustomEditText) {
      LayoutInflater inflater = LayoutInflater.from(getContext());
      AppCompatEditText myEditText = (AppCompatEditText) inflater.inflate(
          R.layout.my_edit_text_view, (ViewGroup) flexInput.getView(), false);
      flexInput.setEditTextComponent(myEditText);
    }
  }

  @Override
  public void onDestroyView() {
    unbinder.unbind();
    super.onDestroyView();
  }

  /**
   * Main point of interaction between the {@link FlexInputFragment} widget and the client.
   */
  private final InputListener flexInputListener = new InputListener() {
    @Override
    public void onSend(final Editable data, List<? extends Attachment> attachments) {
      msgAdapter.addMessage(data);

      for (int i = 0; i < attachments.size(); i++) {
        msgAdapter.addMessage(Editable.Factory.getInstance().newEditable(
            String.format("[%d] Attachment - %s", i, attachments.get(i).displayName)));
      }
    }
  };

  //region PermissionsManager methods

  @Override
  public boolean requestFileReadPermission(final PermissionsManager.PermissionsResultCallback callback) {
    permissionRequestCallback = callback;
    if (ContextCompat.checkSelfPermission(
            getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(
            getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(
            getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

      // Should we show an explanation?
      if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {

        // Show an explanation to the user *asynchronously* -- don't block
        // this thread waiting for the user's response! After the user
        // sees the explanation, try again to request the permission.
        Toast.makeText(getContext(), "Why we need permissions...", Toast.LENGTH_SHORT).show();

      } else {
        // No explanation needed, we can request the permission.
      }
      requestPermissions(
          new String[]{
              Manifest.permission.READ_EXTERNAL_STORAGE,
              Manifest.permission.WRITE_EXTERNAL_STORAGE,
              Manifest.permission.CAMERA},
          MY_PERMISSIONS_REQUEST_CODE);
      return false;
    }
    callback.granted();
    return true;
  }

  @Override
  public boolean requestCameraPermission(final PermissionsManager.PermissionsResultCallback callback) {
    return requestCameraPermission(callback);
  }

  //endregion


  @Override
  public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
    if (requestCode != MY_PERMISSIONS_REQUEST_CODE) {
      return;
    }

    // If request is cancelled, the result arrays are empty.
    if (grantResults.length > 0
        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      permissionRequestCallback.granted();
    } else {
      permissionRequestCallback.denied();
    }
  }
}
