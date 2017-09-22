package com.lytefast.flexinput.fragment;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.lytefast.flexinput.R;
import com.lytefast.flexinput.R2;
import com.lytefast.flexinput.model.Emoji;
import com.lytefast.flexinput.model.EmojiCategory;

import butterknife.BindView;


/**
 * Displays an assortment of Emojis for input.
 *
 * @author Sam Shih
 */
public class EmojiGridFragment extends Fragment {

  public static final String EMOJI_CATEGORY = "emoji_category";

  @BindView(R2.id.list) RecyclerView emojiGrid;

  private EmojiCategory emojiCategory;
  private FlexInputFragment flexInputFrag;

  @Override
  public void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Fragment parentFrag = getParentFragment();
    if (parentFrag instanceof FlexInputFragment) {
      flexInputFrag = (FlexInputFragment) parentFrag;
    }
  }

  @Nullable
  @Override
  public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
                           @Nullable final Bundle savedInstanceState) {
    emojiGrid = new RecyclerView(inflater.getContext(), null, R.style.FlexInput_Emoji_Page);

    final int numFittedColumns = calculateNumOfColumns(getResources());
    emojiGrid.setLayoutManager(new GridLayoutManager(getContext(), numFittedColumns));
    emojiGrid.setAdapter(new Adapter());

    if (savedInstanceState != null) {
      emojiCategory = savedInstanceState.getParcelable(EMOJI_CATEGORY);
    }

    return emojiGrid;
  }

  @Override
  public void onSaveInstanceState(final Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(EMOJI_CATEGORY, emojiCategory);
  }

  public EmojiGridFragment with(final EmojiCategory emojiCategory) {
    this.emojiCategory = emojiCategory;
    return this;
  }

  private class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
      View view = LayoutInflater.from(
          parent.getContext()).inflate(R.layout.view_emoji_item, parent, false);
      return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
      Emoji emoji = emojiCategory.getEmojis().get(position);
      holder.bind(emoji);
    }

    @Override
    public int getItemCount() {
      return emojiCategory.getEmojis().size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
      private final TextView textView;

      public ViewHolder(final View itemView) {
        super(itemView);
        textView = (TextView) itemView;
      }

      public void bind(final Emoji emoji) {
        textView.setText(emoji.getStrValue());
        textView.setContentDescription(emoji.getAliases()[0]);

        itemView.setOnLongClickListener(new View.OnLongClickListener() {
          @Override
          public boolean onLongClick(final View v) {
            Toast.makeText(getContext(), emoji.getAliases()[0], Toast.LENGTH_SHORT).show();
            return true;
          }
        });

        itemView.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(final View v) {
            flexInputFrag.append(emoji.getStrValue());
          }
        });
      }
    }
  }

  public static int calculateNumOfColumns(Resources resources) {
    DisplayMetrics displayMetrics = resources.getDisplayMetrics();
    int numCols =
        (int) (displayMetrics.widthPixels / resources.getDimension(R.dimen.emoji_grid_item_size));
    return numCols;
  }

}
