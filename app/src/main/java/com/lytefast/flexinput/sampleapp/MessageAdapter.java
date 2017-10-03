package com.lytefast.flexinput.sampleapp;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.lytefast.flexinput.model.Attachment;

import java.util.ArrayList;
import java.util.List;


/**
 * Simple string message display adapter.
 * @author Sam Shih
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
  List<Data> msgList = new ArrayList<>();

  @Override
  public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.message_row, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(final ViewHolder holder, final int position) {
    holder.bind(msgList.get(position), position);
  }

  @Override
  public int getItemCount() {
    return msgList.size();
  }

  public void addMessage(Data msg) {
    msgList.add(msg);
    notifyItemInserted(msgList.size() - 1);
  }

  public static class Data {
    final Editable editable;
    @Nullable final Attachment attachment;

    public Data(Editable editable, Attachment attachment) {
      this.editable = editable;
      this.attachment = attachment;
    }
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    private TextView indexTv;
    private TextView messageTv;
    private TextView attachmentTv;
    private SimpleDraweeView imageView;

    public ViewHolder(final View itemView) {
      super(itemView);

      indexTv = itemView.findViewById(R.id.index_tv);
      messageTv = itemView.findViewById(R.id.message_tv);
      attachmentTv = itemView.findViewById(R.id.attachment_tv);
      imageView = itemView.findViewById(R.id.attachment_iv);
    }

    public void bind(Data data, int index) {
      indexTv.setText(String.valueOf(index));
      messageTv.setText(data.editable);
      if (data.attachment != null) {
        imageView.setVisibility(View.VISIBLE);
        Uri uri = data.attachment.getUri();
        imageView.setImageURI(uri);

        attachmentTv.setVisibility(View.VISIBLE);
        attachmentTv.setText(data.attachment.getDisplayName());
      } else {
        imageView.setVisibility(View.GONE);
        attachmentTv.setVisibility(View.GONE);
      }
    }
  }
}
