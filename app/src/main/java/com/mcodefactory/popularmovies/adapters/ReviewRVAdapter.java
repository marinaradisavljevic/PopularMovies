package com.mcodefactory.popularmovies.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mcodefactory.popularmovies.R;
import com.mcodefactory.popularmovies.data.Review;

import java.util.List;

/**
 * Adapter for the Review recyclerView in the ReviewActivity
 * Takes a List of Review objects and loads the items
 */

public class ReviewRVAdapter extends RecyclerView.Adapter<ReviewRVAdapter.ReviewRVAdapterViewHolder>  {
    List<Review> reviews;


    public class ReviewRVAdapterViewHolder extends RecyclerView.ViewHolder {
        public final TextView author;
        public final TextView content;

        public ReviewRVAdapterViewHolder(View view) {
            super(view);
            author= (TextView) view.findViewById(R.id.author);
            content = (TextView) view.findViewById(R.id.tv_content);
        }

    }

    @Override
    public ReviewRVAdapter.ReviewRVAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.review_rv_layout;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
        return new ReviewRVAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ReviewRVAdapterViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.author.setText(review.getAuthor());
        holder.content.setText(review.getReviewText());
    }

    @Override
    public int getItemCount() {
        if (reviews==null) return 0;
        return reviews.size();
    }

    public void setData(List<Review> reviewList) {
        reviews = reviewList;
        notifyDataSetChanged();
    }
}
