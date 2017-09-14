package com.mcodefactory.popularmovies.fragments;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mcodefactory.popularmovies.EndlessScrollListener;
import com.mcodefactory.popularmovies.OnSelectMovieListener;
import com.mcodefactory.popularmovies.R;
import com.mcodefactory.popularmovies.adapters.RVAdapter;
import com.mcodefactory.popularmovies.content_provider.FavoriteMoviesContract;
import com.mcodefactory.popularmovies.data.Movie;

import java.util.ArrayList;
import java.util.List;


public class FavoritesFragment extends Fragment implements RVAdapter.RVAdapterOnClickHandler, LoaderManager.LoaderCallbacks<Cursor> {

    private RecyclerView mRecyclerView;
    public RVAdapter mAdapter;
    private TextView mErrorMessageDisplay;
    private static final int TASK_LOADER_ID = 0;
    private static final String NUMBER_OF_LOADED_FAVORITES = "number_of_loaded_favorites";
    private static final String LIMIT_KEY = "limit";
    private static final String MOVIE_LOCAL_ID_KEY = "movie_id";
    private boolean isRestored;
    // LayoutManager key and object used to preserve state after the device orientation change
    private static final String SAVED_LAYOUT_MANAGER = "LAYOUT_MANAGER_STATE";
    GridLayoutManager layoutManager;
    private Parcelable layoutManagerSavedState;

    public static final int POSTER_WIDTH = 120;
    List<Movie> movies;
    private int totalFavoritesInDB;
    OnSelectMovieListener listener;
    private EndlessScrollListener scrollListener;

    //used to dynamically calculate the number of columns that can be displayed on the device
    public static int calculateNoOfColumns(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        return (int) (dpWidth / POSTER_WIDTH);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        movies = new ArrayList<>();
        totalFavoritesInDB = -1;
        getActivity().getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, this);

