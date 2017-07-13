package com.mcodefactory.popularmovies;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

public abstract class EndlessScrollListener extends RecyclerView.OnScrollListener {
    // The minimum number of items to have below current scroll position
    private int visibleThreshold = 5;
    // The current result page
    private int currentPage = 0;
    // The total number of items in the collection after the last load
    private int previousTotalItemCount = 0;
    // True if not all data has been loaded
    private boolean loading = true;
    // Starting page index
    public static final int STARTING_PAGE_INDEX = 1;
    // Maximum number of pages to load
    public static final int MAX_PAGES = 5;

    GridLayoutManager layoutManager;


    public EndlessScrollListener(GridLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();
        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

        // If the total item count is zero and the previous isn't, reset back to initial state
        if (totalItemCount < previousTotalItemCount) {
            this.currentPage = STARTING_PAGE_INDEX;
            this.previousTotalItemCount = totalItemCount;
            if (totalItemCount == 0) {
                this.loading = true;
            }
        }

        // If it's still loading, check to see if the collection count has changed and update the total item count.
        if (loading && (totalItemCount > previousTotalItemCount)) {
            loading = false;
            previousTotalItemCount = totalItemCount;
        }

        if (!loading && (firstVisibleItemPosition + visibleItemCount + visibleThreshold) >= totalItemCount) {
            loading = onLoadMore(currentPage + 1, totalItemCount);
        }
    }

    // Returns true if more data is being loaded; returns false if there is no more data to load.
    public abstract boolean onLoadMore(int page, int totalItemsCount);

    @Override
    public void onScrollStateChanged(RecyclerView view, int scrollState) {
        // Don't take any action on changed
    }

    public int getPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        currentPage = page;
    }

    public void setLoading(boolean isLoading) {
        loading = isLoading;
    }
}
