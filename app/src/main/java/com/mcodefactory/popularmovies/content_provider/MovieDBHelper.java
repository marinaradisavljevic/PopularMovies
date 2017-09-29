package com.mcodefactory.popularmovies.content_provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper class for the Favorites collection db
 */

public class MovieDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "movie.db";
    private static final int DB_VERSION = 1;

    public MovieDBHelper (Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String CREATE_MOVIEDB = "CREATE TABLE " + FavoriteMoviesContract.MovieEntry.TABLE_NAME +
                " (" + FavoriteMoviesContract.MovieEntry.MOVIEDB_ID + " INTEGER UNIQUE, " +
                FavoriteMoviesContract.MovieEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                FavoriteMoviesContract.MovieEntry.AVERAGE_VOTE + " REAL NOT NULL, " +
                FavoriteMoviesContract.MovieEntry.ORIGINAL_TITLE + " TEXT NOT NULL, " +
                FavoriteMoviesContract.MovieEntry.POSTER_IMAGE_NAME + " TEXT NOT NULL, " +
                FavoriteMoviesContract.MovieEntry.RELEASE_YEAR + " INTEGER NOT NULL, " +
                FavoriteMoviesContract.MovieEntry.RUNTIME + " INTEGER NOT NULL, " +
                FavoriteMoviesContract.MovieEntry.SYNOPSIS + " TEXT NOT NULL);";

        sqLiteDatabase.execSQL(CREATE_MOVIEDB);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + FavoriteMoviesContract.MovieEntry.TABLE_NAME + ";");
        onCreate(sqLiteDatabase);
    }
}
