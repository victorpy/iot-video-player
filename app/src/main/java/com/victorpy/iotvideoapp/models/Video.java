package com.victorpy.iotvideoapp.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Video implements Parcelable {
    private String title;
    private String url;

    public Video(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    // Parcelable implementation
    protected Video(Parcel in) {
        title = in.readString();
        url = in.readString();
        // Add other fields as necessary
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(url);
        // Write other fields as necessary
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Video> CREATOR = new Creator<Video>() {
        @Override
        public Video createFromParcel(Parcel in) {
            return new Video(in);
        }

        @Override
        public Video[] newArray(int size) {
            return new Video[size];
        }
    };
}
