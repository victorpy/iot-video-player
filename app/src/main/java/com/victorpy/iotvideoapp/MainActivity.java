package com.victorpy.iotvideoapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.victorpy.iotvideoapp.managers.VideoPlayerFragmentFactory;
import com.victorpy.iotvideoapp.services.MediaPlaybackService;

public class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;
    private final String TAG = "MyMainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "On MainActivity Create");
        super.onCreate(savedInstanceState);

        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        // Get the singleton instance of CustomFragmentFactory
        VideoPlayerFragmentFactory fragmentFactory = VideoPlayerFragmentFactory.getInstance();
        // Set the FragmentFactory before setting content view
        getSupportFragmentManager().setFragmentFactory(fragmentFactory);

        // Initialize the bottom navigation view and set up a listener for item selection
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this::onNavigationItemSelected);

        // Check if the fragment is already added
        if (savedInstanceState == null && getIntent() != null && !getIntent().hasExtra("showVideoPlayer")) {
            // Load the Video Player Fragment
            Log.d(TAG, "savedInstanceState is null");
            Fragment videoPlayerFragment = fragmentFactory.instantiate(getClassLoader(), VideoPlayerFragment.class.getName());
            loadFragment(videoPlayerFragment);
        }

        // Check the intent for extras
        if (getIntent() != null && getIntent().hasExtra("showVideoPlayer")) {
            Log.d(TAG, "Reloading VideoPlayerFragment from notification");
            Fragment videoPlayerFragment = fragmentFactory.getVideoPlayerFragment();
            loadFragment(videoPlayerFragment);
        }

    }
    // Function to check and request notification permission
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                // Permission is already granted, start the service
                startMediaPlaybackService();
            }
        } else {
            // If OS version is below Android 13, no need to request the permission, just start the service
            startMediaPlaybackService();
        }
    }

    // Function to start the MediaPlaybackService
    private void startMediaPlaybackService() {
        if (!MediaPlaybackService.isServiceRunning()) {
            Intent serviceIntent = new Intent(this, MediaPlaybackService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            Log.d(TAG, "Service is already running");
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start the foreground service
                Log.d(TAG, "on request permission start media playback");
                startMediaPlaybackService();
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Notification permission is required to run the foreground service.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the intent associated with the activity
        Log.d(TAG,"onNewIntent");
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();
        if (itemId == R.id.nav_home) {
            VideoPlayerFragmentFactory videoPlayerFragmentFactory = VideoPlayerFragmentFactory.getInstance();
            if(videoPlayerFragmentFactory != null) {
                Log.d(TAG, "Trying to load VideoPlayerFragment");
                selectedFragment = videoPlayerFragmentFactory.getVideoPlayerFragment();
            }
        } else if (itemId == R.id.nav_playlist) {
            selectedFragment = new PlaylistsFragment(); // Fragment to display playlists
        } else {
            return false;
        }
        loadFragment(selectedFragment);
        return true;
    }

    public void loadFragment(Fragment fragment) {
        if(fragment != null) {
            Log.d(TAG, "Loading fragment");
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
