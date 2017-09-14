package com.mcodefactory.popularmovies.data;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

public class Movie implements Parcelable {

    @SerializedName("title")
    private String title;
    @SerializedName("original_title")
    private String originalTitle;
    @SerializedName("overview")
    private String synopsis;
    @SerializedName("vote_average")
    private double voteAverage;
    @SerializedName("release_date")
    private String releaseDate;
    public int releaseYear;
    @SerializedName("poster_path")
    private String posterPath;
    @SerializedName("id")
    private int id;
    @SerializedName("runtime")
    private int runtime;
    private boolean isFavorite;
    private int localID;
    @SerializedName("reviewNo")
    private int reviewNo;

    public Movie() {

    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public double getVoteAverage() {
        return voteAverage;
    }

    public void setVoteAverage(double voteAverage) {
        this.voteAverage = voteAverage;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    //parse the String value returned from the server into the release year
    public void setReleaseYear(String releaseDate) {
        this.releaseYear = parseDate(releaseDate);
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLocalID() {
        return localID;
    }

    public void setLocalID(int localDbId) {
        this.localID = localDbId;
    }

    public String getOriginalTitle() { return originalTitle; }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public int getRuntime() {
        return runtime;
    }

    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public void setReviewNo(int number) {
        reviewNo = number;
    }

    public int getReviewNo() {
        return reviewNo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return "Original title: " + originalTitle + " release year: " + releaseYear;
    }

    private static int parseDate(String date) {
        String[] values = date.split("-");
        return Integer.valueOf(values[0]);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.title);
        dest.writeString(this.originalTitle);
        dest.writeString(this.synopsis);
        dest.writeDouble(this.voteAverage);
        dest.writeInt(this.releaseYear);
        dest.writeString(this.posterPath);
        dest.writeInt(this.id);
        dest.writeInt(this.runtime);
        dest.writeInt(this.reviewNo);

    }

    protected Movie(Parcel in) {
        this.title = in.readString();
        this.originalTitle = in.readString();
        this.synopsis = in.readString();
        this.voteAverage = in.readDouble();
        this.releaseYear = in.readInt();
        this.posterPath = in.readString();
        this.id = in.readInt();
        this.runtime = in.readInt();
        this.reviewNo = in.readInt();
    }

    public static final Parcelable.Creator<Movie> CREATOR = new Parcelable.Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel source) {
            return new Movie(source);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };
}
