package com.mcodefactory.popularmovies.utils;

/**
 * Retrofit's interface with call definitions and paths
 */

import com.mcodefactory.popularmovies.api_result_models.DetailResult;
import com.mcodefactory.popularmovies.api_result_models.MovieResult;
import com.mcodefactory.popularmovies.api_result_models.ReviewResult;
import com.mcodefactory.popularmovies.api_result_models.VideoResult;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiInterface {

    @GET("movie/top_rated")
    Call<MovieResult> getTopRatedMovies(@Query("api_key") String apiKey);

    @GET("movie/popular")
    Call<MovieResult> getMostPopularMovies(@Query("api_key") String apiKey);

    @GET("movie/{id}")
    Call<DetailResult> getMovieDetails(@Path("id") int id, @Query("api_key") String apiKey);

    @GET("movie/{id}/reviews")
    Call<ReviewResult> getMovieReviews(@Path("id") int id, @Query("api_key") String apiKey);

    @GET("movie/{id}/videos")
    Call<VideoResult> getMovieVideos(@Path("id") int id, @Query("api_key") String apiKey);
}
