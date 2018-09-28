package com.lytefast.flexinput.sampleapp;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import com.lytefast.flexinput.InputListener;
import com.lytefast.flexinput.adapters.AddContentPagerAdapter;
import com.lytefast.flexinput.adapters.AttachmentPreviewAdapter;
import com.lytefast.flexinput.adapters.EmptyListAdapter;
import com.lytefast.flexinput.fragment.CameraFragment;
import com.lytefast.flexinput.fragment.FilesFragment;
import com.lytefast.flexinput.fragment.FlexInputFragment;
import com.lytefast.flexinput.fragment.PhotosFragment;
import com.lytefast.flexinput.managers.KeyboardManager;
import com.lytefast.flexinput.managers.SimpleFileManager;
import com.lytefast.flexinput.model.Attachment;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;


/**
 * Sample of how to use the {@link FlexInputFragment} component.
 *
 * @author Sam Shih
 */
public class MainFragment extends Fragment {

  private RecyclerView recyclerView;

  private FlexInputFragment flexInput;
  private final MessageAdapter msgAdapter;

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
    recyclerView = view.findViewById(R.id.message_list);

    return view;
  }

  @Override
  public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

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
        .setContentPages(/* You can add custom PageSuppliers here */)
        .setInputListener(flexInputListener)
        .setFileManager(new SimpleFileManager("com.lytefast.flexinput.fileprovider", "FlexInput"))
        .setKeyboardManager(new KeyboardManager() {
          @Override
          public void requestDisplay(final EditText textEt) {
            if (textEt == null) {
              return;
            }
            imm.showSoftInput(textEt, InputMethodManager.SHOW_IMPLICIT);
          }

          @Override
          public void requestHide() {
            imm.hideSoftInputFromWindow(flexInput.getView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
          }
        });

    optionalFeatures();
    tryRiskyFeatures();

    consumeSendIntent();
  }

  private void consumeSendIntent() {
    Intent intent = getActivity().getIntent();
    Attachment<?> attachment =
        IntentUtil.consumeSendIntent(intent, getContext().getContentResolver());
    if (attachment != null) {
      flexInput.addExternalAttachment(attachment);
      // Look for text
      String text = intent.getStringExtra(Intent.EXTRA_TEXT);
      if (!TextUtils.isEmpty(text)) {
        flexInput.setText(text);
        intent.removeExtra(Intent.EXTRA_TEXT);
      }
    }
  }

  private void optionalFeatures() {
    flexInput
        // Can be extended to provide custom previews (e.g. larger preview images, onclick) etc.
        .setAttachmentPreviewAdapter(new AttachmentPreviewAdapter(getContext().getContentResolver()));

    final boolean hasCustomPages = false;
    if (hasCustomPages) {
      flexInput.setContentPages(createContentPages());
    }
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
  public void onResume() {
    super.onResume();
    flexInput.requestFocus();
  }

  /**
   * Not necessary if the defaults are sufficient. Add to this array if custom pages needed.
   */
  private static AddContentPagerAdapter.PageSupplier[] createContentPages() {
    return new AddContentPagerAdapter.PageSupplier[]{
        new AddContentPagerAdapter.PageSupplier(R.drawable.ic_image_24dp, R.string.attachment_photos) {
          @Override
          @NotNull
          public Fragment createFragment() {
            return new PhotosFragment();
          }
        },
        new AddContentPagerAdapter.PageSupplier(R.drawable.ic_file_24dp, R.string.attachment_files) {
          @Override
          @NotNull
          public Fragment createFragment() {
            return new CustomFilesFragment();
          }
        },
        new AddContentPagerAdapter.PageSupplier(R.drawable.ic_add_a_photo_24dp, R.string.attachment_camera) {
          @Override
          @NotNull
          public Fragment createFragment() {
            return new CameraFragment();
          }
        }
    };
  }

  public static class CustomFilesFragment extends FilesFragment {
    @Override @NotNull
    protected EmptyListAdapter newPermissionsRequestAdapter(final View.OnClickListener onClickListener) {
       return new EmptyListAdapter(
        R.layout.custom_permission_storage, R.id.permissions_req_btn, onClickListener);
    }
  }

  /**
   * Main point of interaction between the {@link FlexInputFragment} widget and the client.
   */
  private final InputListener flexInputListener = new InputListener() {
    @Override
    public boolean onSend(final Editable data, List<? extends Attachment<?>> attachments) {
      if (data.length() > 0) {
        msgAdapter.addMessage(new MessageAdapter.Data(data, null));
      }

      for (int i = 0; i < attachments.size(); i++) {
        msgAdapter.addMessage(new MessageAdapter.Data(
                Editable.Factory.getInstance().newEditable(String.format("[%d] Attachment", i)),
                attachments.get(i)));
      }
      return true;
    }
  };
}
