package com.mcodefactory.popularmovies;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Paint;
import android.net.Uri;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.mcodefactory.popularmovies.api_result_models.DetailResult;
import com.mcodefactory.popularmovies.content_provider.FavoriteMoviesContract;
import com.mcodefactory.popularmovies.data.Movie;
import com.mcodefactory.popularmovies.data.Review;
import com.mcodefactory.popularmovies.api_result_models.ReviewResult;
import com.mcodefactory.popularmovies.data.Trailer;
import com.mcodefactory.popularmovies.api_result_models.VideoResult;
import com.mcodefactory.popularmovies.databinding.ActivityMovieDetailsBinding;
import com.mcodefactory.popularmovies.utils.ApiInterface;
import com.mcodefactory.popularmovies.utils.NetworkUtils;
import com.mcodefactory.popularmovies.utils.PicassoTarget;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetails extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = MovieDetails.class.getSimpleName();
    private static final int TASK_LOADER_ID = 1;
    ActivityMovieDetailsBinding movieDetailsBinding;
    Movie movie;
    Uri posterUri;
    ArrayList<Integer> favoriteIds;
    public static String imageName;
    public static boolean isFavorite;
    private String choice;
    Context context;
    List<Review> reviews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        movieDetailsBinding = DataBindingUtil.setContentView(this, R.layout.activity_movie_details);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        context = this;

        Intent intent = getIntent();
        Bundle data = intent.getExtras();
        if (data!=null && intent.hasExtra("movie") && intent.hasExtra("choice")) {
            choice = data.getString("choice");
            movie = data.getParcelable("movie");
        }

        favoriteIds = new ArrayList<>();
        getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, this);


        posterUri = NetworkUtils.buildThumbnailUri(movie.getPosterPath());
        Picasso.with(this).load(posterUri).placeholder(R.drawable.ic_panorama_white_48px).into(movieDetailsBinding.ivThumbnail);

        String text = "<html><body>"
                + "<p align=\"justify\">"
                + movie.getSynopsis()
                + "</p> "
                + "</body></html>";


        movieDetailsBinding.tvTitle.setText(movie.getOriginalTitle());
        movieDetailsBinding.tvRating.setText(String.valueOf("Rating: " + movie.getVoteAverage()).concat("/10.0"));
        movieDetailsBinding.wvSynopsis.loadData(text, "text/html", "utf-8");
        movieDetailsBinding.tvReleaseDate.setText(String.valueOf(movie.getReleaseYear()));

        //fetch movie details to obtain the runtime value and check if the original title is different from the title
        ApiInterface apiInterface = NetworkUtils.getClient().create(ApiInterface.class);
        Call<DetailResult> detailsCall = apiInterface.getMovieDetails(movie.getId(), NetworkUtils.KEY);
        detailsCall.enqueue(new Callback<DetailResult>() {
            @Override
            public void onResponse(Call<DetailResult> call, Response<DetailResult> response) {
                if (response != null) {
                    movie.setRuntime(response.body().getRuntime());
                    movieDetailsBinding.tvRuntime.setText(movie.getRuntime() + "min");
                    //if the language is not English, show the English title in the ActionBar
                    if (!response.body().getOriginalLanguage().equals("en") && movie.getTitle()!=null) {
                        getSupportActionBar().setTitle(movie.getTitle());
                    }
                }
            }

            @Override
            public void onFailure(Call<DetailResult> call, Throwable t) {
                //check if the call failed to the lack of internet connection and display a message
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    Toast.makeText(context, R.string.no_internet_connection_message, Toast.LENGTH_SHORT);
                } else {
                    Toast.makeText(context, R.string.failed_loading_movie_details, Toast.LENGTH_SHORT);
                }
            }
        });

        //fetch the available videos for the movie
        Call<VideoResult> videoResultCall = apiInterface.getMovieVideos(movie.getId(), NetworkUtils.KEY);
        videoResultCall.enqueue(new Callback<VideoResult>() {
            @Override
            public void onResponse(Call<VideoResult> call, Response<VideoResult> response) {
                if (response != null) {
                    //gets all the available videos, including clips, trailers, etc.
                    List<Trailer> videos = response.body().getVideos();
                    //creates a new list containing only trailers
                    List<Trailer> allTrailers = keepOnlyTrailers(videos);

                    if (allTrailers.size()>0) {
                        //if there are trailers available, display them
                        displayTrailerInfo(allTrailers);
                    } else {
                        //there are no trailers for the movie, display a message
                        movieDetailsBinding.trailersLayout.playImage1.setVisibility(View.INVISIBLE);
                        movieDetailsBinding.trailersLayout.video1.setText(getResources().getString(R.string.no_videos_message));
                    }
                }
            }

            @Override
            public void onFailure(Call<VideoResult> call, Throwable t) {
                //check if the call failed due to the lack of internet connection and display a message
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    Toast.makeText(context, R.string.no_internet_connection_message, Toast.LENGTH_SHORT);
                } else {
                    Toast.makeText(context, R.string.failed_loading_videos, Toast.LENGTH_SHORT);
                }
            }
        });


        //fetch the reviews for the movie
        Call<ReviewResult> reviewsCall = apiInterface.getMovieReviews(movie.getId(), NetworkUtils.KEY);
        reviewsCall.enqueue(new Callback<ReviewResult>() {
            @Override
            public void onResponse(Call<ReviewResult> call, Response<ReviewResult> response) {
                if (response != null) {
                    int reviewNumber = response.body().getTotalReviews();
                    movieDetailsBinding.numberOfReviews.setText(String.valueOf(reviewNumber));
                    //if there are reviews available, make the number clickable
                    if (reviewNumber>0) {
                        movieDetailsBinding.numberOfReviews.setPaintFlags(movieDetailsBinding.numberOfReviews.getPaintFlags()
                                | Paint.UNDERLINE_TEXT_FLAG);
                        movieDetailsBinding.numberOfReviews.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                        //store the Review objects into a variable
                        reviews = response.body().getReviews();
                    }
                }
            }

            @Override
            public void onFailure(Call<ReviewResult> call, Throwable t) {
                //check if the failure was caused by the lack of internet connection and display a message
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    Toast.makeText(context, R.string.no_internet_connection_message, Toast.LENGTH_SHORT);
                } else {
                    Toast.makeText(context, R.string.failed_loading_reviews, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //goes through all available videos and stores only trailers in a new list
    private List<Trailer> keepOnlyTrailers(List<Trailer> videos) {
        List<Trailer> allTrailers = new ArrayList<>();
        for (Trailer video : videos) {
            if (video.getType().equals("Trailer")) {
                allTrailers.add(video);
            }
        }
        return allTrailers;
    }

    //the layout foresees a max of 3 trailers to display. This method loads the data based on the number of available trailers
    private void displayTrailerInfo(List<Trailer> trailers) {
        //checks if the Trailer object has a name. If not, assigns a default name to it
        if (trailers.get(0).getName() == null) {
                setDefaultTrailerName(trailers.get(0));
        }
        //sets the value for the trailer name
        movieDetailsBinding.trailersLayout.video1.setText(trailers.get(0).getName());
        //sets the trailer's tag to be used when launching Youtube app to view the trailer
        movieDetailsBinding.trailersLayout.playImage1.setTag(trailers.get(0).getKey());

        //if there is more than 1 trailer, show the second one
        if (trailers.size()>1) {
            //checks if the Trailer object has a name. If not, assigns a default name to it
            if (trailers.get(1).getName() == null) {
                setDefaultTrailerName(trailers.get(1));
            }
            //sets the value for the trailer name
            movieDetailsBinding.trailersLayout.video2.setText(trailers.get(1).getName());
            //sets the trailer's tag to be used when launching Youtube app to view the trailer
            movieDetailsBinding.trailersLayout.playImage2.setTag(trailers.get(1).getKey());
            //makes the "play" image and the name text view visible
            movieDetailsBinding.trailersLayout.video2.setVisibility(View.VISIBLE);
            movieDetailsBinding.trailersLayout.playImage2.setVisibility(View.VISIBLE);
        }
        //if there are more than 2 trailers, show the third one
        if (trailers.size()>2) {
            //checks if the Trailer object has a name. If not, assigns a default name to it
            if (trailers.get(2).getName() == null) {
                setDefaultTrailerName(trailers.get(2));
            }
            //sets the value for the trailer name
            movieDetailsBinding.trailersLayout.video3.setText(trailers.get(2).getName());
            //sets the trailer's tag to be used when launching Youtube app to view the trailer
            movieDetailsBinding.trailersLayout.playImage3.setTag(trailers.get(2).getKey());
            //makes the "play" image and the name text view visible
            movieDetailsBinding.trailersLayout.video3.setVisibility(View.VISIBLE);
            movieDetailsBinding.trailersLayout.playImage3.setVisibility(View.VISIBLE);
        }

    }

    //assigns a default name to the Trailer object without a name value
    private void setDefaultTrailerName (Trailer trailer) {
        if (trailer.getName() == null) {
            trailer.setName("Trailer");
        }
    }

    //checks if the number of available reviews is greater than 0 and launches a new activity to display them
    public void showReviews(View view) {
        TextView textView = (TextView)view;
        String value = textView.getText().toString();
        int numberOfReviews = Integer.valueOf(value);

        if (numberOfReviews>0) {
            Intent intent = new Intent(view.getContext(), ReviewActivity.class);
            Bundle bundle = new Bundle();
            //send the List of Review objects to the ReviewActivity
            bundle.putParcelableArrayList("reviews", (ArrayList)reviews);
            //send the movie title to the ReviewActivity
            bundle.putString("title", movie.getOriginalTitle());
            intent.putExtra("bundle", bundle);
            startActivity(intent);
        } else {
            //display a message to the user that there are no reviews to display
            Toast.makeText(view.getContext(), R.string.no_reviews, Toast.LENGTH_LONG).show();
        }
    }

    //launches the Youtube app to play the trailer
    public void playVideo(View view) {
        ImageView video = (ImageView) view;
        String tag = video.getTag().toString();
        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(NetworkUtils.YOUTUBE_BASE_URL.concat(tag)));
        //check if the Youtube app is present on the device
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            //display a message to the user that the video cannot be played on the device
            Toast.makeText(this, R.string.no_app_for_videos, Toast.LENGTH_LONG).show();
        }
    }

    //fetches the values in the Favorite collection
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new AsyncTaskLoader<Cursor>(this) {
            Cursor favoritesIdsData = null;
            @Override
            protected void onStartLoading() {
                if (favoritesIdsData!=null) {
                    deliverResult(favoritesIdsData);
                } else {
                    forceLoad();
                }
            }

            @Override
            public Cursor loadInBackground() {
                try {
                    favoritesIdsData = getContentResolver().query(FavoriteMoviesContract.MovieEntry.CONTENT_URI, null, null, null, null);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to asynchronously load movie ids from Favorites.");
                    e.printStackTrace();
                    return null;
                }
                return favoritesIdsData;
            }
            public void deliverResult(Cursor data) {
                //get the ids of the favorite movies
                favoriteIds = loadCursorDataArray(data);
                //check if the given movie is among them
                isFavorite = checkIfFavorite();
                movie.setFavorite(isFavorite);

                if (movie.isFavorite()) {
                    movieDetailsBinding.ivFavorite.setImageResource(R.drawable.ic_grade_yellow_36px);
                    imageName = movie.getPosterPath();
                    File poster = loadPosterFromStorage(getBaseContext(), imageName);
                    //if the Favorites collection launched this activity, load the poster from local storage
                    if (choice.equals("favorites")) {
                        Picasso.with(getBaseContext()).load(poster).placeholder(R.drawable.ic_panorama_white_48px).into(movieDetailsBinding.ivThumbnail);
                    }
                } else {
                    movieDetailsBinding.ivFavorite.setImageResource(R.drawable.ic_grade_gray_36px);
                }
                favoritesIdsData = data;
                super.deliverResult(data);

            }
        };
    }

    public File loadPosterFromStorage(Context context, String imageName) {
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File poster = new File(directory, imageName);
        return poster;
    }

    private ArrayList<Integer> loadCursorDataArray(Cursor data) {
        ArrayList<Integer> ids = new ArrayList<>();
        if (data.getCount()>0) {
            //iterate of the Cursor and create a list of ids of favorite movies
            for (int i = 0; i<data.getCount(); i++) {
                data.moveToPosition(i);
                int movieDbIdIndex = data.getColumnIndex(FavoriteMoviesContract.MovieEntry.MOVIEDB_ID);
                ids.add(data.getInt(movieDbIdIndex));
            }
        }
        return ids;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public void setFavorite(View view) {
        if (movie.isFavorite()) {
            //if the movie is a favorite, delete it from the collection
            deleteMovieFromFavorites(movie.getId());
        } else {
            //if the movie is not already among the favorites, add it to the collection
            addMovieToFavorites(movie);
        }
    }

    //check if the id of the given movie is among the ids of favorite movies
    public boolean checkIfFavorite() {
        if (favoriteIds.contains(movie.getId())) {
            return true;
        } else {
            return false;
        }

    }

    //delete the Movie from the Favorites
    public void deleteMovieFromFavorites(int id) {
        movie.setFavorite(false);
        //change the star from yellow to gray
        movieDetailsBinding.ivFavorite.setImageResource(R.drawable.ic_grade_gray_36px);
        //delete the poster from local storage
        deleteMoviePoster();
        String stringId = Integer.toString(id);
        Uri uri = FavoriteMoviesContract.MovieEntry.CONTENT_URI.buildUpon().appendPath(stringId).build();
        String whereClause = "moviedb_id=?";
        String[] whereArgs = new String[] {stringId};

        int rowsDeleted = getContentResolver().delete(uri, whereClause, whereArgs);
        if (rowsDeleted>0) {
            String successMessage = "You have successfully deleted " + movie.getOriginalTitle() + " from the Favorites collection";
            Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
        } else {
            String errorMessage = "An error occurred while deleting " + movie.getOriginalTitle() + " from the Favorites collection";
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    //add a Movie to the Favorites
    public void addMovieToFavorites(Movie movie) {
        movie.setFavorite(true);
        //change the star from gray to yellow
        movieDetailsBinding.ivFavorite.setImageResource(R.drawable.ic_grade_yellow_36px);
        //save the poster in local storage
        String imageName = saveMoviePoster();

        //load the necessary movie fields into the ContentValues object
        ContentValues values = new ContentValues();
        values.put(FavoriteMoviesContract.MovieEntry.MOVIEDB_ID, movie.getId());
        values.put(FavoriteMoviesContract.MovieEntry.AVERAGE_VOTE, movie.getVoteAverage());
        values.put(FavoriteMoviesContract.MovieEntry.ORIGINAL_TITLE, movie.getOriginalTitle());
        values.put(FavoriteMoviesContract.MovieEntry.RELEASE_YEAR, movie.getReleaseYear());
        values.put(FavoriteMoviesContract.MovieEntry.RUNTIME, movie.getRuntime());
        values.put(FavoriteMoviesContract.MovieEntry.SYNOPSIS, movie.getSynopsis());
        values.put(FavoriteMoviesContract.MovieEntry.POSTER_IMAGE_NAME, imageName);

        //insert the movie into the Favorites db
        Uri uri = getContentResolver().insert(FavoriteMoviesContract.MovieEntry.CONTENT_URI, values);

        if (uri!=null) {
            String successMessage = "You have successfully added " + movie.getOriginalTitle() + " to the Favorites collection";
            Toast.makeText(getBaseContext(), successMessage, Toast.LENGTH_LONG).show();
        }
    }

    public Target getPicassoTarget(Context context, final String imageDir, final String imageName) {
        Log.d("picassoTarget", " picassoTarget");
        ContextWrapper cw = new ContextWrapper(context);
        final File directory = cw.getDir(imageDir, Context.MODE_PRIVATE);
        return new PicassoTarget(directory, imageName);}

    public String saveMoviePoster() {
        String imageName = String.valueOf(movie.getId()).concat(".jpeg");
        String directoryName = "imageDir";
        Picasso.with(this).load(posterUri).into(getPicassoTarget(getApplicationContext(), directoryName, imageName));
        return imageName;
    }

    public void deleteMoviePoster() {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        String imageName = String.valueOf(movie.getId()).concat(".jpeg");
        File poster = new File(directory, imageName);
        poster.delete();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        movie.setFavorite(isFavorite);
        if (imageName!=null) {
            loadPosterFromStorage(this, imageName);
        }

        getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, this);
    }

}