        Bundle parametersBundle = new Bundle();
        parametersBundle.putInt(MOVIE_LOCAL_ID_KEY, 0);
        parametersBundle.putInt(LIMIT_KEY, 20);
        getActivity().getSupportLoaderManager().restartLoader(TASK_LOADER_ID, parametersBundle, this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.collection, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mErrorMessageDisplay = (TextView) view.findViewById(R.id.tv_error_message_display);
        int noOfColumns = calculateNoOfColumns(getActivity().getApplicationContext());
        layoutManager = new GridLayoutManager(getActivity(), noOfColumns, GridLayoutManager.VERTICAL,
                false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setSaveEnabled(true);
        mAdapter = new RVAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        scrollListener = new EndlessScrollListener(layoutManager) {
            @Override
            public boolean onLoadMore(int page, int totalItemsCount) {
                if (movies.size() / 20 >= page && movies.size() < totalFavoritesInDB) {
                    page = movies.size() / 20 + 1;
                }
                loadMoreData(page);
                return true;

            }
        };
        mRecyclerView.addOnScrollListener(scrollListener);
        if (savedInstanceState != null) {
            isRestored = true;
            layoutManagerSavedState = savedInstanceState.getParcelable(SAVED_LAYOUT_MANAGER);
            onRestoreInstanceState(savedInstanceState);
        }
        return view;
    }

    private void loadMoreData(int page) {
        if (movies.size() == 0) {
            return;
        }
        int lastMovieIndex = movies.size() - 1;
        Movie lastFavorite = movies.get(lastMovieIndex);
        Bundle parametersBundle = new Bundle();
        parametersBundle.putInt(MOVIE_LOCAL_ID_KEY, lastFavorite.getLocalID());
        parametersBundle.putInt(LIMIT_KEY, 20);
        getActivity().getSupportLoaderManager().restartLoader(TASK_LOADER_ID, parametersBundle, this);
    }

    @Override
    public void onItemClick(Movie movie) {
        listener.onMovieSelected(movie);
    }


    public int count(Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = getContext().getContentResolver().query(uri, new String[]{"count(*)"},
                selection, selectionArgs, null);
        if (cursor == null || cursor.getCount() == 0) {
            cursor.close();
            return 0;
        } else {
            cursor.moveToFirst();
            int result = cursor.getInt(0);
            cursor.close();
            return result;
        }
    }

    //Loader that obtains the Favorites collection Cursor object
    @Override
    public Loader<Cursor> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<Cursor>(getActivity()) {
            boolean fetchAll;
            int lastMovieId;
            int limit;
            Cursor favoritesData = null;

            @Override
            protected void onStartLoading() {
                if (args == null) {
                    fetchAll = true;
                } else {
                    if (isRestored) {
                        lastMovieId = 0;
                    } else {
                        lastMovieId = args.getInt(MOVIE_LOCAL_ID_KEY);
                    }
                    limit = args.getInt(LIMIT_KEY);
                }
                forceLoad();

            }

            @Override
            public Cursor loadInBackground() {
                if (fetchAll) {
                    try {
                        totalFavoritesInDB = count(FavoriteMoviesContract.MovieEntry.CONTENT_URI,
                                null, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                } else {
                    try {
                        String selection = "_ID>?";
                        String[] selectionArgs = {String.valueOf(lastMovieId)};
                        String limitConstraint = "_ID LIMIT " + limit;
                        favoritesData = getActivity().getContentResolver().query
                                (FavoriteMoviesContract.MovieEntry.CONTENT_URI, null, selection,
                                        selectionArgs, limitConstraint);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                return favoritesData;
            }

            public void deliverResult(Cursor data) {
                //if the Favorites collection contains no data, show an error message
                if (data == null || data.getCount() == 0) {
                    if (movies.size() == 0) {
                        mErrorMessageDisplay.setText(R.string.no_favorites);
                        showErrorMessage();
                    }
                } else {
                    //reads the Cursor data into a list of movies for the adapter
                    movies.addAll(loadCursorDataToObjects(data));
                    mAdapter.notifyDataSetChanged();
                }
                super.deliverResult(data);
            }
        };

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //sets the adapter data and restores the scroll position, if saved
        mAdapter.setData(movies);
        if (layoutManagerSavedState != null) {
            layoutManager.onRestoreInstanceState(layoutManagerSavedState);
        }
        showMovieDataView();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    // iterates of the Cursor data and loads it into a Movie array
    public List<Movie> loadCursorDataToObjects(Cursor data) {
        int noOfMovies = data.getCount();
        List<Movie> movieCollection = new ArrayList<>();
        for (int i = 0; i < noOfMovies; i++) {
            Movie movie = new Movie();
            data.moveToPosition(i);
            //set the Movie's isFavorite to true
            movie.setFavorite(true);

            int movieDbIndex = data.getColumnIndex(FavoriteMoviesContract.MovieEntry.MOVIEDB_ID);
            movie.setId(data.getInt(movieDbIndex));

            int localDbID = data.getColumnIndex(FavoriteMoviesContract.MovieEntry._ID);
            movie.setLocalID(data.getInt(localDbID));

            int runtimeIndex = data.getColumnIndex(FavoriteMoviesContract.MovieEntry.RUNTIME);
            movie.setRuntime(data.getInt(runtimeIndex));

            int titleIndex = data.getColumnIndex(FavoriteMoviesContract.MovieEntry.ORIGINAL_TITLE);
            movie.setOriginalTitle(data.getString(titleIndex));

            int voteIndex = data.getColumnIndex(FavoriteMoviesContract.MovieEntry.AVERAGE_VOTE);
            movie.setVoteAverage(data.getDouble(voteIndex));

            int releaseYearIndex = data.getColumnIndex(FavoriteMoviesContract.MovieEntry.RELEASE_YEAR);
            movie.releaseYear = data.getInt(releaseYearIndex);

            int synopsisIndex = data.getColumnIndex(FavoriteMoviesContract.MovieEntry.SYNOPSIS);
            movie.setSynopsis(data.getString(synopsisIndex));

            int posterNameIndex = data.getColumnIndex(FavoriteMoviesContract.MovieEntry.POSTER_IMAGE_NAME);
            movie.setPosterPath(data.getString(posterNameIndex));

            movieCollection.add(movie);
        }
        return movieCollection;
    }

    private void showErrorMessage() {
        mRecyclerView.setVisibility(View.INVISIBLE);
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }

    public void showMovieDataView() {
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
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
        outState.putParcelable(SAVED_LAYOUT_MANAGER, mRecyclerView.getLayoutManager().
                onSaveInstanceState());
        outState.putInt(NUMBER_OF_LOADED_FAVORITES, movies.size());
    }

    public void onRestoreInstanceState(Bundle state) {
        if (state != null) {
            movies = new ArrayList<>();
            int previoslyLoaded = state.getInt(NUMBER_OF_LOADED_FAVORITES);
            getActivity().getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, this);
            int limit;
            if (previoslyLoaded > totalFavoritesInDB) {
                limit = totalFavoritesInDB;
            } else {
                limit = previoslyLoaded;
            }
            Bundle parametersBundle = new Bundle();
            parametersBundle.putInt(LIMIT_KEY, limit);
            getActivity().getSupportLoaderManager().restartLoader(TASK_LOADER_ID, parametersBundle, this);
            isRestored = false;
        }
    }
}
