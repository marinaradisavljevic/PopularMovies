package com.mcodefactory.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.mcodefactory.popularmovies.data.Movie;
import com.mcodefactory.popularmovies.fragments.FavoritesFragment;
import com.mcodefactory.popularmovies.fragments.PopularMoviesFragment;
import com.mcodefactory.popularmovies.fragments.TopRatedFragment;

public class MainActivity extends AppCompatActivity implements OnSelectMovieListener {

    private static final String MOVIE_KEY = "movie";

    //local variable used to trigger changes in the selection criterion from top rated to most popular to favorites
    private String choice;
    public static final String CHOICE_KEY = "choice";
    public static final String POPULAR = "popular";
    public static final String TOP_RATED = "top_rated";
    public static final String FAVORITES = "favorites";

    public static final int REQUEST_CODE = 10;
    public static final String COLLECTION_CHANGED_KEY = "changed";

    private FragmentManager fragmentManager;
    private boolean isRestored;
    public static Context appContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appContext = getApplicationContext();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fragmentManager = getSupportFragmentManager();
        if (savedInstanceState != null) {
            isRestored = true;
            choice = savedInstanceState.getString(CHOICE_KEY);
        } else {
            isRestored = false;
        }

    }

    @Override
    public void onMovieSelected(Movie movie) {
        Intent intent = new Intent();
        Context context = this;
        Class activityClass = DetailsActivity.class;
        intent.setClass(context, activityClass);
        //send the Movie object to the Details activity
        intent.putExtra(MOVIE_KEY, movie);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && choice.equals(FAVORITES)) {
            boolean collectionChanged = data.getBooleanExtra(COLLECTION_CHANGED_KEY, false);
            if (collectionChanged) {
                clearBackStack();
                FavoritesFragment fragment = new FavoritesFragment();
                fragmentManager.beginTransaction().replace(R.id.collection_fragment_container,
                        fragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
            }
        }
    }

    private void clearBackStack() {
        int backStackEntries = fragmentManager.getBackStackEntryCount();
        for (int i = 0; i < backStackEntries; i++) {
            fragmentManager.popBackStack();
        }
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

                Object item = adapterView.getItemAtPosition(position);
                if (isRestored && !choice.equals(FAVORITES)) {
                    isRestored = false;
                    return;
                }
                if (item != null) {
                    if (position == 0) {
                        choice = POPULAR;
                        PopularMoviesFragment fragment = new PopularMoviesFragment();
                        fragmentManager.beginTransaction().replace(R.id.collection_fragment_container,
                                fragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).
                                commit();
                    }
                    if (position == 1) {
                        choice = TOP_RATED;
                        TopRatedFragment fragment = new TopRatedFragment();
                        fragmentManager.beginTransaction().replace(R.id.collection_fragment_container,
                                fragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).
                                commit();
                    }
                    if (position == 2) {
                        choice = FAVORITES;
                        FavoritesFragment fragment = new FavoritesFragment();
                        fragmentManager.beginTransaction().replace(R.id.collection_fragment_container,
                                fragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).
                                commit();
                    }

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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CHOICE_KEY, choice);
    }
}