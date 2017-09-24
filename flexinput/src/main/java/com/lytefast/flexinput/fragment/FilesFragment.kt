package com.lytefast.flexinput.fragment

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.lytefast.flexinput.FlexInputCoordinator
import com.lytefast.flexinput.R
import com.lytefast.flexinput.adapters.EmptyListAdapter
import com.lytefast.flexinput.adapters.FileListAdapter
import com.lytefast.flexinput.model.Attachment
import com.lytefast.flexinput.utils.SelectionCoordinator
import java.io.File


/**
 * Fragment that displays the recent files for selection.
 *
 * @author Sam Shih
 */
/**
 * Mandatory empty constructor for the fragment manager to instantiate the
 * fragment (e.g. upon screen orientation changes).
 */
class FilesFragment : PermissionsFragment() {

  private var selectionCoordinator: SelectionCoordinator<Attachment<Any>, Attachment<File>>? = null

  internal var swipeRefreshLayout: SwipeRefreshLayout? = null
  internal var recyclerView: RecyclerView? = null

  private var adapter: FileListAdapter? = null

  override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    this.selectionCoordinator = SelectionCoordinator()

    val targetFragment = parentFragment?.parentFragment as? FlexInputCoordinator<Any>
    targetFragment?.also {
      val selectionAgg = targetFragment.selectionAggregator
      selectionAgg.registerSelectionCoordinator(selectionCoordinator!!)

    }

    val view = inflater?.inflate(R.layout.fragment_recycler_view, container, false)
    return view?.apply {
      recyclerView = findViewById(R.id.list)

      if (hasPermissions(REQUIRED_PERMISSION)) {
        adapter = FileListAdapter(context.contentResolver, selectionCoordinator!!)
        recyclerView?.adapter = adapter
      } else {
        recyclerView?.adapter = newPermissionsRequestAdapter(View.OnClickListener { requestPermissions() })
      }

      swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
      swipeRefreshLayout?.setOnRefreshListener(this@FilesFragment::loadDownloadFolder)
    }
  }

  /**
   * Provides an adapter that is shown when the fragment doesn't have the necessary permissions.
   * Override this for a more customized UX.
   *
   * @param onClickListener listener to be triggered when the user requests permissions.
   *
   * @return [RecyclerView.Adapter] shown when user has no permissions.
   * @see EmptyListAdapter
   */
  protected fun newPermissionsRequestAdapter(onClickListener: View.OnClickListener): EmptyListAdapter {
    return EmptyListAdapter(
        R.layout.item_permission_storage, R.id.permissions_req_btn, onClickListener)
  }

  override fun onStart() {
    super.onStart()
    loadDownloadFolder()
  }

  override fun onDestroyView() {
    selectionCoordinator?.close()
    super.onDestroyView()
  }

  private fun loadDownloadFolder() {
    if (adapter == null) {
      swipeRefreshLayout!!.isRefreshing = false
      return
    }
    val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    adapter!!.load(downloadFolder)
    swipeRefreshLayout!!.isRefreshing = false
  }

  private fun requestPermissions() {
    requestPermissions(object : PermissionsFragment.PermissionsResultCallback {
      override fun granted() {
        adapter = FileListAdapter(context.contentResolver, selectionCoordinator!!)
        recyclerView?.adapter = adapter
        loadDownloadFolder()
      }

      override fun denied() {
        Toast.makeText(
            context, R.string.files_permission_reason_msg, Toast.LENGTH_LONG).show()
      }
    }, REQUIRED_PERMISSION)
  }

  companion object {

    private val REQUIRED_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
  }
}
