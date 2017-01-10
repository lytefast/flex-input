package com.lytefast.flexinput.fragment;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lytefast.flexinput.R;
import com.lytefast.flexinput.adapters.FileCursorAdapter;


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
    loadRecentFiles();
  }

  private void loadRecentFiles() {
    final ContentResolver contentResolver = getContext().getContentResolver();

    String selectionFilter = MediaStore.Files.FileColumns.MEDIA_TYPE + "=?";
    String[] selectionArgs = {String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE)};

    final Cursor cursor = contentResolver.query(
        MediaStore.Files.getContentUri("external"),
        new String[]{
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.TITLE,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.PARENT},
        selectionFilter, selectionArgs,
        MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC");

    if (cursor != null) {
      String[] colNames = cursor.getColumnNames();
      while (cursor.moveToNext()) {
        long id = cursor.getLong(0);
        String data = cursor.getString(1);
        String type = cursor.getString(2);
        long modified = cursor.getLong(3);
        String title = cursor.getString(4);
        String name = cursor.getString(5);
        String parent = cursor.getString(6);
        Log.v("TEST", String.format("Result %s %s %s %s %s %s", parent, data, type, modified, title, name));
      }
    }

    recyclerView.setAdapter(new FileCursorAdapter(contentResolver, cursor));
  }
}
