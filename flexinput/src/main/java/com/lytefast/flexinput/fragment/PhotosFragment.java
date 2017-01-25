package com.lytefast.flexinput.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.lytefast.flexinput.FlexInputCoordinator;
import com.lytefast.flexinput.R;
import com.lytefast.flexinput.R2;
import com.lytefast.flexinput.adapters.EmptyListAdapter;
import com.lytefast.flexinput.adapters.PhotoCursorAdapter;
import com.lytefast.flexinput.model.Photo;
import com.lytefast.flexinput.utils.SelectionCoordinator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


/**
 * Fragment that displays the recent photos on the phone for selection.
 *
 * @author Sam Shih
 */
public class PhotosFragment extends PermissionsFragment {

  private static final String REQUIRED_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;

  private final SelectionCoordinator<Photo> selectionCoordinator = new SelectionCoordinator<>();

  @BindView(R2.id.swipeRefreshLayout) SwipeRefreshLayout swipeRefreshLayout;
  @BindView(R2.id.list) RecyclerView recyclerView;
  private Unbinder unbinder;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public PhotosFragment() {
  }

  @Override
  public void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Fragment parentFrag = getParentFragment();
    if (parentFrag instanceof FlexInputCoordinator) {
      FlexInputCoordinator flexInputCoordinator = (FlexInputCoordinator) parentFrag;
      flexInputCoordinator.addSelectionCoordinator(selectionCoordinator);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_recycler_view, container, false);
    unbinder = ButterKnife.bind(this, view);

    final PhotoCursorAdapter photoAdapter = new PhotoCursorAdapter(
        getContext().getContentResolver(), selectionCoordinator);

    if (hasPermissions(REQUIRED_PERMISSION)) {
      recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
      recyclerView.setAdapter(photoAdapter);
    } else {
      View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
          requestPermissions(photoAdapter);
        }
      };
      recyclerView.setAdapter(newPermissionRequestAdapter(onClickListener));
    }

    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override
      public void onRefresh() {
        photoAdapter.loadPhotos();
        swipeRefreshLayout.setRefreshing(false);
      }
    });
    return view;
  }

  protected EmptyListAdapter newPermissionRequestAdapter(final View.OnClickListener onClickListener) {
    return new EmptyListAdapter(
        R.layout.item_permission_storage, R.id.permissions_req_btn, onClickListener);
  }

  @Override
  public void onDestroyView() {
    unbinder.unbind();
    super.onDestroyView();
  }

  private void requestPermissions(final PhotoCursorAdapter photoAdapter) {
    requestPermissions(new PermissionsResultCallback() {
      @Override
      public void granted() {
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(photoAdapter);
        recyclerView.invalidateItemDecorations();
      }

      @Override
      public void denied() {
        Toast.makeText(
            getContext(), R.string.files_permission_reason_msg, Toast.LENGTH_LONG).show();
      }
    }, REQUIRED_PERMISSION);
  }
}