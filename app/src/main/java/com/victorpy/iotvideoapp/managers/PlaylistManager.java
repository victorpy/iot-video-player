package com.victorpy.iotvideoapp.managers;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.victorpy.iotvideoapp.models.Video;

import java.util.ArrayList;
import java.util.List;

public class PlaylistManager {
    private static PlaylistManager instance;
    private static final String TAG = "PlaylistManager";
    private final MutableLiveData<ArrayList<Video>> videoList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> currentVideoIndex = new MutableLiveData<>(0);
    // Private constructor to prevent instantiation
    private PlaylistManager() {}

    // Public method to provide access to the singleton instance
    public static synchronized PlaylistManager getInstance(ArrayList<Video> videoList) {
        if (instance == null) {
            Log.d(TAG, "instance created");
            instance = new PlaylistManager();
        }
        return instance;
    }

    // Public method to get the singleton instance
    public static synchronized PlaylistManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PlaylistManager not initialized. Call getInstance(videoList) first.");
        }
        return instance;
    }

    public LiveData<ArrayList<Video>> getVideoList() {
        ArrayList<Video> videos = videoList.getValue();
        assert videos != null;
        Log.d(TAG, "getVideoList called, returning " + videos.size() + " videos");
        return videoList;
    }

    public void setVideoList(ArrayList<Video> videos) {
        Log.d(TAG, "setVideoList");
        if (videos != null) {
            videoList.setValue(videos);
            Log.d(TAG, "setVideoList video list size: "+videos.size());
        }
    }

    public LiveData<Integer> getCurrentVideoIndex() {
        return currentVideoIndex;
    }

    public void setCurrentVideoIndex(int index) {
        Log.d(TAG, "set video index: "+index);
        currentVideoIndex.setValue(index);
    }

    public int nextVideo() {
        Log.d(TAG, "next video, current index: "+currentVideoIndex);
        Integer currentIndex = currentVideoIndex.getValue();
        ArrayList<Video> videos = videoList.getValue();
        assert videos != null;
        Log.d(TAG, "next video list size: "+videos.size());
        if(currentIndex == null) return -1;

        if(currentIndex < videos.size() - 1) {
            currentIndex++;
            setCurrentVideoIndex(currentIndex);
        } else {
            Log.d(TAG, "nextVideo Current index: "+currentIndex);
        }
        return currentIndex;
    }

    public void prevVideo() {
        Integer currentIndex = currentVideoIndex.getValue();
        if (currentIndex != null && currentIndex > 0) {
            currentIndex--;
            setCurrentVideoIndex(currentIndex);
        }
    }

    public Video getCurrentVideo() {
        // Get the current values of videoList and currentVideoIndex
        ArrayList<Video> videos = videoList.getValue();
        Integer index = currentVideoIndex.getValue();

        // Check if the index and list are valid before accessing the video
        if (videos != null && index != null && index >= 0 && index < videos.size()) {
            return videos.get(index);
        }
        return null; // Return null if no valid video is found
    }
}
