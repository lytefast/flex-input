package com.lytefast.flexinput.fragment

import android.Manifest
import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.lytefast.flexinput.FlexInputCoordinator
import com.lytefast.flexinput.R
import com.lytefast.flexinput.adapters.EmptyListAdapter
import com.lytefast.flexinput.adapters.PhotoCursorAdapter
import com.lytefast.flexinput.model.Attachment
import com.lytefast.flexinput.model.Photo
import com.lytefast.flexinput.utils.SelectionCoordinator


/**
 * Fragment that displays the recent photos on the phone for selection.
 *
 * @author Sam Shih
 */
/**
 * Mandatory empty constructor for the fragment manager to instantiate the
 * fragment (e.g. upon screen orientation changes).
 */
open class PhotosFragment : PermissionsFragment() {

  private var selectionCoordinator: SelectionCoordinator<Attachment<Any>, Photo>? = null

  internal var swipeRefreshLayout: SwipeRefreshLayout? = null
  internal var recyclerView: RecyclerView? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    this.selectionCoordinator = SelectionCoordinator()

    val targetFragment = parentFragment?.parentFragment as? FlexInputCoordinator<Any>
    targetFragment?.also {
      val selectionAgg = targetFragment.selectionAggregator
      selectionAgg.registerSelectionCoordinator(selectionCoordinator!!)

    }

    val view = inflater.inflate(R.layout.fragment_recycler_view, container, false)
    return view?.apply {
      recyclerView = findViewById(R.id.list)

      val photoAdapter = PhotoCursorAdapter(context.contentResolver, selectionCoordinator!!)

      if (hasPermissions(REQUIRED_PERMISSION)) {
        recyclerView?.layoutManager = GridLayoutManager(context, 3)
        recyclerView?.adapter = photoAdapter
      } else {
        recyclerView?.adapter = newPermissionsRequestAdapter(
            View.OnClickListener { requestPermissions(photoAdapter) })
      }

      swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
      swipeRefreshLayout?.setOnRefreshListener {
        if (hasPermissions(REQUIRED_PERMISSION)) {
          photoAdapter.loadPhotos()
        }
        swipeRefreshLayout?.isRefreshing = false
      }
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
  protected open fun newPermissionsRequestAdapter(onClickListener: View.OnClickListener): EmptyListAdapter {
    return EmptyListAdapter(
        R.layout.item_permission_storage, R.id.permissions_req_btn, onClickListener)
  }

  override fun onDestroyView() {
    selectionCoordinator!!.close()
    super.onDestroyView()
  }

  private fun requestPermissions(photoAdapter: PhotoCursorAdapter) {
    requestPermissions(object : PermissionsFragment.PermissionsResultCallback {
      override fun granted() {
        recyclerView!!.layoutManager = GridLayoutManager(context, 3)
        recyclerView!!.adapter = photoAdapter
        recyclerView!!.invalidateItemDecorations()
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