package com.mcodefactory.popularmovies.content_provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Customized ContentProvider for the Favorites collection
 */

public class MovieContentProvider extends ContentProvider {
    private MovieDBHelper dbHelper;
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    public static final int FAVORITE_MOVIES = 100;
    public static final int SINGLE_FAVORITE_MOVIE = 101;

    public static UriMatcher buildUriMatcher() {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(FavoriteMoviesContract.AUTHORITY, FavoriteMoviesContract.FAVORITES_TABLE_PATH, FAVORITE_MOVIES);
        uriMatcher.addURI(FavoriteMoviesContract.AUTHORITY, FavoriteMoviesContract.FAVORITES_TABLE_PATH + "/#", SINGLE_FAVORITE_MOVIE);
        return uriMatcher;
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        dbHelper = new MovieDBHelper(context);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor favoritesCursor;
        int match = sUriMatcher.match(uri);

        switch (match) {
            case FAVORITE_MOVIES:
                favoritesCursor = db.query(FavoriteMoviesContract.MovieEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        favoritesCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return favoritesCursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        Uri returnUri;
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);

        switch (match) {
            case FAVORITE_MOVIES:
                long id = db.insert(FavoriteMoviesContract.MovieEntry.TABLE_NAME, null, contentValues);
                if (id>0) {
                    returnUri = ContentUris.withAppendedId(FavoriteMoviesContract.MovieEntry.CONTENT_URI, id);
                } else {
                    throw new SQLiteException("Failed to insert row into " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String where, @Nullable String[] whereArgs) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        int rowsDeleted;

        switch (match) {
            case SINGLE_FAVORITE_MOVIE:
                rowsDeleted = db.delete(FavoriteMoviesContract.MovieEntry.TABLE_NAME, where, whereArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }
}
