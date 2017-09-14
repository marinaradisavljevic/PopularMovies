package com.mcodefactory.popularmovies;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.mcodefactory.popularmovies.data.Movie;
import com.mcodefactory.popularmovies.data.Review;
import com.mcodefactory.popularmovies.fragments.DetailsFragment;
import com.mcodefactory.popularmovies.fragments.ReviewsFragment;
import com.mcodefactory.popularmovies.utils.NetworkUtils;

import java.util.List;

public class DetailsActivity extends ActionBarActivity implements DetailsFragment.OnSelectedListener {

    Movie movie;
    ReviewsFragment reviewsFragment;
    FragmentManager fragmentManager;
    boolean isDualPane;
    boolean removedFromFavorites;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        Intent intent = getIntent();
        Bundle data = intent.getExtras();
        if (data != null && intent.hasExtra("movie")) {
            movie = data.getParcelable("movie");
        }

        fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        isDualPane = getResources().getBoolean(R.bool.dual_pane_mode);
        removedFromFavorites = false;
        if (isDualPane) {
            if (fragmentManager.findFragmentById(R.id.details_container) == null) {
                DetailsFragment detailsFragment = DetailsFragment.newInstance(movie, isDualPane);
                transaction.replace(R.id.details_container, detailsFragment, null);
            }
            reviewsFragment = new ReviewsFragment();
            transaction.replace(R.id.reviews_container, reviewsFragment, null);
            transaction.commit();
        } else {
            if (fragmentManager.findFragmentById(R.id.details_container) == null) {
                DetailsFragment detailsFragment = DetailsFragment.newInstance(movie, isDualPane);
                transaction.replace(R.id.fragment_container, detailsFragment, null).commit();
            }

        }
        clearBackStack();

    }

    private void clearBackStack() {
        int backStackEntries = fragmentManager.getBackStackEntryCount();
        for (int i = 0; i < backStackEntries; i++) {
            fragmentManager.popBackStack();
        }
    }

    @Override
    public void playVideo(String tag) {
        Intent intent = new Intent
                (Intent.ACTION_VIEW, Uri.parse(NetworkUtils.YOUTUBE_BASE_URL.concat(tag)));
        //check if the Youtube app is present on the device
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            //display a message to the user that the video cannot be played on the device
            Toast.makeText(this, R.string.no_app_for_videos, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void displayReviews(List<Review> reviews) {
        isDualPane = getResources().getBoolean(R.bool.dual_pane_mode);
        if (reviews != null) {
            if (!isDualPane) {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                reviewsFragment = ReviewsFragment.newInstance(reviews);
                transaction.replace(R.id.fragment_container, reviewsFragment, null).commit();
            } else {
                reviewsFragment = ReviewsFragment.newInstance(reviews);
                getSupportFragmentManager().beginTransaction().replace(R.id.reviews_container,
                        reviewsFragment, null).commit();
            }
        }
    }

    @Override
    public void removedFromFavorites() {
        removedFromFavorites = true;
    }


    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(MainActivity.COLLECTION_CHANGED_KEY, removedFromFavorites);
        setResult(MainActivity.REQUEST_CODE, resultIntent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("movie", movie);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            movie = savedInstanceState.getParcelable("movie");
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            isDualPane = getResources().getBoolean(R.bool.dual_pane_mode);
            if (isDualPane) {

                if (fragmentManager.findFragmentById(R.id.details_container) == null) {
                    DetailsFragment detailsFragment = DetailsFragment.newInstance(movie, isDualPane);
                    transaction.replace(R.id.details_container, detailsFragment, null);
                }

                reviewsFragment = new ReviewsFragment();
                transaction.replace(R.id.reviews_container, reviewsFragment, null);
                transaction.commit();

            } else {
                clearBackStack();
                DetailsFragment detailsFragment = DetailsFragment.newInstance(movie, isDualPane);
                transaction.replace(R.id.fragment_container, detailsFragment, null).commit();

            }

        }
    }

}
