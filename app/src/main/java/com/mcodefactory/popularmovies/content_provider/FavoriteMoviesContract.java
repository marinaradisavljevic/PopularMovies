package com.mcodefactory.popularmovies.content_provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract class for the Favorites collection locally stored in a SQLite db and used through a contentProvider
 */

public class FavoriteMoviesContract {

    public static final String SCHEME = "content://";
    public static final String AUTHORITY = "com.mcodefactory.popularmovies";
    public static final Uri BASE_CONTENT = Uri.parse(SCHEME.concat(AUTHORITY));
    public static final String FAVORITES_TABLE_PATH = "favorite_movies";

    public static final class MovieEntry implements BaseColumns {
        public static final String TABLE_NAME = "favorite_movies";
        public static final String MOVIEDB_ID = "moviedb_id";
        public static final String ORIGINAL_TITLE = "title";
        public static final String RUNTIME = "runtime";
        public static final String AVERAGE_VOTE = "vote";
        public static final String SYNOPSIS = "synopsis";
        public static final String POSTER_IMAGE_NAME = "poster_path";
        public static final String RELEASE_YEAR = "release_year";

        public static final Uri CONTENT_URI = BASE_CONTENT.buildUpon().appendPath(FAVORITES_TABLE_PATH).build();

    }
}
