package com.victorpy.iotvideoapp.managers;
import android.content.Context;

import com.google.android.exoplayer2.ExoPlayer;
public class ExoPlayerManager {
    private static ExoPlayerManager instance;
    private ExoPlayer exoPlayer;

    private ExoPlayerManager(Context context) {
        Context appContext = context.getApplicationContext();
        exoPlayer = new ExoPlayer.Builder(appContext).build();
    }

    public static synchronized ExoPlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new ExoPlayerManager(context);
        }
        return instance;
    }

    public ExoPlayer getPlayer() {
        return exoPlayer;
    }

    public void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
            instance = null;
        }
    }
}
