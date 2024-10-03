package com.victorpy.iotvideoapp.models;

import androidx.annotation.NonNull;

public class Track {
    private String id;
    private String title;
    private String url;

    public String getUrl() {
        return url;
    }

    public Track(String id, String title, String url) {
        this.id = id;
        this.title = title;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    @NonNull
    @Override
    public String toString() {
        return "Track{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}