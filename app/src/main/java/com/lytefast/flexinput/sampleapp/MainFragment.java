package com.lytefast.flexinput.sampleapp;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.lytefast.flexinput.InputListener;
import com.lytefast.flexinput.adapters.AttachmentPreviewAdapter;
import com.lytefast.flexinput.fragment.FlexInputFragment;
import com.lytefast.flexinput.managers.KeyboardManager;
import com.lytefast.flexinput.managers.SimpleFileManager;
import com.lytefast.flexinput.model.Attachment;

import java.util.List;


/**
 * Sample of how to use the {@link FlexInputFragment} component.
 *
 * @author Sam Shih
 */
public class MainFragment extends Fragment {

  private RecyclerView recyclerView;

  private FlexInputFragment flexInput;
  private MessageAdapter msgAdapter;

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

    consumeSendIntent(getActivity().getIntent());
  }

  private void consumeSendIntent(final Intent intent) {
    if (intent.getAction() != Intent.ACTION_SEND) {
      return;
    }

    Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
    if (uri != null) {
      flexInput.addExternalAttachment(Attachment.fromUri(getContext().getContentResolver(), uri));
      intent.removeExtra(Intent.EXTRA_STREAM);
    }
  }

  private void optionalFeatures() {
    flexInput
        // Can be extended to provide custom previews (e.g. larger preview images, onclick) etc.
        .setAttachmentPreviewAdapter(new AttachmentPreviewAdapter(getContext().getContentResolver()));
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
   * Main point of interaction between the {@link FlexInputFragment} widget and the client.
   */
  private final InputListener flexInputListener = new InputListener() {
    @Override
    public boolean onSend(final Editable data, List<? extends Attachment> attachments) {
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
