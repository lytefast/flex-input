package com.lytefast.flexinput.adapters;

import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import butterknife.ButterKnife;


/**
 * Simple {@link android.support.v7.widget.RecyclerView.Adapter} which just renders one item entry.
 * @author Sam Shih
 */
public class EmptyListAdapter extends RecyclerView.Adapter<EmptyListAdapter.ViewHolder> {

  @LayoutRes private final int itemLayoutId;
  @IdRes private final int actionBtnId;

  private final View.OnClickListener onClickListener;

  public EmptyListAdapter(
      @LayoutRes final int itemLayoutId,
      @IdRes final int actionBtnId,
      View.OnClickListener onClickListener) {
    this.itemLayoutId = itemLayoutId;
    this.actionBtnId = actionBtnId;
    this.onClickListener = onClickListener;
  }

  @Override
  public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(itemLayoutId, parent, false);
    return new EmptyListAdapter.ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(final ViewHolder holder, final int position) {
    holder.actionBtn.setOnClickListener(onClickListener);
  }

  @Override
  public int getItemCount() {
    return 1;
  }

  protected class ViewHolder extends RecyclerView.ViewHolder {

    private final Button actionBtn;

    public ViewHolder(final View itemView) {
      super(itemView);
      actionBtn = ButterKnife.findById(itemView, actionBtnId);
    }
  }
}
