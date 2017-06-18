package com.mcodefactory.popularmovies;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mcodefactory.popularmovies.adapters.ReviewRVAdapter;
import com.mcodefactory.popularmovies.data.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewActivity extends AppCompatActivity {

    public static final String TAG = ReviewActivity.class.getSimpleName();
    private RecyclerView mRecyclerView;
    public ReviewRVAdapter mAdapter;
    private TextView mErrorMessageDisplay;
    List<Review> reviews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mRecyclerView = (RecyclerView) findViewById(R.id.review_recycler_view);
        mErrorMessageDisplay = (TextView) findViewById(R.id.tv_error_message_display);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(false);
        mAdapter = new ReviewRVAdapter();
        mRecyclerView.setAdapter(mAdapter);

        reviews = new ArrayList<>();
        Bundle data = getIntent().getExtras();
        if (data.containsKey("bundle")) {
            Bundle bundle = data.getBundle("bundle");
            if (bundle != null) {
                //extract the movie title and the list of Review objects from the bundle
                getSupportActionBar().setTitle(bundle.getString("title"));
                reviews = bundle.getParcelableArrayList("reviews");
                Log.d(TAG, "Number of reviews obtained: " + reviews.size());
            } else {
                showErrorMessage();
            }
        }
        loadReviews();
    }

    private void loadReviews() {
        showMovieReviewView();
        mAdapter.setData(reviews);
    }

    public void showMovieReviewView() {
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showErrorMessage () {
        mRecyclerView.setVisibility(View.INVISIBLE);
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }



}
