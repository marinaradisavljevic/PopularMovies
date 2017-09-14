package com.mcodefactory.popularmovies.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mcodefactory.popularmovies.R;
import com.mcodefactory.popularmovies.adapters.ReviewRVAdapter;
import com.mcodefactory.popularmovies.data.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewsFragment extends Fragment {

    private RecyclerView mRecyclerView;
    public ReviewRVAdapter mAdapter;
    private TextView mErrorMessageDisplay;

    List<Review> reviewList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reviews_fragment, container, false);
        readArguments(getArguments());
        mErrorMessageDisplay = (TextView) view.findViewById(R.id.tv_error_message_display);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.review_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(),
                LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(false);
        mAdapter = new ReviewRVAdapter();
        mAdapter.setData(reviewList);
        if (reviewList == null || reviewList.size() < 1) {
            showErrorMessage();
        }
        mRecyclerView.setAdapter(mAdapter);
        return view;
    }

    public static ReviewsFragment newInstance(List<Review> reviews) {
        Bundle args = new Bundle();
        args.putParcelableArrayList("reviews", (ArrayList) reviews);
        ReviewsFragment fragment = new ReviewsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void readArguments(Bundle args) {
        if (args != null) {
            reviewList = args.getParcelableArrayList("reviews");
        }
    }

    private void showErrorMessage() {
        mRecyclerView.setVisibility(View.GONE);
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }
}
