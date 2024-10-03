package com.victorpy.iotvideoapp.services;

// MediaPlaybackService.java
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.victorpy.iotvideoapp.MainActivity;
import com.victorpy.iotvideoapp.R;
import com.victorpy.iotvideoapp.managers.ExoPlayerManager;
import com.victorpy.iotvideoapp.managers.PlaylistManager;
import com.victorpy.iotvideoapp.models.Video;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class MediaPlaybackService extends Service {

    private final String APP_NAME = "com.victorpy.iotvideoapp";
    private final String TRACK_CHANGE = ".TRACK_CHANGE";
    private final String VIDEO_API_SERVICE_ERROR = ".VIDEO_API_SERVICE_ERROR";
    private final String TAG = "MediaPlaybackService";
    private static final String CHANNEL_ID = "MediaPlaybackChannel";
    private ExoPlayer exoPlayer;
    private final IBinder binder = new LocalBinder();
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_NEXT_TRACK = "ACTION_NEXT_TRACK";

    private boolean isAutoplayEnabled;
    private ApiService apiService;
    private Handler mainHandler;
    private static boolean isServiceRunning = false;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;


    public void setCurrentIndex(int currentIndex) {
    }

    public class LocalBinder extends Binder {
        public MediaPlaybackService getService() {
            return MediaPlaybackService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        // Create notification channel for the foreground service
        createNotificationChannel();
        // Initialize ExoPlayer
        exoPlayer = ExoPlayerManager.getInstance(getApplicationContext()).getPlayer(); // Ensure single instance
        initAudioFocus(); // Initialize AudioManager and AudioFocusRequest

        // Set up listener to detect when a track finishes playing
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                switch (state) {
                    case Player.STATE_IDLE:
                        Log.d(TAG, "ExoPlayer state: STATE_IDLE");
                        break;
                    case Player.STATE_BUFFERING:
                        Log.d(TAG, "ExoPlayer state: STATE_BUFFERING");
                        break;
                    case Player.STATE_READY:
                        Log.d(TAG, "ExoPlayer state: STATE_READY");
                        break;
                    case Player.STATE_ENDED:
                        Log.d(TAG, "ExoPlayer state: STATE_ENDED");
                        if (isAutoplayEnabled()) {
                            Log.d(TAG, "Track ended, trying to play the next song.");
                            playNextTrack(); // Play the next track when the current one ends
                        }
                        break;
                    default:
                        Log.d(TAG, "ExoPlayer state: UNKNOWN");
                        break;
                }
            }
        });

        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headphoneReceiver, filter);

        startForeground(1, createNotification());
        isServiceRunning = true;

        Log.d(TAG, "Initialized");

    }

    public static boolean isServiceRunning() {
        return isServiceRunning;  // Method to check if the service is running
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Log when the service is being started or restarted
        Log.d(TAG, "onStartCommand called, intent: " + intent);

        // Handle commands from the intent if needed
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_PLAY:
                        //Only if something was already playing and paused, we will continue playing
                        resumeMedia();
                        updateNotification();
                        break;
                    case ACTION_PAUSE:
                        pauseMedia();
                        updateNotification();
                        break;
                    case ACTION_NEXT_TRACK:
                        playNextTrack();
                        updateNotification();
                    case "STOP_MEDIA":
                        stopMedia();
                        stopSelf(); // Stop the service if explicitly requested
                        break;
                    // Handle other actions
                }
            }
        }

        // Return START_STICKY to ensure the service is restarted if killed by the system
        return START_STICKY;
    }

    private void initAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Audio focus change listener
        AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        // Stop playback or pause when audio focus is permanently lost (like a phone call)
                        pauseMedia();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        // Pause when audio focus is temporarily lost (like a notification sound)
                        pauseMedia();
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        // Resume playback when audio focus is regained
                        resumeMedia();
                        break;
                }
            }
        };

        // Create an AudioFocusRequest for API 26 and above
        audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(afChangeListener)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(true)
                .build();
    }

    private boolean requestAudioFocus() {
        {
            int result = audioManager.requestAudioFocus(audioFocusRequest);
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    private void releaseAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest);
    }

    private BroadcastReceiver headphoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        // Headphones disconnected, pause media
                        pauseMedia();
                        break;
                    case 1:
                        // Headphones connected
                        break;
                }
            }
        }
    };

    private void updateNotification() {
        // Call your createNotification method to rebuild the notification
        Log.d(TAG, "Updating notification");
        Notification notification = createNotification();
        startForeground(1, notification);
    }

    private void broadcastTrackChange(int nextTract) {
        Intent intent = new Intent(APP_NAME+TRACK_CHANGE);
        intent.putExtra("nextTrack", nextTract);
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        // Create the notification channel if necessary
        NotificationChannel channel = new NotificationChannel(
                "media_playback_channel",
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }

        // Create the notification for the foreground service
        Notification notification = new NotificationCompat.Builder(this, "media_playback_channel")
                .setContentTitle("Media Playback")
                .setContentText("Playing media...")
                .setSmallIcon(R.drawable.ic_play_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("showVideoPlayer", true);
        notificationIntent.putExtra("FROM_NOTIFICATION", true);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("IOT Player")
                .setContentText("Playing media")
                .setSmallIcon(R.drawable.ic_play_foreground) // Use your own icon
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Ensure it's high priority
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle() // Use MediaStyle for expanded notifications
                        .setShowActionsInCompactView(0, 1, 2))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(new NotificationCompat.Action(
                        R.drawable.icons8_pause_24, // Pause icon
                        "Pause",
                        createPauseIntent())) // PendingIntent for pause action
                .addAction(new NotificationCompat.Action(
                        R.drawable.icons8_play_24, // Play icon
                        "Play",
                        createPlayIntent()))
                .addAction(new NotificationCompat.Action(
                        R.drawable.icons8_next_24, // Next track icon
                        "Next",
                        createNextTrackIntent()))
                .build();
    }

    // Create pending intents for play and pause actions
    private PendingIntent createPlayIntent() {
        Intent playIntent = new Intent(this, MediaPlaybackService.class);
        playIntent.setAction(ACTION_PLAY);
        return PendingIntent.getService(this, 0, playIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0);
    }

    private PendingIntent createPauseIntent() {
        Intent pauseIntent = new Intent(this, MediaPlaybackService.class);
        pauseIntent.setAction(ACTION_PAUSE);
        return PendingIntent.getService(this, 0, pauseIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0);
    }
    private PendingIntent createNextTrackIntent() {
        Intent nextTrackIntent = new Intent(this, MediaPlaybackService.class);
        nextTrackIntent.setAction(ACTION_NEXT_TRACK); // Custom action for next track
        return PendingIntent.getService(this, 0, nextTrackIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0);
    }

    public void setAutoplayEnabled(boolean enable) {
        isAutoplayEnabled = enable;
    }
    private boolean isAutoplayEnabled() {
        return isAutoplayEnabled;
    }

    public void fetchAndPlayVideo(Video video) {
        apiService = new ApiService(getApplicationContext());
        // Fetch video details using the URL in the Video object
        apiService.getVideoDetails(video.getUrl(), new ApiService.VideoDetailsCallback() {
            @Override
            public void onSuccess(JSONObject videoDetails) {

                    try {
                        Log.d(TAG, "Response body fetchAndPlayVideo: "+videoDetails);
                        // Extract the playable URL from the JSON response
                        JSONObject encodingsInfo = videoDetails.getJSONObject("encodings_info");
                        JSONObject encoding360 = encodingsInfo.getJSONObject("360");
                        JSONObject h264 = encoding360.getJSONObject("h264");
                        String videoUrl = h264.getString("url");

                        // Play the video
                        String fullUrl = apiService.buildFullUrl(videoUrl);
                        Log.d(TAG, "Media URL: " + fullUrl);
                        playMedia(fullUrl);
                    } catch (JSONException e) {
                        Log.e("VideoPlayerFragment", "Failed to parse video details", e);
                    }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to parse video details "+error);

                // Broadcast the error message
                Intent intent = new Intent(APP_NAME+VIDEO_API_SERVICE_ERROR);
                intent.putExtra("errorMessage", error);
                sendBroadcast(intent);
            }
        });
    }

    /**
     * Plays the next track in the playlist.
     */
    private void playNextTrack() {
        Log.d(TAG, "playNextTrack");
        PlaylistManager playlistManager = PlaylistManager.getInstance();
        // Get the next index from the playlist manager
        int nextIndex = playlistManager.nextVideo();
        Video nextVideo = playlistManager.getCurrentVideo();

        // Log the next index for debugging purposes
        Log.d(TAG, "Next video index: " + nextIndex);

        // Get the current video after advancing to the next

        if (nextVideo != null) {
            Log.d(TAG, "Next video url: " + nextVideo.getUrl());
            playlistManager.setCurrentVideoIndex(nextIndex);
            fetchAndPlayVideo(nextVideo);
        } else {
            // Handle end of playlist if needed
            // Optional: Reset to the first track or stop playback
            playlistManager.setCurrentVideoIndex(0);
            // Notify the user or log the end of the playlist
        }
    }

    public void playMedia(String url) {
        if (mainHandler == null ) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        mainHandler.post(() -> {
            if (requestAudioFocus()) {
                MediaItem mediaItem = MediaItem.fromUri(url);
                exoPlayer.setMediaItem(mediaItem);
                exoPlayer.prepare();
                exoPlayer.play();
            } else {
                // Handle focus not being granted (e.g., show an error message or retry later)
                Log.e(TAG, "Audio focus not granted, cannot play media.");
            }
        });

    }

    public void pauseMedia() {
        if (exoPlayer != null) {
            exoPlayer.pause();
        }
    }

    public void stopMedia() {
        if (exoPlayer != null) {
            //exoPlayer.setPlayWhenReady(false);
            exoPlayer.stop();
            exoPlayer.seekTo(0);
            //exoPlayer.clearMediaItems();
        }
    }

    public void resumeMedia() {
        if (requestAudioFocus()) {
            if (exoPlayer != null && !exoPlayer.isPlaying()) {
                exoPlayer.play();
            }
        } else {
            // Handle focus not being granted (e.g., show an error message or retry later)
            Log.e(TAG, "Audio focus not granted, cannot resume media playback.");
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "MediaPlaybackService Destroyed");
        super.onDestroy();
        releaseAudioFocus();
        if (exoPlayer != null) {
            exoPlayer.release();
        }
        unregisterReceiver(headphoneReceiver);
        // Stop foreground service and remove notification
        stopForeground(true);
    }

    public ExoPlayer getExoPlayer() {
        return exoPlayer;
    }
}
