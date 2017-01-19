package com.lytefast.flexinput.sampleapp;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.lytefast.flexinput.InputListener;
import com.lytefast.flexinput.fragment.FlexInputFragment;
import com.lytefast.flexinput.managers.KeyboardManager;
import com.lytefast.flexinput.managers.SimpleFileManager;
import com.lytefast.flexinput.adapters.AttachmentPreviewAdapter;
import com.lytefast.flexinput.model.Attachment;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


/**
 * Sample of how to use the {@link FlexInputFragment} component.
 *
 * @author Sam Shih
 */
public class MainFragment extends Fragment {

  @BindView(R.id.message_list) RecyclerView recyclerView;
  private FlexInputFragment flexInput;

  private Unbinder unbinder;
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

  private class MessageAdapter extends RecyclerView.Adapter<ViewHolder> {
    List<Editable> msgList = new ArrayList<>();

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.message_row, parent, false);
    return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
      holder.bind(msgList.get(position));
    }

    @Override
    public int getItemCount() {
      return msgList.size();
    }

    public void addMessage(Editable msg) {
      msgList.add(msg);
      notifyItemInserted(msgList.size() - 1);
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.message_tv) TextView messageTv;

    public ViewHolder(final View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void bind(Editable data) {
      messageTv.setText(data);
    }
  }
}
