package com.mcodefactory.popularmovies.fragments;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mcodefactory.popularmovies.MainActivity;
import com.mcodefactory.popularmovies.R;
import com.mcodefactory.popularmovies.api_result_models.DetailResult;
import com.mcodefactory.popularmovies.api_result_models.ReviewResult;
import com.mcodefactory.popularmovies.api_result_models.VideoResult;
import com.mcodefactory.popularmovies.content_provider.FavoriteMoviesContract;
import com.mcodefactory.popularmovies.data.Movie;
import com.mcodefactory.popularmovies.data.Review;
import com.mcodefactory.popularmovies.data.Trailer;
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


public class DetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int TASK_LOADER_ID = 1;
    private static Movie movie;
    private Uri posterUri;
    private ArrayList<Integer> favoriteIds;
    private String imageName;
    private boolean isFavorite;
    private static boolean isDualPane;
    private List<Review> reviews;
    private OnSelectedListener listener;

    private TextView tvTitle;
    private String title;
    private TextView releaseDate;
    private int releaseYear;
    private ImageView poster;
    private String posterPath;
    private TextView rating;
    private double voteAverage;
    private ImageView star;
    private WebView description;
    private String synopsis;
    private TextView runtime;
    private int movieRuntime;
    private TextView numberOfReviews;
    private static int reviewNo = -1;
    private ViewGroup trailers;
    private List<Trailer> allTrailers;
    private TextView video1;
    private ImageView play1;
    private TextView video2;
    private ImageView play2;
    private TextView video3;
    private ImageView play3;
    private Call<DetailResult> detailsCall;
    private Call<ReviewResult> reviewsCall;
    private Call<VideoResult> videoResultCall;


    public interface OnSelectedListener {
        void playVideo(String tag);

        void displayReviews(List<Review> reviews);

        void removedFromFavorites();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isDualPane = MainActivity.appContext.getResources().getBoolean(R.bool.dual_pane_mode);

        //fetch movie details to obtain the runtime value
        ApiInterface apiInterface = NetworkUtils.getClient().create(ApiInterface.class);
        detailsCall = apiInterface.getMovieDetails(movie.getId(), NetworkUtils.KEY);
        detailsCall.enqueue(new Callback<DetailResult>() {
            @Override
            public void onResponse(Call<DetailResult> call, Response<DetailResult> response) {
                if (response != null) {
                    movie.setRuntime(response.body().getRuntime());
                    movieRuntime = movie.getRuntime();
                    runtime.setText(movieRuntime + "min");
                }
            }

            @Override
            public void onFailure(Call<DetailResult> call, Throwable t) {
                //check if the call failed due to the lack of internet connection and display a message
                if (!NetworkUtils.isNetworkAvailable(getActivity())) {
                    Toast.makeText(getActivity(), R.string.no_internet_connection_message,
                            Toast.LENGTH_SHORT).show();
                    movie.setRuntime(0);
                } else {
                    Toast.makeText(getActivity(), R.string.failed_loading_movie_details,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        //fetch the reviews for the movie
        reviewsCall = apiInterface.getMovieReviews(movie.getId(), NetworkUtils.KEY);
        reviewsCall.enqueue(new Callback<ReviewResult>() {
            @Override
            public void onResponse(Call<ReviewResult> call, Response<ReviewResult> response) {
                if (response != null) {
                    movie.setReviewNo(response.body().getTotalReviews());
                    reviewNo = movie.getReviewNo();

                    //store the Review objects into a variable
                    reviews = response.body().getReviews();
                    numberOfReviews.setText(String.valueOf(reviewNo));
                    if (reviewNo > 0) {
                        numberOfReviews.setPaintFlags(numberOfReviews.getPaintFlags()
                                | Paint.UNDERLINE_TEXT_FLAG);
                        numberOfReviews.setTextColor(MainActivity.
                                appContext.getResources().getColor(R.color.colorPrimaryDark));
                    }
                    if (isDualPane) {
                        listener.displayReviews(reviews);
                    }
                }
            }

            @Override
            public void onFailure(Call<ReviewResult> call, Throwable t) {
                //check if the failure was caused by the lack of internet connection and display a message
                if (!NetworkUtils.isNetworkAvailable(getActivity())) {
                    Toast.makeText(getActivity(), R.string.no_internet_connection_message,
                            Toast.LENGTH_SHORT);
                } else {
                    Toast.makeText(getActivity(), R.string.failed_loading_reviews,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        //fetch the available videos for the movie
        videoResultCall = apiInterface.getMovieVideos(movie.getId(), NetworkUtils.KEY);
        videoResultCall.enqueue(new Callback<VideoResult>() {
            @Override
            public void onResponse(Call<VideoResult> call, Response<VideoResult> response) {
                if (response != null) {
                    //gets all the available videos, including clips, trailers, etc.
                    List<Trailer> videos = response.body().getVideos();
                    //creates a new list containing only trailers
                    allTrailers = keepOnlyTrailers(videos);
                    if (allTrailers != null && allTrailers.size() > 0) {
                        //if there are trailers available, display them
                        displayTrailerInfo(allTrailers);
                    } else {
                        //there are no trailers for the movie, display a message
                        play1.setVisibility(View.INVISIBLE);
                        video1.setText(getResources().getString(R.string.no_videos_message));
                    }
                }
            }

            @Override
            public void onFailure(Call<VideoResult> call, Throwable t) {
                //check if the call failed due to the lack of internet connection and display a message
                if (!NetworkUtils.isNetworkAvailable(getActivity())) {
                    Toast.makeText(getActivity(),
                            R.string.no_internet_connection_message, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(),
                            R.string.failed_loading_videos, Toast.LENGTH_SHORT).show();
                }
            }
        });

        favoriteIds = new ArrayList<>();
        getActivity().getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, this);
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.details_fragment, container, false);
        readArguments(getArguments());
        runtime = (TextView) view.findViewById(R.id.tv_runtime);
        numberOfReviews = (TextView) view.findViewById(R.id.number_of_reviews);
        trailers = (ViewGroup) view.findViewById(R.id.trailers_layout);
        video1 = (TextView) trailers.findViewById(R.id.video1);
        play1 = (ImageView) trailers.findViewById(R.id.play_image1);
        video2 = (TextView) trailers.findViewById(R.id.video2);
        play2 = (ImageView) trailers.findViewById(R.id.play_image2);
        video3 = (TextView) trailers.findViewById(R.id.video3);
        play3 = (ImageView) trailers.findViewById(R.id.play_image3);
        star = (ImageView) view.findViewById(R.id.iv_favorite);
        poster = (ImageView) view.findViewById(R.id.iv_thumbnail);
        posterUri = NetworkUtils.buildThumbnailUri(posterPath);
        Picasso.with(getActivity()).load(posterUri).into(poster, new com.squareup.picasso.Callback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError() {
                if (getActivity() != null) {
                    File image = loadPosterFromStorage(getActivity(), imageName);
                    Picasso.with(getActivity()).load(image).
                            placeholder(R.drawable.ic_panorama_white_48px).into(poster);
                }

            }
        });
        if (savedInstanceState != null) {
            restoreSavedState(savedInstanceState);
        }
        tvTitle = (TextView) view.findViewById(R.id.tv_title);
        tvTitle.setText(title);
        releaseDate = (TextView) view.findViewById(R.id.tv_release_date);
        releaseDate.setText(String.valueOf(releaseYear));

        description = (WebView) view.findViewById(R.id.wv_synopsis);
        String text = "<html><body>"
                + "<p align=\"justify\">"
                + synopsis
                + "</p> "
                + "</body></html>";
        description.loadData(text, "text/html", "utf-8");

        rating = (TextView) view.findViewById(R.id.tv_rating);
        rating.setText(String.valueOf("Rating: " + voteAverage + "/10.0"));

        star.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFavorite(v);
            }
        });

        if (!isDualPane) {
            numberOfReviews.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showReviews(v);
                }
            });

        } else {
            numberOfReviews.setVisibility(View.GONE);
            TextView reviewsLabel = (TextView) view.findViewById(R.id.reviews_label);
            reviewsLabel.setVisibility(View.GONE);
        }
        return view;
    }

    public static DetailsFragment newInstance(Movie receivedMovie, boolean isDualPaneMode) {
        movie = receivedMovie;
        isDualPane = isDualPaneMode;
        Bundle args = new Bundle();
        args.putString("title", movie.getOriginalTitle());
        args.putInt("releaseYear", movie.getReleaseYear());
        args.putDouble("voteAverage", movie.getVoteAverage());
        args.putString("posterPath", movie.getPosterPath());
        args.putString("synopsis", movie.getSynopsis());
        args.putInt("runtime", movie.getRuntime());
        args.putInt("reviews", movie.getReviewNo());

        DetailsFragment fragment = new DetailsFragment();
        fragment.setArguments(args);
        return fragment;
    }


    private void readArguments(Bundle args) {
        if (args != null) {
            title = args.getString("title");
            releaseYear = args.getInt("releaseYear");
            voteAverage = args.getDouble("voteAverage");
            posterPath = args.getString("posterPath");
            synopsis = args.getString("synopsis");
            movieRuntime = args.getInt("runtime");
            reviewNo = args.getInt("reviews");
        }
    }


    //the layout foresees a max of 3 trailers to display. This method loads the data based on the number of available trailers
    private void displayTrailerInfo(List<Trailer> trailers) {
        //checks if the Trailer object has a name. If not, assigns a default name to it
        if (trailers.get(0).getName() == null) {
            setDefaultTrailerName(trailers.get(0));
        }
        //sets the value for the trailer name
        video1.setText(trailers.get(0).getName());
        //sets the trailer's tag to be used when launching Youtube app to view the trailer
        play1.setTag(trailers.get(0).getKey());
        play1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playVideo(v);
            }
        });

        //if there is more than 1 trailer, show the second one
        if (trailers.size() > 1) {
            //checks if the Trailer object has a name. If not, assigns a default name to it
            if (trailers.get(1).getName() == null) {
                setDefaultTrailerName(trailers.get(1));
            }
            //sets the value for the trailer name
            video2.setText(trailers.get(1).getName());
            //sets the trailer's tag to be used when launching Youtube app to view the trailer
            play2.setTag(trailers.get(1).getKey());
            play2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playVideo(v);
                }
            });
            //makes the "play" image and the name text view visible
            video2.setVisibility(View.VISIBLE);
            play2.setVisibility(View.VISIBLE);
        }
        //if there are more than 2 trailers, show the third one
        if (trailers.size() > 2) {
            //checks if the Trailer object has a name. If not, assigns a default name to it
            if (trailers.get(2).getName() == null) {
                setDefaultTrailerName(trailers.get(2));
            }
            //sets the value for the trailer name
            video3.setText(trailers.get(2).getName());
            //sets the trailer's tag to be used when launching Youtube app to view the trailer
            play3.setTag(trailers.get(2).getKey());
            play3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playVideo(v);
                }
            });
            //makes the "play" image and the name text view visible
            video3.setVisibility(View.VISIBLE);
            play3.setVisibility(View.VISIBLE);
        }

    }

    //assigns a default name to the Trailer object without a name value
    private void setDefaultTrailerName(Trailer trailer) {
        if (trailer.getName() == null) {
            trailer.setName("Trailer");
        }
    }

    //checks if the number of available reviews is greater than 0 calls the host activity to show the ReviewsFragment
    public void showReviews(View view) {
        if (reviewNo > 0) {
            //send the List of Review objects to the host activity
            listener.displayReviews(reviews);
        }
    }

    //sends the the youtube tag to the host activity to launch the Youtube app and play the trailer
    public void playVideo(View view) {
        ImageView video = (ImageView) view;
        String tag = video.getTag().toString();
        listener.playVideo(tag);
    }

    //fetches the values from the Favorite collection
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new AsyncTaskLoader<Cursor>(getActivity()) {
            Cursor favoritesIdsData = null;

            @Override
            protected void onStartLoading() {
                if (favoritesIdsData != null) {
                    deliverResult(favoritesIdsData);
                } else {
                    forceLoad();
                }
            }

            @Override
            public Cursor loadInBackground() {
                try {
                    favoritesIdsData = getActivity().getContentResolver().
                            query(FavoriteMoviesContract.MovieEntry.CONTENT_URI, null, null, null, null);
                } catch (Exception e) {
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
                imageName = movie.getPosterPath();

                if (movie.isFavorite()) {
                    star.setImageResource(R.drawable.ic_grade_yellow_36px);
                    favoritesIdsData = data;
                    super.deliverResult(data);

                } else {
                    star.setImageResource(R.drawable.ic_grade_gray_36px);

                }
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (OnSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement DetailsFragment.OnSelectMovieListener");
        }
    }

    private File loadPosterFromStorage(FragmentActivity activity, String imageName) {
        ContextWrapper cw = new ContextWrapper(activity);
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        return new File(directory, imageName);
    }

    //check if the id of the given movie is among the ids of favorite movies
    private boolean checkIfFavorite() {
        if (favoriteIds.contains(movie.getId())) {
            movie.setFavorite(true);
            return true;
        }
        movie.setFavorite(false);
        return false;
    }

    private ArrayList<Integer> loadCursorDataArray(Cursor data) {
        ArrayList<Integer> ids = new ArrayList<>();
        if (data.getCount() > 0) {
            //iterate of the Cursor and create a list of ids of favorite movies
            for (int i = 0; i < data.getCount(); i++) {
                data.moveToPosition(i);
                int movieDbIdIndex = data.getColumnIndex(FavoriteMoviesContract.MovieEntry.MOVIEDB_ID);
                ids.add(data.getInt(movieDbIdIndex));
            }
        }
        return ids;
    }

    public void setFavorite(View view) {
        if (movie.isFavorite()) {
            //if the movie is a favorite, delete it from the collection
            deleteMovieFromFavorites(movie.getId());
            listener.removedFromFavorites();
        } else {
            //if the movie is not already among the favorites, add it to the collection
            addMovieToFavorites(movie);
        }
    }

    //delete the Movie from the Favorites
    public void deleteMovieFromFavorites(int id) {
        movie.setFavorite(false);
        isFavorite = false;
        //change the star from yellow to gray
        star.setImageResource(R.drawable.ic_grade_gray_36px);
        //delete the poster from local storage
        deleteMoviePoster();
        String stringId = Integer.toString(id);
        Uri uri = FavoriteMoviesContract.MovieEntry.CONTENT_URI.buildUpon()
                .appendPath(stringId).build();
        String whereClause = "moviedb_id=?";
        String[] whereArgs = new String[]{stringId};

        int rowsDeleted = getActivity().getContentResolver().delete(uri, whereClause, whereArgs);
        if (rowsDeleted > 0) {
            String successMessage = "You have successfully deleted " + movie.getOriginalTitle() +
                    " from the Favorites collection";
            Toast.makeText(getActivity(), successMessage, Toast.LENGTH_LONG).show();
        } else {
            String errorMessage = "An error occurred while deleting " + movie.getOriginalTitle() +
                    " from the Favorites collection";
            Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    //add a Movie to the Favorites
    public void addMovieToFavorites(Movie movie) {
        movie.setFavorite(true);
        isFavorite = true;
        //change the star from gray to yellow
        star.setImageResource(R.drawable.ic_grade_yellow_36px);
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
        Uri uri = getActivity().getContentResolver().
                insert(FavoriteMoviesContract.MovieEntry.CONTENT_URI, values);

        if (uri != null) {
            String successMessage = "You have successfully added " + movie.getOriginalTitle() +
                    " to the Favorites collection";
            Toast.makeText(getActivity(), successMessage, Toast.LENGTH_LONG).show();
        }
    }

    public Target getPicassoTarget(Context context, final String imageDir, final String imageName) {
        Log.d("picassoTarget", " picassoTarget");
        ContextWrapper cw = new ContextWrapper(context);
        final File directory = cw.getDir(imageDir, Context.MODE_PRIVATE);
        return new PicassoTarget(directory, imageName);
    }

    public String saveMoviePoster() {
        String imageName = String.valueOf(movie.getId()).concat(".jpeg");
        String directoryName = "imageDir";
        Picasso.with(getActivity()).load(posterUri).into(getPicassoTarget(getActivity().
                getApplicationContext(), directoryName, imageName));
        return imageName;
    }

    public void deleteMoviePoster() {
        ContextWrapper cw = new ContextWrapper(getActivity().getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        String imageName = String.valueOf(movie.getId()).concat(".jpeg");
        File poster = new File(directory, imageName);
        poster.delete();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("runtimeString", movieRuntime + "min");
        outState.putString("reviewNo", String.valueOf(reviewNo));
        outState.putBoolean("isFavorite", movie.isFavorite());
        outState.putParcelable("posterUri", posterUri);
        outState.putString("image_name", imageName);
        outState.putParcelableArrayList("allTrailers", (ArrayList) allTrailers);
    }

    public void restoreSavedState(Bundle savedState) {
        if (savedState == null) {
            return;
        }
        runtime.setText(savedState.getString("runtimeString"));
        numberOfReviews.setText(savedState.getString("reviewNo"));
        isFavorite = savedState.getBoolean("isFavorite");
        posterUri = savedState.getParcelable("posterUri");
        imageName = savedState.getString("image_name");
        if (isFavorite) {
            star.setImageResource(R.drawable.ic_grade_yellow_36px);
        } else {
            star.setImageResource(R.drawable.ic_grade_gray_36px);
            Picasso.with(getActivity()).load(posterUri).
                    placeholder(R.drawable.ic_panorama_white_48px).into(poster);
        }
        allTrailers = (List) savedState.getParcelableArrayList("allTrailers");
        displayTrailerInfo(allTrailers);
    }
}
