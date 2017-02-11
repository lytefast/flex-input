package com.lytefast.flexinput.sampleapp.fragment;

import android.Manifest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.lytefast.flexinput.FlexInputCoordinator;
import com.lytefast.flexinput.R2;
import com.lytefast.flexinput.adapters.EmptyListAdapter;
import com.lytefast.flexinput.fragment.PermissionsFragment;
import com.lytefast.flexinput.model.Attachment;
import com.lytefast.flexinput.sampleapp.R;
import com.lytefast.flexinput.sampleapp.model.GiphyImage;
import com.lytefast.flexinput.sampleapp.model.GiphyImageWrapper;
import com.lytefast.flexinput.sampleapp.model.GiphyImagesMetadata;
import com.lytefast.flexinput.sampleapp.model.GiphyResponse;
import com.lytefast.flexinput.sampleapp.services.GiphyService;
import com.lytefast.flexinput.utils.SelectionAggregator;
import com.lytefast.flexinput.utils.SelectionCoordinator;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * @author Sam Shih
 */
public class GiphyFragment extends PermissionsFragment {

  private static final String REQUIRED_PERMISSION = Manifest.permission.INTERNET;

  private final SelectionCoordinator<Attachment<File>> selectionCoordinator = new SelectionCoordinator<>();

  @BindView(R2.id.swipeRefreshLayout) SwipeRefreshLayout swipeRefreshLayout;
  @BindView(R2.id.list) RecyclerView recyclerView;
  private Unbinder unbinder;

  private Adapter adapter;
  private final GiphyService giphyService;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public GiphyFragment() {
    Retrofit retrofit = createGiphyRetrofit();
    this.giphyService = retrofit.create(GiphyService.class);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    final Fragment targetFragment = getParentFragment().getTargetFragment();
    if (targetFragment instanceof FlexInputCoordinator) {
      FlexInputCoordinator flexInputCoordinator = (FlexInputCoordinator) targetFragment;

      SelectionAggregator selectionAgg = flexInputCoordinator.getSelectionAggregator();
      selectionAgg.registerSelectionCoordinator(selectionCoordinator);
    }

    View view = inflater.inflate(com.lytefast.flexinput.R.layout.fragment_recycler_view, container, false);
    unbinder = ButterKnife.bind(this, view);

    recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

    adapter = new Adapter(selectionCoordinator);
    if (hasPermissions(REQUIRED_PERMISSION)) {
      recyclerView.setAdapter(adapter);
    } else {
      recyclerView.setAdapter(newPermissionsRequestAdapter(new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
          requestPermissions();
        }
      }));
    }

    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override
      public void onRefresh() {
        load();
      }
    });

    getLoaderManager().initLoader(0, savedInstanceState,
        createLoaderCallback()).forceLoad();

    return view;
  }

  /**
   * Provides an adapter that is shown when the fragment doesn't have the necessary permissions.
   * Override this for a more customized UX.
   *
   * @param onClickListener listener to be triggered when the user requests permissions.
   *
   * @return {@link RecyclerView.Adapter} shown when user has no permissions.
   * @see EmptyListAdapter
   */
  protected EmptyListAdapter newPermissionsRequestAdapter(final View.OnClickListener onClickListener) {
    return new EmptyListAdapter(
        com.lytefast.flexinput.R.layout.item_permission_storage, com.lytefast.flexinput.R.id.permissions_req_btn, onClickListener);
  }

  @Override
  public void onStart() {
    super.onStart();
    load();
  }

  @Override
  public void onDestroyView() {
    unbinder.unbind();
    selectionCoordinator.close();
    super.onDestroyView();
  }

  private void load() {
    if (adapter == null) {
      swipeRefreshLayout.setRefreshing(false);
    }
    swipeRefreshLayout.setRefreshing(true);
    getLoaderManager().getLoader(0).forceLoad();
  }

  private void requestPermissions() {
    requestPermissions(new PermissionsFragment.PermissionsResultCallback() {
      @Override
      public void granted() {
        recyclerView.setAdapter(adapter);
        load();
      }

      @Override
      public void denied() {
        Toast.makeText(
            getContext(), com.lytefast.flexinput.R.string.files_permission_reason_msg, Toast.LENGTH_LONG).show();
      }
    }, REQUIRED_PERMISSION);
  }

  class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private final SelectionCoordinator<Attachment<File>> selectionCoordinator;
    private List<GiphyImagesMetadata> data = Collections.EMPTY_LIST;

    public Adapter(final SelectionCoordinator<Attachment<File>> selectionCoordinator) {
      this.selectionCoordinator = selectionCoordinator;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
      View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.view_giphy_item, parent, false);
      return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
      holder.bind(data.get(position).images);
    }

    @Override
    public int getItemCount() {
      return data.size();
    }

    private void setData(final List<GiphyImagesMetadata> data) {
      this.data = data;
      notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.gif_iv) SimpleDraweeView gifIv;

      public ViewHolder(final View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
      }

      public void bind(GiphyImageWrapper imageWrapper) {
        GiphyImage image = imageWrapper.fixed_height_downsampled;

        DraweeController controller = Fresco.newDraweeControllerBuilder()
            .setUri(image.webp)
            .setAutoPlayAnimations(true)
            .build();
        gifIv.setController(controller);
      }
    }
  }

  @NonNull
  private LoaderManager.LoaderCallbacks<GiphyResponse> createLoaderCallback() {
    return new LoaderManager.LoaderCallbacks<GiphyResponse>() {
      @Override
      public Loader<GiphyResponse> onCreateLoader(final int id, final Bundle args) {
        return new AsyncTaskLoader<GiphyResponse>(getContext()) {
          @Override
          public GiphyResponse loadInBackground() {
            Call<GiphyResponse> call = giphyService.search("yolo", 10, 0, null);
            try {
              retrofit2.Response<GiphyResponse> response = call.execute();
              return response.body();
            } catch (IOException e) {
              Log.e(getClass().getName(), "could not load giphy", e);
            }
            return null;
          }
        };
      }

      @Override
      public void onLoadFinished(final Loader<GiphyResponse> loader, final GiphyResponse data) {
        if (data != null) {
          adapter.setData(data.data);
        }
        swipeRefreshLayout.setRefreshing(false);
      }

      @Override
      public void onLoaderReset(final Loader<GiphyResponse> loader) {
        adapter.setData(Collections.EMPTY_LIST);
      }
    };
  }

  @NonNull
  private static Retrofit createGiphyRetrofit() {
    OkHttpClient.Builder httpClient =
        new OkHttpClient.Builder();
    httpClient.addInterceptor(new Interceptor() {
      @Override
      public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        HttpUrl originalHttpUrl = original.url();

        HttpUrl url = originalHttpUrl.newBuilder()
            .addQueryParameter("api_key", "dc6zaTOxFJmzC")  // beta test key
            .build();

        // Request customization: add request headers
        Request.Builder requestBuilder = original.newBuilder()
            .url(url);

        Request request = requestBuilder.build();
        return chain.proceed(request);
      }
    });

    // Request customization: add request headers
    return new Retrofit.Builder()
        .baseUrl("http://api.giphy.com")
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient.build())
        .build();
  }
}
