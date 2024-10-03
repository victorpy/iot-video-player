package com.victorpy.iotvideoapp.managers;

import android.os.Bundle;
import android.util.Log;

import com.victorpy.iotvideoapp.VideoPlayerFragment;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;

public class VideoPlayerFragmentFactory extends FragmentFactory {
    // Static instance for Singleton
    private static VideoPlayerFragmentFactory instance;
    private VideoPlayerFragment videoPlayerFragment;

    private final String TAG = "VideoPlayerFragmentFactory";

    // Private constructor to prevent direct instantiation
    private VideoPlayerFragmentFactory() {}

    // Static method to get the single instance
    public static synchronized VideoPlayerFragmentFactory getInstance() {
        if (instance == null) {
            instance = new VideoPlayerFragmentFactory();
        }
        return instance;
    }

    @NonNull
    @Override
    public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
        if (className.equals(VideoPlayerFragment.class.getName())) {
            if (videoPlayerFragment == null) {
                Log.d(TAG, "creating a new video player fragment");
                // Create a new instance of VideoPlayerFragment
                videoPlayerFragment = new VideoPlayerFragment();
                // Initialize VideoPlayerFragment if needed
                Bundle args = new Bundle();
                // Set any default arguments if needed
                videoPlayerFragment.setArguments(args);
            } else {
                Log.d(TAG, "reusing existing video player fragment");
            }
            return videoPlayerFragment;
        }
        return super.instantiate(classLoader, className);
    }

    public VideoPlayerFragment getVideoPlayerFragment(){
        return videoPlayerFragment;
    }
}
