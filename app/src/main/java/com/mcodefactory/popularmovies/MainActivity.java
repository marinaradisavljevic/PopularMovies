package com.mcodefactory.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mcodefactory.popularmovies.adapters.RVAdapter;
import com.mcodefactory.popularmovies.content_provider.FavoriteMoviesContract;
import com.mcodefactory.popularmovies.data.Movie;
import com.mcodefactory.popularmovies.api_result_models.MovieResult;
import com.mcodefactory.popularmovies.utils.ApiInterface;
import com.mcodefactory.popularmovies.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements RVAdapter.RVAdapterOnClickHandler,
        LoaderManager.LoaderCallbacks<Cursor> {


    private RecyclerView mRecyclerView;
    public RVAdapter mAdapter;
    private EndlessScrollListener scrollListener;
    private static final int TASK_LOADER_ID = 0;
    private TextView mErrorMessageDisplay;
    // LayoutManager key and object used to preserve state after the device orientation change
    private static final String SAVED_LAYOUT_MANAGER = "LAYOUT_MANAGER_STATE";
    GridLayoutManager layoutManager;
    private Parcelable layoutManagerSavedState;

    private static final String TAG = MainActivity.class.getSimpleName();

    //local variable used to trigger changes in the selection criterion from top rated to most popular to favorites
    public static String choice;
    public static final String CHOICE_KEY = "choice";
    public static final String POPULAR = "popular";
    public static final String TOP_RATED = "top_rated";
    public static final String FAVORITES = "favorites";

    public static final String LANGUAGE = "en_US";
    public static final String MOVIE_KEY = "movie";
    private static final String MOVIES_KEY = "movies";
    public static final int POSTER_WIDTH = 120;
    ApiInterface apiInterface;
    private Context context;
    List<Movie> movies;
    private boolean isRestored;
    // starting value for the number of result pages API offers
    final int[] totalPages = {1};


    //used to dynamically calculate the number of columns that can be displayed on the device
    public static int calculateNoOfColumns(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (dpWidth / POSTER_WIDTH);
        return noOfColumns;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        movies = new ArrayList<>();
        isRestored = false;
        apiInterface = NetworkUtils.getClient().create(ApiInterface.class);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mErrorMessageDisplay = (TextView) findViewById(R.id.tv_error_message_display);
        if (layoutManager == null) {
            int noOfColumns = calculateNoOfColumns(getApplicationContext());
            layoutManager = new GridLayoutManager(MainActivity.this, noOfColumns, GridLayoutManager.VERTICAL, false);
        }
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setSaveEnabled(true);
        if (mAdapter == null) {
            mAdapter = new RVAdapter(this);
        }

        mRecyclerView.setAdapter(mAdapter);
        if (savedInstanceState != null) {
            if (savedInstanceState.get(CHOICE_KEY) != null && savedInstanceState.getParcelableArrayList(MOVIES_KEY) != null) {
                choice = savedInstanceState.getString(CHOICE_KEY);
            }
        }
        if (scrollListener == null) {
            scrollListener = new EndlessScrollListener(layoutManager) {
                @Override
                public boolean onLoadMore(int page, int totalItemsCount) {
                    loadMoreData(page);
                    return true;
                }
            };
        }
        mRecyclerView.addOnScrollListener(scrollListener);
        Log.d(TAG, "Going through onCreate there are " + movies.size() + " movies");
    }

    private void loadMoreData(int page) {
        final int previousMovieCount = movies.size();
        if (totalPages[0] < page || page > EndlessScrollListener.MAX_PAGES) {
            return;
        }
        if (choice.equals(POPULAR)) {
            Call<MovieResult> call = apiInterface.getMostPopularMovies(NetworkUtils.KEY,
                    LANGUAGE, page);
            call.enqueue(new Callback<MovieResult>() {
                @Override
                public void onResponse(Call<MovieResult> call, Response<MovieResult> response) {
                    List<Movie> moviesPerPage = response.body().getResults();
                    parseAndAddToCollection(moviesPerPage);

                    Log.d(TAG, "Number of movies received: " + movies.size());
                    if (movies.size() > previousMovieCount) {
                        showMovieDataView();
                        //load the collection in the adapter
                        mAdapter.setData(movies);
                    }
                }

                @Override
                public void onFailure(Call<MovieResult> call, Throwable t) {
                    Toast.makeText(context, R.string.error_loading_more_data, Toast.LENGTH_SHORT).show();
                }
            });
        } else if (choice.equals(TOP_RATED)) {
            Call<MovieResult> call = apiInterface.getTopRatedMovies(NetworkUtils.KEY,
                    LANGUAGE, page);
            call.enqueue(new Callback<MovieResult>() {
                @Override
                public void onResponse(Call<MovieResult> call, Response<MovieResult> response) {
                    List<Movie> moviesPerPage = response.body().getResults();
                    parseAndAddToCollection(moviesPerPage);

                    Log.d(TAG, "Number of movies received: " + movies.size());
                    if (movies.size() > previousMovieCount) {
                        showMovieDataView();
                        //load the collection in the adapter
                        mAdapter.setData(movies);
                    }
                }

                @Override
                public void onFailure(Call<MovieResult> call, Throwable t) {
                    Toast.makeText(context, R.string.error_loading_more_data, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void showMovieDataView() {
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showErrorMessage() {
        mRecyclerView.setVisibility(View.INVISIBLE);
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }

    private void loadMovieData(String choice) {
        showMovieDataView();
        //check if choice is Favorites, if so, load the collection from the local db
        if (choice.equals(FAVORITES)) {
            Log.d(TAG, "Going through loadMovieData, no movies in the Favorites page, loading from storage...");
            getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, this);

            //if choice is most popular or top rated, call the Retrofit client to fetch collection
        } else {
            Log.d(TAG, "Going through loadMovieData, no movies loaded, start loading from API...");
            // load the first page
            int pageToLoad = EndlessScrollListener.STARTING_PAGE_INDEX;
            Call<MovieResult> call;
            if (choice.equals(POPULAR)) {
                call = apiInterface.getMostPopularMovies(NetworkUtils.KEY,
                        LANGUAGE, pageToLoad);
            } else {
                call = apiInterface.getTopRatedMovies(NetworkUtils.KEY,
                        LANGUAGE, pageToLoad);
            }
            call.enqueue(new Callback<MovieResult>() {
                @Override
                public void onResponse(Call<MovieResult> call, Response<MovieResult> response) {
                    // retrieve the total number of result pages API offers
                    totalPages[0] = response.body().getTotalPages();
                    Log.d(TAG, "Total pages: " + totalPages[0]);
                    List<Movie> moviesPerPage = response.body().getResults();
                    parseAndAddToCollection(moviesPerPage);

                    Log.d(TAG, "Number of movies received: " + movies.size());
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
                    if (!NetworkUtils.isNetworkAvailable(context)) {
                        mErrorMessageDisplay.setText(R.string.no_internet_connection_message);
                        showErrorMessage();
                    }
                    Log.e(TAG, t.toString());
                }
            });

        }
    }


    private void setReleaseYear(Movie movie) {
        movie.setReleaseYear(movie.getReleaseDate());
    }

    private void parseAndAddToCollection(List<Movie> result) {
        for (Movie movie : result) {
            //for each Movie object, parse its release year and store it in the object field
            setReleaseYear(movie);
            movies.add(movie);
        }
    }


    @Override
    public void onItemClick(Movie movie) {
        Intent intent = new Intent();
        Context context = this;
        Class activityClass = MovieDetails.class;
        intent.setClass(context, activityClass);
        //send the Movie object to the Details activity
        intent.putExtra(MOVIE_KEY, movie);
        intent.putExtra(CHOICE_KEY, choice);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem sMenu = menu.findItem(R.id.spinner);

        final Spinner spinner = (Spinner) MenuItemCompat.getActionView(sMenu);

        //load the Spinner options from a String array
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (choice != null) {
            spinner.setSelection(getChoicePosition(choice));
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                if (isRestored) {
                    isRestored = false;
                    return;
                }
                Object item = adapterView.getItemAtPosition(position);
                if (item != null) {
                    mAdapter.setData(null);
                    movies.clear();
                    if (position == 0) {
                        choice = POPULAR;
                    }
                    if (position == 1) {
                        choice = TOP_RATED;
                    }
                    if (position == 2) {
                        choice = FAVORITES;

                    }
                    loadMovieData(choice);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }

        });

        return true;
    }

    //links an int position value to the String value of choice and returns that position
    private int getChoicePosition(String choice) {
        int position = -1;
        if (choice.equals(POPULAR)) {
            position = 0;
        }
        if (choice.equals(TOP_RATED)) {
            position = 1;
        }
        if (choice.equals(FAVORITES)) {
            position = 2;
        }
        return position;
    }


    //Loader that obtains the Favorites collection Cursor object
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        return new AsyncTaskLoader<Cursor>(this) {
            Cursor favoritesData = null;

            @Override
            protected void onStartLoading() {
                if (favoritesData != null) {
                    deliverResult(favoritesData);
                } else {
                    forceLoad();
                }
            }

            @Override
            public Cursor loadInBackground() {
                try {
                    favoritesData = getContentResolver().query(FavoriteMoviesContract.MovieEntry.CONTENT_URI, null, null, null, null);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to asynchronously load data from Favorites.");
                    e.printStackTrace();
                    return null;
                }
                return favoritesData;
            }

            public void deliverResult(Cursor data) {
                //if the Favorites collection contains no data, show an error message
                if (data == null || data.getCount() == 0) {
                    mErrorMessageDisplay.setText(R.string.no_favorites);
                    showErrorMessage();
                } else {
                    //reads to Cursor data into a list of movies for the adapter
                    movies = loadCursorDataToObjects(data);
                }

                favoritesData = data;
                super.deliverResult(data);

            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //sets the adapter data and restores the scroll position
        mAdapter.setData(movies);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    // iterates of the Cursor data and loads it into a Movie array
    public List<Movie> loadCursorDataToObjects(Cursor data) {
        int noOfMovies = data.getCount();
        List<Movie> movies = new ArrayList<>();
        for (int i = 0; i < noOfMovies; i++) {
            Movie movie = new Movie();
            data.moveToPosition(i);
            //set the Movie's isFavorite to true
            movie.setFavorite(true);

            int movieDbIndex = data.getColumnIndex(FavoriteMoviesContract.MovieEntry.MOVIEDB_ID);
            movie.setId(data.getInt(movieDbIndex));

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

            movies.add(movie);
        }
        return movies;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //save the scroll position in the LayoutManager
        outState.putParcelable(SAVED_LAYOUT_MANAGER, mRecyclerView.getLayoutManager().onSaveInstanceState());
        // save the currently loaded list of movie objects
        outState.putParcelableArrayList(MOVIES_KEY, (ArrayList<Movie>) movies);
        Log.d(TAG, "Going through onSaveInstanceState saved " + movies.size() + " movies");
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        if (state != null) {
            Bundle bundle = state;
            //restore the LayoutManager's state (i.e. scroll position)
            layoutManagerSavedState = bundle.getParcelable(SAVED_LAYOUT_MANAGER);
            // restore the list of previously loaded movies
            movies = bundle.getParcelableArrayList(MOVIES_KEY);
            Log.d(TAG, "Going through onRestoreInstanceState retrieved " + movies.size() + " movies");
            mAdapter.setData(movies);
            mRecyclerView.getLayoutManager().onRestoreInstanceState(layoutManagerSavedState);
            isRestored = true;
        }
        super.onRestoreInstanceState(state);
    }

}
