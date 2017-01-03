package com.lytefast.fancyinput.fragment;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.lytefast.fancyinput.R;

import java.util.List;


/**
 * TODO: Replace the implementation with code for your data type.
 */
public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

  private final List<?> mValues;

  public Adapter(List<?> items) {
    mValues = items;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.fragment_imageview, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(final ViewHolder holder, int position) {
    // do nothing
  }

  @Override
  public int getItemCount() {
    return mValues.size();
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    public final View mView;
    public final ImageView mContentView;

    public ViewHolder(View view) {
      super(view);
      mView = view;
      mContentView = (ImageView) view.findViewById(R.id.content_iv);
    }
  }
}
