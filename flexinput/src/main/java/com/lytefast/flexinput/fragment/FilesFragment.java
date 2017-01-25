package com.lytefast.flexinput.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.lytefast.flexinput.FlexInputCoordinator;
import com.lytefast.flexinput.R;
import com.lytefast.flexinput.R2;
import com.lytefast.flexinput.adapters.EmptyListAdapter;
import com.lytefast.flexinput.adapters.FileListAdapter;
import com.lytefast.flexinput.managers.PermissionsManager;
import com.lytefast.flexinput.model.Generic;
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

  private final SelectionCoordinator<Generic<File>> selectionCoordinator = new SelectionCoordinator<>();

  @BindView(R2.id.swipeRefreshLayout) SwipeRefreshLayout swipeRefreshLayout;
  @BindView(R2.id.list) RecyclerView recyclerView;
  private Unbinder unbinder;

  private FileListAdapter adapter;

  private PermissionsManager permissionsManager;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public FilesFragment() {
  }

  @Override
  public void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Fragment parentFrag = getParentFragment();
    if (parentFrag instanceof FlexInputCoordinator) {
      FlexInputCoordinator flexInputCoordinator = (FlexInputCoordinator) parentFrag;
      flexInputCoordinator.addSelectionCoordinator(selectionCoordinator);
    }

    if (parentFrag instanceof PermissionsManager) {
      this.permissionsManager = (PermissionsManager) parentFrag;
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_recycler_view, container, false);
    unbinder = ButterKnife.bind(this, view);

    DividerItemDecoration bottomPadding =
        new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
    recyclerView.addItemDecoration(bottomPadding);

    final boolean canRead = ContextCompat.checkSelfPermission(
            getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    if (canRead) {
      adapter = new FileListAdapter(getContext().getContentResolver(), selectionCoordinator);
      recyclerView.setAdapter(adapter);
    } else {
      View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
          requestPermissions();
        }
      };
      recyclerView.setAdapter(new EmptyListAdapter(
          R.layout.item_permission_storage, R.id.permissions_req_btn, onClickListener));
    }

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
  }

  @Override
  public void onDestroyView() {
    unbinder.unbind();
    super.onDestroyView();
  }

  private void loadDownloadFolder() {
    if (adapter == null) {
      return;
    }
    File downloadFolder =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    adapter.load(downloadFolder);
    swipeRefreshLayout.setRefreshing(false);
  }

  private void requestPermissions() {
    permissionsManager.requestFileReadPermission(new PermissionsManager.PermissionsResultCallback() {
      @Override
      public void granted() {
        adapter = new FileListAdapter(getContext().getContentResolver(), selectionCoordinator);
        recyclerView.setAdapter(adapter);
        loadDownloadFolder();
      }

      @Override
      public void denied() {
        Toast.makeText(
            getContext(), R.string.files_permission_reason_msg, Toast.LENGTH_LONG).show();
      }
    });
  }
}
