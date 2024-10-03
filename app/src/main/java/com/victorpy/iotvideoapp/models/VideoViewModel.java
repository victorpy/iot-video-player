package com.victorpy.iotvideoapp.models;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.victorpy.iotvideoapp.managers.PlaylistManager;

import java.util.ArrayList;

public class VideoViewModel extends ViewModel {

    private static final String TAG = "VideoViewModel";
    private static final String KEY_VIDEO_LIST = "video_list";
    private PlaylistManager playlistManager;
    private MutableLiveData<String> currentVideoUrl = new MutableLiveData<>();
    private final SavedStateHandle savedStateHandle;

    public VideoViewModel(SavedStateHandle savedStateHandle) {
        Log.d(TAG,"Constructor, initializing");
        this.savedStateHandle = savedStateHandle;

        // Check if there's a saved video list, else use PlaylistManager's list
        if (savedStateHandle.contains(KEY_VIDEO_LIST)) {
            // If there's a saved list, restore it
            ArrayList<Video> savedVideos = savedStateHandle.get(KEY_VIDEO_LIST);
            assert savedVideos != null;
            Log.d(TAG,"Restoring saved videos list, size: "+savedVideos.size());
            playlistManager = PlaylistManager.getInstance(savedVideos);
        }
    }

    // Save the video list when needed
    public void saveVideoList(ArrayList<Video> videoList) {
        Log.d(TAG,"Saving video list to state handler");
        savedStateHandle.set(KEY_VIDEO_LIST, videoList);
    }

    public LiveData<String> getCurrentVideoUrl() {
        return currentVideoUrl;
    }

    public void setCurrentVideoUrl(String url) {
        currentVideoUrl.setValue(url);
    }

    public LiveData<ArrayList<Video>> getVideoList() {
        if (playlistManager != null) {
            return playlistManager.getVideoList();
        } else {
            // Return an empty LiveData to prevent null issues
            return new MutableLiveData<>(new ArrayList<>());
        }
    }

    public void setVideoList(ArrayList<Video> videos) {
        Log.d(TAG, "setVideoList called with " + videos.size() + " videos");
        playlistManager = PlaylistManager.getInstance(videos);
        playlistManager.setVideoList(videos);
        saveVideoList(videos);
    }

    public LiveData<Integer> getCurrentVideoIndex() {
        return playlistManager.getCurrentVideoIndex();
    }

    public Video getCurrentVideo(){
        if(playlistManager != null)
            return playlistManager.getCurrentVideo();
        else
            return null;
    }

    public void setCurrentVideoIndex(int index) {
        Log.d(TAG, "set current video index: "+index);
        playlistManager.setCurrentVideoIndex(index);
    }

    public int nextVideo() {
        return playlistManager.nextVideo();
    }

    public void prevVideo() {
        playlistManager.prevVideo();
    }
}
