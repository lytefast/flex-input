package com.lytefast.flexinput.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lytefast.flexinput.R;
import com.lytefast.flexinput.adapters.FileListAdapter;
import com.lytefast.flexinput.events.ClearAttachmentsEvent;
import com.lytefast.flexinput.events.ItemClickedEvent;
import com.lytefast.flexinput.model.Attachment;
import com.lytefast.flexinput.utils.AttachmentUtils;
import com.lytefast.flexinput.utils.SelectionCoordinator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayList;


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
    recyclerView.setAdapter(new FileListAdapter(
        getContext().getContentResolver(), downloadFolder, selectionCoordinator));
  }

  @Subscribe
  void handleClearAttachmentEvent(ClearAttachmentsEvent evt) {
    selectionCoordinator.clearSelectedItems();
  }

  private final SelectionCoordinator<File> selectionCoordinator = new SelectionCoordinator<File>() {
    @Override
    public void onItemSelected(final File item) {
      EventBus.getDefault().post(new ItemClickedEvent<>(AttachmentUtils.fromFile(item)));
      Log.d(getClass().getCanonicalName(), "Select: " + item.getPath());
    }

    @Override
    public void onItemUnselected(final File item) {
      EventBus.getDefault().post(new ItemClickedEvent<>(AttachmentUtils.fromFile(item)));
      Log.d(getClass().getCanonicalName(), "Remove: " + item.getPath());
    }
  };
}
