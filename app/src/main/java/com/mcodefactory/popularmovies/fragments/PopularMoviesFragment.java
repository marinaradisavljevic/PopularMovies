package com.mcodefactory.popularmovies.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mcodefactory.popularmovies.EndlessScrollListener;
import com.mcodefactory.popularmovies.OnSelectMovieListener;
import com.mcodefactory.popularmovies.R;
import com.mcodefactory.popularmovies.adapters.RVAdapter;
import com.mcodefactory.popularmovies.api_result_models.MovieResult;
import com.mcodefactory.popularmovies.data.Movie;
import com.mcodefactory.popularmovies.utils.ApiInterface;
import com.mcodefactory.popularmovies.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class PopularMoviesFragment extends Fragment implements RVAdapter.RVAdapterOnClickHandler {

    private RecyclerView mRecyclerView;
    public RVAdapter mAdapter;
    private TextView mErrorMessageDisplay;
    // LayoutManager key and object used to preserve state after the device orientation change
    private static final String SAVED_LAYOUT_MANAGER = "LAYOUT_MANAGER_STATE";
    private static final String PAGE_KEY = "scrollListenerCurrentPage";
    private static final String MOVIES_KEY = "movies";
    private static final String RESULT_PAGES_KEY = "result_pages_number";
    private GridLayoutManager layoutManager;
    private Parcelable layoutManagerSavedState;
    private static final int POSTER_WIDTH = 120;
    private ApiInterface apiInterface;
    private List<Movie> movies;
    public static final String LANGUAGE = "en_US";
    private int[] totalPages;
    private OnSelectMovieListener listener;
    private EndlessScrollListener scrollListener;


    //used to dynamically calculate the number of columns that can be displayed on the device
    private int calculateNoOfColumns(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        return (int) (dpWidth / POSTER_WIDTH);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        totalPages = new int[1];
        totalPages[0] = 1;
        movies = new ArrayList<>();
        apiInterface = NetworkUtils.getClient().create(ApiInterface.class);

        if (savedInstanceState == null) {

            Call<MovieResult> call = apiInterface.getMostPopularMovies(NetworkUtils.KEY,
                    LANGUAGE, EndlessScrollListener.STARTING_PAGE_INDEX);
            call.enqueue(new Callback<MovieResult>() {
                @Override
                public void onResponse(Call<MovieResult> call, Response<MovieResult> response) {
                    // retrieve the total number of result pages API offers
                    totalPages[0] = response.body().getTotalPages();
                    List<Movie> moviesPerPage = response.body().getResults();
                    parseAndAddToCollection(moviesPerPage);

                    if (movies.size() > 0) {
                        showMovieDataView();
                        //load the collection in the adapter
                        mAdapter.setData(movies);
                    } else {
                        //if there are no movies in the result page, show error message
                        mErrorMessageDisplay.setText(R.string.error_fetching_collection);
                        showErrorMessage();
                    }
                }

                @Override
                public void onFailure(Call<MovieResult> call, Throwable t) {
                    //check if there is an internet connection, if not, display a message
                    if (!NetworkUtils.isNetworkAvailable(getActivity())) {
                        mErrorMessageDisplay.setText(R.string.no_internet_connection_message);
                        showErrorMessage();
                    }
                }
            });
        }


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.collection, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mErrorMessageDisplay = (TextView) view.findViewById(R.id.tv_error_message_display);
        int noOfColumns = calculateNoOfColumns(getActivity().getApplicationContext());
        layoutManager = new GridLayoutManager(getActivity(), noOfColumns, GridLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setSaveEnabled(true);
        mAdapter = new RVAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        scrollListener = new EndlessScrollListener(layoutManager) {
            @Override
            public boolean onLoadMore(int page, int totalItemsCount) {
                if (totalItemsCount >= 100) {
                    return false;
                } else {
                    if (movies.size() / 20 >= page) {
                        page = movies.size() / 20 + 1;
                    }
                    loadMoreData(page);
                    return true;
                }
            }
        };
        mRecyclerView.addOnScrollListener(scrollListener);
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }
        return view;
    }

    private void loadMoreData(int page) {
        scrollListener.setCurrentPage(page);
        final int previousMovieCount = movies.size();
        if (totalPages[0] < page || page > EndlessScrollListener.MAX_PAGES) {
            return;
        }
        Call<MovieResult> call = apiInterface.getMostPopularMovies(NetworkUtils.KEY,
                LANGUAGE, page);
        call.enqueue(new Callback<MovieResult>() {
            @Override
            public void onResponse(Call<MovieResult> call, Response<MovieResult> response) {
                List<Movie> moviesPerPage = response.body().getResults();
                parseAndAddToCollection(moviesPerPage);

                if (movies.size() > previousMovieCount) {
                    showMovieDataView();
                    //load the collection in the adapter
                    mAdapter.setData(movies);
                }
            }

            @Override
            public void onFailure(Call<MovieResult> call, Throwable t) {
                Toast.makeText(getActivity(), R.string.error_loading_more_data,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(Movie movie) {
        listener.onMovieSelected(movie);
    }

    private void showErrorMessage() {
        mRecyclerView.setVisibility(View.INVISIBLE);
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }

    public void showMovieDataView() {
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void parseAndAddToCollection(List<Movie> result) {
        for (Movie movie : result) {
            //for each Movie object, parse its release year and store it in the object field
            setReleaseYear(movie);
            movies.add(movie);
        }
    }

    private void setReleaseYear(Movie movie) {
        movie.setReleaseYear(movie.getReleaseDate());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (OnSelectMovieListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " needs to implement the OnSelectMovieListener");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState != null) {
            outState.clear();
        }
        super.onSaveInstanceState(outState);
        //save the scroll position in the LayoutManager
        outState.putParcelable(SAVED_LAYOUT_MANAGER, mRecyclerView.getLayoutManager().onSaveInstanceState());
        // save the currently loaded list of movie objects
        outState.putParcelableArrayList(MOVIES_KEY, (ArrayList<Movie>) movies);
        outState.putInt(PAGE_KEY, scrollListener.getPage());
        outState.putInt(RESULT_PAGES_KEY, totalPages[0]);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // restore the list of previously loaded movies
            movies = savedInstanceState.getParcelableArrayList(MOVIES_KEY);
            mAdapter.setData(movies);
            mAdapter.notifyDataSetChanged();
            showMovieDataView();
            //restore the LayoutManager's state (i.e. scroll position)
            layoutManagerSavedState = savedInstanceState.getParcelable(SAVED_LAYOUT_MANAGER);
            mRecyclerView.getLayoutManager().onRestoreInstanceState(layoutManagerSavedState);
            totalPages[0] = savedInstanceState.getInt(RESULT_PAGES_KEY);
            scrollListener.setCurrentPage(savedInstanceState.getInt(PAGE_KEY));
            scrollListener.setLoading(false);
        }
    }
}
