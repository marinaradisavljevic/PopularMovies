package com.mcodefactory.popularmovies.api_result_models;

import com.google.gson.annotations.SerializedName;
import com.mcodefactory.popularmovies.data.Review;

import java.util.List;

/**
 * API call result model class for movie reviews
 */

public class ReviewResult {

    @SerializedName("page")
    private int page;
    @SerializedName("total_pages")
    private int totalPages;
    @SerializedName("total_results")
    private int totalReviews;
    @SerializedName("results")
    private List<Review> reviews;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }
}
