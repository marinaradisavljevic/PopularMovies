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
import com.mcodefactory.popularmovies.adapters.RVAdapter;
import com.mcodefactory.popularmovies.content_provider.FavoriteMoviesContract;
import com.mcodefactory.popularmovies.data.Movie;
import com.mcodefactory.popularmovies.api_result_models.MovieResult;
import com.mcodefactory.popularmovies.utils.ApiInterface;
import com.mcodefactory.popularmovies.utils.NetworkUtils;
import java.util.Arrays;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements RVAdapter.RVAdapterOnClickHandler,
        LoaderManager.LoaderCallbacks<Cursor> {

    private RecyclerView mRecyclerView;
    public RVAdapter mAdapter;
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

    public static final String MOVIE_KEY = "movie";
    public static final int POSTER_WIDTH = 120;
    ApiInterface apiInterface;
    private Context context;

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

        apiInterface = NetworkUtils.getClient().create(ApiInterface.class);
        context = this;
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mErrorMessageDisplay = (TextView) findViewById(R.id.tv_error_message_display);
        int noOfColumns = calculateNoOfColumns(getApplicationContext());
        layoutManager = new GridLayoutManager(MainActivity.this, noOfColumns, GridLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setSaveEnabled(true);
        mAdapter = new RVAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        if (savedInstanceState!=null) {
            if (savedInstanceState.containsKey(CHOICE_KEY) && savedInstanceState.get(CHOICE_KEY)!=null) {
                String savedChoice = savedInstanceState.getString(CHOICE_KEY);
                choice = savedChoice;
            }
        } else {
            choice = POPULAR;
        }
        }

        public void showMovieDataView() {
            mErrorMessageDisplay.setVisibility(View.INVISIBLE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }

        private void showErrorMessage () {
            mRecyclerView.setVisibility(View.INVISIBLE);
            mErrorMessageDisplay.setVisibility(View.VISIBLE);
        }

        private void loadMovieData(String choice) {
            showMovieDataView();
            //check if choice is Favorites, if so, load the collection from the local db
            if (choice.equals(FAVORITES)) {
                getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, this);
            //if choice is most popular or top rated, call the Retrofit client to fetch collection
            } else {
                if(choice.equals(POPULAR)) {
                    Call<MovieResult> call = apiInterface.getMostPopularMovies(NetworkUtils.KEY);
                    call.enqueue(new Callback<MovieResult>() {
                        @Override
                        public void onResponse(Call<MovieResult>call, Response<MovieResult> response) {
                            List<Movie> movies = response.body().getResults();
                            Log.d(TAG, "Number of movies received: " + movies.size());
                            if (movies.size()>0) {
                                //for each Movie object, parse its release year and store it in the object field
                                setEachReleaseYearInCollection(movies);
                                showMovieDataView();
                                //load the collection in the adapter
                                mAdapter.setData(movies);
                                //restore the previous scroll position
                                restoreLayoutManagerPosition();
                            } else {
                                //if there are no movies in the collection, show error message
                                mErrorMessageDisplay.setText(R.string.error_fetching_collection);
                                showErrorMessage();
                            }
                        }

                        @Override
                        public void onFailure(Call<MovieResult>call, Throwable t) {
                            //check if there is an internet connection, if not, display a message
                            if (!NetworkUtils.isNetworkAvailable(context)) {
                                mErrorMessageDisplay.setText(R.string.no_internet_connection_message);
                                showErrorMessage();
                            }
                            Log.e(TAG, t.toString());
                        }
                    });
                } else {
                    Call<MovieResult> call = apiInterface.getTopRatedMovies(NetworkUtils.KEY);
                    call.enqueue(new Callback<MovieResult>() {
                        @Override
                        public void onResponse(Call<MovieResult>call, Response<MovieResult> response) {
                            List<Movie> movies = response.body().getResults();
                            Log.d(TAG, "Number of movies received: " + movies.size());
                            if (movies.size()>0) {
                                //for each Movie object, parse its release year and store it in the object field
                                setEachReleaseYearInCollection(movies);
                                showMovieDataView();
                                //load the collection in the adapter
                                mAdapter.setData(movies);
                                //restore the previous scroll position
                                restoreLayoutManagerPosition();
                            } else {
                                //if there are no movies in the collection, show error message
                                mErrorMessageDisplay.setText(R.string.error_fetching_collection);
                                showErrorMessage();
                            }
                        }

                        @Override
                        public void onFailure(Call<MovieResult>call, Throwable t) {
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
        }

    private void setEachReleaseYearInCollection (List<Movie> movies) {
        for (Movie movie : movies) {
            movie.setReleaseYear(movie.getReleaseDate());
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
        if (choice!=null) {
            spinner.setSelection(getChoicePosition(choice));
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                Object item = adapterView.getItemAtPosition(position);
                if (item != null) {
                    mAdapter.setData(null);
                    if (position==0) {
                        choice = POPULAR;
                    }
                    if (position == 1) {
                        mAdapter.setData(null);
                        choice = TOP_RATED;
                    }
                    if (position == 2) {
                        choice = FAVORITES;

                    }
                    loadMovieData(choice);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}

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
            Movie[] movies;

            @Override
            protected void onStartLoading() {
                if (favoritesData!=null) {
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
                if (data==null || data.getCount()==0) {
                    mErrorMessageDisplay.setText(R.string.no_favorites);
                    showErrorMessage();
                } else {
                    //reads to Cursor data into a Movie array for the adapter
                    movies = loadCursorDataToObjects(data);
                    mAdapter.setData(Arrays.asList(movies));
                    //restore the scroll position
                    restoreLayoutManagerPosition();
                }

                favoritesData = data;
                super.deliverResult(data);

            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //sets the adapter data and restores the scroll position
        mAdapter.setData(Arrays.asList(loadCursorDataToObjects(data)));
        restoreLayoutManagerPosition();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    // iterates of the Cursor data and loads it into a Movie array
    public Movie[] loadCursorDataToObjects(Cursor data) {
        int noOfMovies = data.getCount();
        Movie[] movies = new Movie[noOfMovies];
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

            movies[i] = movie;
        }
        return movies;
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        //load the collection saved before restart
        loadMovieData(choice);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //save the scroll position in the LayoutManager
        outState.putParcelable(SAVED_LAYOUT_MANAGER, mRecyclerView.getLayoutManager().onSaveInstanceState());
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        if (state!=null) {
            //restore the LayoutManager's state (i.e. scroll position)
            layoutManagerSavedState = ((Bundle) state).getParcelable(SAVED_LAYOUT_MANAGER);
        }
        super.onRestoreInstanceState(state);
    }

    private void restoreLayoutManagerPosition() {
        if (layoutManagerSavedState != null) {
            //if the LayoutManager's state is saved, restore it
            mRecyclerView.getLayoutManager().onRestoreInstanceState(layoutManagerSavedState);
        }
    }
}
