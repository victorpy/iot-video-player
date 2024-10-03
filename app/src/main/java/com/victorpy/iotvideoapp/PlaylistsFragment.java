// PlaylistsFragment.java
package com.victorpy.iotvideoapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.victorpy.iotvideoapp.adapters.PlaylistsAdapter;
import com.victorpy.iotvideoapp.managers.VideoPlayerFragmentFactory;
import com.victorpy.iotvideoapp.models.Playlist;
import com.victorpy.iotvideoapp.models.Track;
import com.victorpy.iotvideoapp.models.Video;
import com.victorpy.iotvideoapp.models.VideoViewModel;
import com.victorpy.iotvideoapp.services.ApiService;

import java.util.ArrayList;
import java.util.List;

public class PlaylistsFragment extends Fragment {

    private static final String TAG = "PlaylistsFragment";
    private RecyclerView playlistsRecyclerView;
    private VideoViewModel videoViewModel;
    private ApiService apiService;

    private PlaylistsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlists, container, false);

        // Initialize ApiService
        apiService = new ApiService(requireContext());

        playlistsRecyclerView = view.findViewById(R.id.playlists_recycler_view);
        playlistsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        videoViewModel = new ViewModelProvider(requireActivity()).get(VideoViewModel.class);
        fetchPlaylists();

        return view;
    }

    private void fetchPlaylists() {
        // Fetch the playlists using ApiService
        apiService.getPlaylists(new ApiService.PlaylistsCallback() {
            @Override
            public void onSuccess(List<Playlist> playlists) {
                // Ensure the UI updates happen on the main thread
                requireActivity().runOnUiThread(() -> {
                    // Set up the adapter with the fetched playlists
                    adapter = new PlaylistsAdapter(playlists, playlist -> {
                        // Call getPlaylistTracks when a playlist is clicked
                        fetchPlaylistTracks(playlist);
                    });
                    playlistsRecyclerView.setAdapter(adapter);
                });
            }

            @Override
            public void onError(String error) {
                // Display an error message if the fetch fails
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Failed to fetch playlists: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    // Fetch tracks for the selected playlist
    private void fetchPlaylistTracks(Playlist playlist) {
        apiService.getPlaylistTracks(playlist.getApiUrl(), new ApiService.TracksCallback() {
            @Override
            public void onSuccess(List<Track> tracks) {
                requireActivity().runOnUiThread(() -> {
                    if (!tracks.isEmpty()) {
                        // Update the view model with the tracks from the selected playlist
                        // Reload PlaylistFragment or navigate to display tracks
                        reloadWithTracks(tracks, playlist.getTitle());

                        ArrayList<Video> videoModels = new ArrayList<>(convertTracksToVideoModels(tracks));
                        videoViewModel.setVideoList(videoModels);
                        videoViewModel.setCurrentVideoIndex(0);

                        // Highlight the home button or any specific UI element
                        updateNavigationUI();
                    } else {
                        Toast.makeText(getContext(), "No tracks found in this playlist.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Failed to fetch tracks: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void updateNavigationUI() {
        // Example: For BottomNavigationView
        BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home); // Set the home button as selected
        }
    }

    // Method to convert tracks into video models for the video player
    private List<Video> convertTracksToVideoModels(List<Track> tracks) {
        List<Video> videoModels = new ArrayList<>();
        for (Track track : tracks) {
            videoModels.add(new Video(track.getTitle(), track.getUrl())); // Adjust if Track has different fields
        }
        return videoModels;
    }

    // Reload the fragment with the fetched tracks
    private void reloadWithTracks(List<Track> tracks, String playlistTitle) {

        Log.d(TAG, "In reloadWithTracks...");
        //VideoPlayerFragment videoPlayerFragment = new VideoPlayerFragment();
        VideoPlayerFragmentFactory videoPlayerFragmentFactory = VideoPlayerFragmentFactory.getInstance();

        if(videoPlayerFragmentFactory != null) {
            VideoPlayerFragment videoPlayerFragment = videoPlayerFragmentFactory.getVideoPlayerFragment();

            // Pass the title of the selected playlist as a Bundle argument if needed
            Bundle bundle = new Bundle();
            bundle.putString("PLAYLIST_TITLE", playlistTitle);
            videoPlayerFragment.setArguments(bundle);

            // Load VideoPlayerFragment with the tracks
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, videoPlayerFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
