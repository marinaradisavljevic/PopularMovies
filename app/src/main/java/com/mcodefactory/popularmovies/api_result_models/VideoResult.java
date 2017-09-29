package com.mcodefactory.popularmovies.api_result_models;

import com.google.gson.annotations.SerializedName;
import com.mcodefactory.popularmovies.data.Trailer;

import java.util.List;

/**
 * API call result model class for the movie video collection
 */

public class VideoResult {

    @SerializedName("id")
    private int id;
    @SerializedName("results")
    private List<Trailer> videos;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Trailer> getVideos() {
        return videos;
    }

    public void setVideos(List<Trailer> videos) {
        this.videos = videos;
    }
}
