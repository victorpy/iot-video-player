package com.victorpy.iotvideoapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ServiceConnection;
import android.content.ComponentName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.victorpy.iotvideoapp.adapters.VideoListAdapter;
import com.victorpy.iotvideoapp.managers.PlaylistManager;
import com.victorpy.iotvideoapp.models.Video;
import com.victorpy.iotvideoapp.models.VideoViewModel;
import com.victorpy.iotvideoapp.services.MediaPlaybackService;

import java.util.ArrayList;
import java.util.Objects;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class VideoPlayerFragment extends Fragment {

    private static final String TAG = "VideoPlayerFragment";
    private StyledPlayerView playerView;
    private MediaPlaybackService mediaPlaybackService;
    private boolean isBound = false;
    private ExoPlayer exoPlayer;
    private Button stopButton, prevButton, nextButton;
    private ImageButton playPauseButton;
    private VideoViewModel videoViewModel;
    private RecyclerView videoListRecyclerView;
    private TextView playlistTitleTextView;
    private VideoListAdapter videoListAdapter;
    private boolean playNext = false;
    private boolean playPrev = false;
    private boolean isAutoplayEnabled = true; // Variable to track autoplay state
    private final String APP_NAME = "com.victorpy.iotvideoapp";
    private final String TRACK_CHANGE = ".TRACK_CHANGE";
    private final String VIDEO_API_SERVICE_ERROR = ".VIDEO_API_SERVICE_ERROR";
    private boolean fromForeground = false;


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlaybackService.LocalBinder binder = (MediaPlaybackService.LocalBinder) service;
            mediaPlaybackService = binder.getService();
            isBound = true;
            Log.d(TAG, "MediaPlaybackService bound");
            // Retrieve ExoPlayer instance and set it to the player view
            if (mediaPlaybackService != null && mediaPlaybackService.getExoPlayer() != null) {
                exoPlayer = mediaPlaybackService.getExoPlayer();
                playerView.setPlayer(exoPlayer);

                updatePlayPauseButton();

                // Add listener to monitor the player's state
                mediaPlaybackService.getExoPlayer().addListener(new Player.Listener() {
                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        playPauseButton.setImageResource(isPlaying ? R.drawable.icons8_pause_24 : R.drawable.icons8_play_24);
                    }
                });
            }
            if(isAutoplayEnabled)
                enableAutoplay();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private void updatePlayPauseButton() {
        boolean isPlaying = mediaPlaybackService.getExoPlayer().isPlaying();
        playPauseButton.setImageResource(isPlaying ? R.drawable.icons8_pause_24 : R.drawable.icons8_play_24);
        Log.d(TAG, "Updated Play/Pause button: " + (isPlaying ? "Pause" : "Play"));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate called");
        // Get the ViewModel with SavedStateHandle
        videoViewModel = new ViewModelProvider(requireActivity()).get(VideoViewModel.class);
        try {
            PlaylistManager playlistManager = PlaylistManager.getInstance();
            videoViewModel.setVideoList(Objects.requireNonNull(playlistManager.getVideoList().getValue()));
            Log.d(TAG, "playlist on create: "+ Objects.requireNonNull(playlistManager.getVideoList().getValue()).size());
        } catch (Exception e){
            Log.d(TAG, "playlist error");
        }

    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "On View Created called");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_player, container, false);

        // Initialize StyledPlayerView and Control Buttons
        playerView = view.findViewById(R.id.player_view);
        Log.d(TAG, "onCreateView Player view: " + (playerView != null ? "Initialized" : "Not Initialized"));
        playPauseButton = view.findViewById(R.id.play_pause_button);
        stopButton = view.findViewById(R.id.stop_button);
        prevButton = view.findViewById(R.id.prev_button);
        nextButton = view.findViewById(R.id.next_button);

        videoViewModel = new ViewModelProvider(requireActivity()).get(VideoViewModel.class);
        //apiService = new ApiService(requireContext());

        // Initialize the RecyclerView for the video list
        playlistTitleTextView = view.findViewById(R.id.playlist_title);
        videoListRecyclerView = view.findViewById(R.id.video_list_recycler_view);
        videoListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        initializeVideoListRecyclerView();

        // Set up button listeners
        setupButtonListeners();

        // Check if the fragment is opened via the notification
        if (getActivity() != null && getActivity().getIntent() != null) {
            Intent intent = getActivity().getIntent();
            if (intent.getBooleanExtra("FROM_NOTIFICATION", false)) {
                Log.d(TAG, "Fragment opened from foreground notification");
                // Perform actions specific to notification launch
                fromForeground = true;
            }
        }

        // Bind to MediaPlaybackService
        Intent serviceIntent = new Intent(requireContext(), MediaPlaybackService.class);
        requireContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        // Start the service
        requireContext().startService(serviceIntent);

        // Initialize the autoplay switch
        initializeAutoplaySwitch(view);

        return view;
    }

    private void setupButtonListeners() {

        playPauseButton.setOnClickListener(v -> {
            if (mediaPlaybackService != null) {
                if (mediaPlaybackService.getExoPlayer().isPlaying()) {
                    Log.d(TAG,"is Playing, will be paused");
                    mediaPlaybackService.pauseMedia();
                    playPauseButton.setImageResource(R.drawable.icons8_play_24);
                } else {
                    if (mediaPlaybackService.getExoPlayer().getPlaybackState() == Player.STATE_READY &&
                            mediaPlaybackService.getExoPlayer().getCurrentPosition() > 0) {
                        // Resume playback if it was paused
                        Log.d(TAG, "Resuming from paused state");
                        mediaPlaybackService.resumeMedia();
                    } else {
                        // This means the media was either stopped or hasn't been played yet
                        Log.d(TAG, "media was either stopped or hasn't been played yet");
                        String savedUrl = videoViewModel.getCurrentVideoUrl().getValue();
                        if (savedUrl != null) {
                            // If we have the URL, play it directly from the beginning
                            mediaPlaybackService.playMedia(savedUrl);
                        } else {
                            // Fetch the current video URL and play it via MediaPlaybackService
                            Video currentVideo = videoViewModel.getCurrentVideo(); // Use the VideoViewModel to get the current video
                            if (currentVideo != null) {
                                mediaPlaybackService.fetchAndPlayVideo(currentVideo);
                            }
                        }
                    }
                    // Update to "Pause" when playing
                    playPauseButton.setImageResource(R.drawable.icons8_pause_24);
                }
            }
        });

        stopButton.setOnClickListener(v -> {
            if (mediaPlaybackService != null) {
                mediaPlaybackService.stopMedia();
                playPauseButton.setImageResource(R.drawable.icons8_play_24);
            }
        });

        prevButton.setOnClickListener(v -> {
            Log.d(TAG, "Clicked prev button");
            playPrev = true;
            //TODO check if video list adapter should be used here
            videoViewModel.prevVideo();
        });

        nextButton.setOnClickListener(v -> {
            Log.d(TAG, "Clicked next button");
            playNext = true;
            videoListAdapter.setSelectedPosition(videoViewModel.nextVideo());
        });
    }

    // Separate method to set up the currentVideoIndex observer
    private void setupCurrentVideoIndexObserver(VideoListAdapter videoListAdapter) {
        // Observe the current video index and update the selected item in the UI
        videoViewModel.getCurrentVideoIndex().observe(getViewLifecycleOwner(), index -> {
            Log.d(TAG,"current video index observer, position: "+index);
            int prevPosition = videoListAdapter.getPreviousPosition();
            Log.d(TAG,"Prev position: "+ prevPosition);
            if (index != null && index >= 0 &&
                    index < Objects.requireNonNull(videoViewModel.getVideoList().getValue()).size()) {
                videoListAdapter.setSelectedPosition(index);
                Log.d(TAG,"scroll to position");
                ((LinearLayoutManager) Objects.requireNonNull(videoListRecyclerView.getLayoutManager())).scrollToPositionWithOffset(index,100);
                videoListRecyclerView.setAdapter(videoListAdapter);
                if(fromForeground){
                    fromForeground = false;
                    Log.d(TAG, "Coming from foreground, I won't do anything with the player");
                } else if(!mediaPlaybackService.getExoPlayer().isPlaying() || playNext || playPrev
                   || (prevPosition != index)) {
                    playNext = false;
                    playPrev = false;
                    Log.d(TAG, "PLAY VIDEO!");
                    playVideo(index); // Only call playVideo if the index is valid
                } else {
                    Log.d(TAG, "VideoViewModel getCurrentVideoIndex observer player is playing");
                }
            }
        });
    }

    /**
     * Initializes the autoplay switch, sets its initial state, and handles state changes.
     *
     * @param view The root view of the fragment layout.
     */
    private void initializeAutoplaySwitch(View view) {
        // Initialize the SwitchMaterial
        SwitchMaterial autoplaySwitch = view.findViewById(R.id.autoplay_switch);

        // Set the initial state of the switch
        autoplaySwitch.setChecked(isAutoplayEnabled);

        // Set up the listener for state changes
        autoplaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAutoplayEnabled = isChecked;
            if (isChecked) {
                Toast.makeText(requireContext(), "Autoplay Enabled", Toast.LENGTH_SHORT).show();
                enableAutoplay();
            } else {
                Toast.makeText(requireContext(), "Autoplay Disabled", Toast.LENGTH_SHORT).show();
                disableAutoplay();
            }
        });
    }

    /**
     * Enables autoplay functionality, such as automatically playing the next video.
     */
    private void enableAutoplay() {
        // Logic to enable autoplay, e.g., automatically playing the next video
        enableAutoplayMediaPlaybackService();
    }

    /**
     * Disables autoplay functionality, stopping the automatic play of the next video.
     */
    private void disableAutoplay() {
        // Logic to disable autoplay
        disableAutoplayMediaPlaybackService();
    }

    private BroadcastReceiver errorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String error = APP_NAME+VIDEO_API_SERVICE_ERROR;
            if (error.equals(intent.getAction())) {
                String errorMessage = intent.getStringExtra("errorMessage");
                // Show the Toast in the Fragment
                Toast.makeText(context, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    };

    private void playVideo(Integer index) {
        if (index == null || videoViewModel.getVideoList().getValue() == null) {
            return;
        }

        ArrayList<Video> videoList = videoViewModel.getVideoList().getValue();
        if (index >= 0 && index < videoList.size()) {
            Video video = videoList.get(index);
            // Delegate the task to MediaPlaybackService
            if (mediaPlaybackService != null) {
                // Delegate to service to handle video playback
                mediaPlaybackService.fetchAndPlayVideo(video);
                mediaPlaybackService.setCurrentIndex(index);
            } else {
                Toast.makeText(requireContext(), "MediaPlaybackService not available", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "No more videos", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeVideoListRecyclerView() {
        // Initialize RecyclerView only if it hasn’t been initialized yet
        Log.d(TAG, "in initializeVideoListRecyclerView");

        // Observe the video list from the ViewModel
        videoViewModel.getVideoList().observe(getViewLifecycleOwner(), videoList -> {
            Log.d(TAG, "Observer triggered in VideoPlayerFragment initializeVideoListRecyclerView with " + (videoList != null ? videoList.size() : 0) + " videos");
            if (videoList != null && !videoList.isEmpty()) {
                if (videoListAdapter == null) {
                    Log.d(TAG, "initializing videoListAdapter");
                    videoListAdapter = new VideoListAdapter(videoList, position -> {
                        videoViewModel.setCurrentVideoIndex(position); // Update ViewModel with the selected video index
                    });
                    videoListRecyclerView.setAdapter(videoListAdapter);
                }
                if (videoListAdapter != null) {
                    Log.d(TAG, "Updating existing videoListAdapter with new video list");
                    setupCurrentVideoIndexObserver(videoListAdapter);

                    videoListAdapter.updateVideoList(videoList);
                    //I don't know why notifyDataSetChanged doesn't work, need to check. will use set adapter for now
                    videoListRecyclerView.setAdapter(videoListAdapter);

                    Log.d(TAG, "Adapter item count after update: " + videoListAdapter.getItemCount());
                }
                Log.d(TAG, "RecyclerView updated with new video list");
                Bundle arguments = getArguments();
                if (arguments != null) {
                    String playlistTitle = arguments.getString("PLAYLIST_TITLE", "Title Unknown"); // Retrieve the title
                    Log.d(TAG, "Set playlist text");
                    playlistTitleTextView.setText(playlistTitle);
                } else {
                    Log.d(TAG, "Bundle Arguments is null");
                }
                // Set the selected position in the adapter based on the current video index
                if(!mediaPlaybackService.getExoPlayer().isPlaying()) {
                    Log.d(TAG, "initializeVideoListRecyclerView is not playing");
                    Integer currentIndex = videoViewModel.getCurrentVideoIndex().getValue();
                    Log.d(TAG, "Current video position " + currentIndex);
                    if (currentIndex != null && currentIndex >= 0 && currentIndex < videoList.size()) {
                        videoListAdapter.setSelectedPosition(currentIndex);
                    } else {
                        videoListAdapter.setSelectedPosition(0); // Default to first item if index is invalid
                    }
                }
            } else {
                Log.d(TAG, "No videos to update in the RecyclerView");
                Toast.makeText(requireContext(), "No videos available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private BroadcastReceiver trackChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String trackChange = APP_NAME+TRACK_CHANGE;
            if (trackChange.equals(intent.getAction())) {
                int nextIndex = intent.getIntExtra("nextTrack",0);
                // Update ViewModel with the new track information
                videoViewModel.setCurrentVideoIndex(nextIndex);
            }
        }
    };

    private void enableAutoplayMediaPlaybackService() {
        // Set autoplay state in MediaPlaybackService or shared state
        Log.d(TAG, "Autoplay enabled");
        mediaPlaybackService.setAutoplayEnabled(true); // Add this method in your service
    }

    private void disableAutoplayMediaPlaybackService() {
        mediaPlaybackService.setAutoplayEnabled(false); // Add this method in your service
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        if (getActivity() != null && getActivity().getIntent() != null) {
            Intent intent = getActivity().getIntent();
            if (intent.getBooleanExtra("FROM_NOTIFICATION", false)) {
                Log.d(TAG, "Fragment resumed from foreground notification");
                fromForeground = true;
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onStart() {
        super.onStart();
        Intent serviceIntent = new Intent(requireContext(), MediaPlaybackService.class);
        requireContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        requireContext().startService(serviceIntent);

        IntentFilter trackChangeFilter = new IntentFilter(APP_NAME+TRACK_CHANGE);
        requireContext().registerReceiver(trackChangeReceiver, trackChangeFilter, Context.RECEIVER_NOT_EXPORTED);

        IntentFilter videoErrorFilter = new IntentFilter(APP_NAME+VIDEO_API_SERVICE_ERROR);
        requireContext().registerReceiver(errorReceiver, videoErrorFilter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (isBound) {
            requireContext().unbindService(serviceConnection);
            isBound = false;
        }

        requireContext().unregisterReceiver(trackChangeReceiver);
        requireContext().unregisterReceiver(errorReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
