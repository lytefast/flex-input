package com.lytefast.flexinput.fragment;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lytefast.flexinput.FlexInput;
import com.lytefast.flexinput.R;
import com.lytefast.flexinput.R2;
import com.lytefast.flexinput.adapters.FileListAdapter;
import com.lytefast.flexinput.events.ClearAttachmentsEvent;
import com.lytefast.flexinput.managers.EventManager;
import com.lytefast.flexinput.utils.FileUtils;
import com.lytefast.flexinput.utils.SelectionCoordinator;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


/**
 * Fragment that displays the recent files for selection.
 *
 * @author Sam Shih
 */
public class FilesFragment extends Fragment {

  @BindView(R2.id.swipeRefreshLayout) SwipeRefreshLayout swipeRefreshLayout;
  @BindView(R2.id.list) RecyclerView recyclerView;
  private Unbinder unbinder;
  private FileListAdapter adapter;
  private EventManager.Bus<ClearAttachmentsEvent> bus;

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
    unbinder = ButterKnife.bind(this, view);

    DividerItemDecoration bottomPadding =
        new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
    recyclerView.addItemDecoration(bottomPadding);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

    adapter = new FileListAdapter(getContext().getContentResolver(), selectionCoordinator);
    recyclerView.setAdapter(adapter);

    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override
      public void onRefresh() {
        loadDownloadFolder();
      }
    });
    return view;
  }

  @Override
  public void onStart() {
    super.onStart();
    loadDownloadFolder();
    bus = FlexInput.eventManager.register(this, ClearAttachmentsEvent.class)
        .using(new EventManager.EventCallback<ClearAttachmentsEvent>() {
          @Override
          public void onEvent(ClearAttachmentsEvent event) {
            selectionCoordinator.clearSelectedItems();
          }
        });
  }

  @Override
  public void onStop() {
    bus.unregister();
    super.onStop();
  }

  private void loadDownloadFolder() {
    File downloadFolder =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    adapter.load(downloadFolder);
    swipeRefreshLayout.setRefreshing(false);
  }

  private final SelectionCoordinator<File> selectionCoordinator = new SelectionCoordinator<File>() {
    @Override
    public void onItemSelected(final File item) {
      FlexInput.eventManager.postOnItemClicked(FileUtils.toAttachment(item));
      Log.d(getClass().getCanonicalName(), "Select: " + item.getPath());
    }

    @Override
    public void onItemUnselected(final File item) {
      FlexInput.eventManager.postOnItemClicked(FileUtils.toAttachment(item));
      Log.d(getClass().getCanonicalName(), "Remove: " + item.getPath());
    }
  };
}
