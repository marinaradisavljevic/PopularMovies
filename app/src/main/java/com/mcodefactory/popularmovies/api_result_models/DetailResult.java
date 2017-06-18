package com.mcodefactory.popularmovies.api_result_models;

import com.google.gson.annotations.SerializedName;

/**
 * Retrofit's model class for the movie details call
 */

public class DetailResult {

    @SerializedName("runtime")
    private int runtime;
    @SerializedName("original_language")
    private String originalLanguage;

    public int getRuntime() {
        return runtime;
    }

    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }
}
