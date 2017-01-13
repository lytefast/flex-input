package com.lytefast.flexinput.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lytefast.flexinput.R;
import com.lytefast.flexinput.adapters.FileListAdapter;
import com.lytefast.flexinput.adapters.OnItemClickListener;
import com.lytefast.flexinput.events.ClearAttachmentsEvent;
import com.lytefast.flexinput.events.ItemClickedEvent;
import com.lytefast.flexinput.model.Attachment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;


/**
 * Fragment that displays the recent files for selection.
 *
 * @author Sam Shih
 */
public class FilesFragment extends Fragment {
  private RecyclerView recyclerView;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public FilesFragment() {
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_recycler_view, container, false);

    if (view instanceof RecyclerView) {
      recyclerView = (RecyclerView) view;
      DividerItemDecoration bottomPadding =
          new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
      recyclerView.addItemDecoration(bottomPadding);
      recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }
    return view;
  }

  @Override
  public void onStart() {
    super.onStart();
    loadDownloadFolder();
    EventBus.getDefault().register(this);
  }

  @Override
  public void onStop() {
    EventBus.getDefault().unregister(this);
    super.onStop();
  }

  private void loadDownloadFolder() {
    File downloadFolder =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    FileListAdapter adapter = new FileListAdapter(getContext().getContentResolver(), downloadFolder);
    adapter.setOnItemClickListener(new OnItemClickListener<File>() {
      @Override
      public void onItemClicked(final File item) {
          EventBus.getDefault().post(new ItemClickedEvent<>(transformFileToAttachment(item)));
      }
    });
    recyclerView.setAdapter(adapter);
  }

  @NonNull
  private Attachment transformFileToAttachment(final File f) {
    return new Attachment(f.hashCode(), Uri.fromFile(f), f.getName());
  }

  @Subscribe
  void handleClearAttachmentEvent(ClearAttachmentsEvent evt) {
    ((FileListAdapter) recyclerView.getAdapter()).clearSelectedItems();
  }
}
