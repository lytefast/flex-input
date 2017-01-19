package com.lytefast.flexinput.sampleapp;

import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * Simple string message display adapter.
 * @author Sam Shih
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
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

  static class ViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.message_tv)
    TextView messageTv;

    public ViewHolder(final View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void bind(Editable data) {
      messageTv.setText(data);
    }
  }
}
