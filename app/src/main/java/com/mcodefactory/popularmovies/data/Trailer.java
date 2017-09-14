package com.mcodefactory.popularmovies.data;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Trailer object POJO
 */

public class Trailer implements Serializable {

    @SerializedName("name")
    private String name;
    @SerializedName("type")
    private String type;
    @SerializedName("key")
    private String key;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
