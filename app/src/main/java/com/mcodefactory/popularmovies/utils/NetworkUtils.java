package com.mcodefactory.popularmovies.utils;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class NetworkUtils {

    final static String BASE_URL = "https://api.themoviedb.org/3/";

    //base urls with two different poster sizes needed for the Picasso calls
    final static String BASE_URL_WITH_SMALL_POSTER_SIZE = "http://image.tmdb.org/t/p/w185/";
    final static String BASE_URL_WITH_LARGE_POSTER_SIZE = "http://image.tmdb.org/t/p/w342/";

    //insert your API key provided by the https://www.themoviedb.org
    public final static String KEY = "*****************";

    public final static String YOUTUBE_BASE_URL = "http://www.youtube.com/watch?v=";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit==null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    //builds a Uri for the small poster to be loaded by Picasso
    public static Uri buildPosterUri (String posterPath) {
        Uri pathUri = Uri.parse(BASE_URL_WITH_SMALL_POSTER_SIZE.concat(posterPath));
        return pathUri;

    }

    //builds a Uri for the large poster to be loaded by Picasso
    public static Uri buildThumbnailUri (String posterPath) {
        Uri pathUri = Uri.parse(BASE_URL_WITH_LARGE_POSTER_SIZE.concat(posterPath));
        return pathUri;

    }

    //checks if the device is connected to the internet
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
