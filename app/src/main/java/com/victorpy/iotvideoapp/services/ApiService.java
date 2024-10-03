package com.victorpy.iotvideoapp.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.victorpy.iotvideoapp.models.Playlist;
import com.victorpy.iotvideoapp.models.Track;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiService {

    private static final String TAG = "ApiService";
    private final String baseUrl;
    private final OkHttpClient client;
    private final SharedPreferences sharedPreferences;

    public ApiService(Context context) {
        this.client = new OkHttpClient();
        this.sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        baseUrl = "https://"+sharedPreferences.getString("hostname", "");
    }

    // Callback interface for fetching playlist tracks
    public interface TracksCallback {
        void onSuccess(List<Track> tracks);
        void onError(String error);
    }

    // Interface for callbacks
    public interface PlaylistsCallback {
        void onSuccess(List<Playlist> playlists);

        void onError(String error);
    }

    // Interface for handling the callback from the API call
    public interface VideoDetailsCallback {
        void onSuccess(JSONObject videoDetails);
        void onError(String error);
    }

    public String buildFullUrl(String path) {
        // Ensure the path starts with a slash if missing, for consistent URL structure
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return baseUrl + path;
    }
    // Fetch playlists from API
    public void getPlaylists(PlaylistsCallback callback) {
        String username = sharedPreferences.getString("username", "");
        String password = sharedPreferences.getString("password", "");

        // Use basic auth with the saved credentials
        String credentials = Credentials.basic(username, password);

        Request request = new Request.Builder()
                .url(baseUrl + "/api/v1/playlists")
                .header("Authorization", credentials)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(TAG, "Request failed: " + e.getMessage());
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                Log.d(TAG, "Response Code: " + response.code());
                Log.d(TAG, "Response Headers: " + response.headers());
                assert response.body() != null;
                String responseBody = response.body().string();
                Log.d(TAG, "Response Body: " + responseBody);

                if (response.isSuccessful()) {
                    try {
                        JSONObject responseJson = new JSONObject(responseBody);
                        Log.d(TAG, "Parsing response JSON...");
                        List<Playlist> playlists = parsePlaylists(responseJson);
                        Log.d(TAG, "Successfully parsed playlists.");
                        callback.onSuccess(playlists);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing Error: " + e.getMessage(), e);
                        Log.d(TAG, "Failed JSON: " + responseBody);
                        callback.onError("Failed to parse playlists: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Unsuccessful response: " + response.message());

                    callback.onError("Failed to fetch playlists: " + response.message());
                }
            }
        });
    }

    // Parse playlists from JSON
    // Parse playlists from JSON
    private List<Playlist> parsePlaylists(JSONObject responseJson) throws JSONException {
        List<Playlist> playlists = new ArrayList<>();
        try {
            // Extract the "results" array from the response JSON
            JSONArray resultsArray = responseJson.getJSONArray("results");
            Log.d(TAG, "Results Array Length: " + resultsArray.length());

            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject jsonObject = resultsArray.getJSONObject(i);
                Log.d(TAG, "Parsing playlist item: " + jsonObject.toString());

                // Check fields before accessing them to prevent crashes
                String id = jsonObject.optString("api_url", "");  // Using "api_url" as ID
                String title = jsonObject.optString("title", "Unknown Title");

                if (!id.isEmpty() && !title.isEmpty()) {
                    playlists.add(new Playlist(id, title, id, title));
                    Log.d(TAG, "Added playlist: ID = " + id + ", Title = " + title);
                } else {
                    Log.e(TAG, "Missing required fields for playlist item: " + jsonObject.toString());
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error while parsing results array: " + e.getMessage(), e);
            throw e;  // Re-throw to allow the calling method to handle it
        }

        Log.d(TAG, "Total playlists parsed: " + playlists.size());
        return playlists;
    }

    // Function to fetch tracks for a specific playlist using its API URL
    public void getPlaylistTracks(String apiUrl, TracksCallback callback) {
        String username = sharedPreferences.getString("username", "");
        String password = sharedPreferences.getString("password", "");

        // Use basic auth with the saved credentials
        String credentials = Credentials.basic(username, password);

        Log.d(TAG, "Playlist url: " + baseUrl + apiUrl);
        Request request = new Request.Builder()
                .url(baseUrl + apiUrl) // Append the playlist-specific API URL to the base URL
                .header("Authorization", credentials)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(TAG, "Request failed: " + e.getMessage());
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d(TAG, "Response Code: " + response.code());
                assert response.body() != null;
                String responseBody = response.body().string();
                Log.d(TAG, "Response Body: " + responseBody);

                if (response.isSuccessful()) {
                    try {
                        // Assuming the response contains an array of tracks
                        JSONObject responseJson = new JSONObject(responseBody);
                        Log.d(TAG, "Response Json: " + responseJson);
                        JSONArray jsonArray = responseJson.getJSONArray("playlist_media");
                        Log.d(TAG, "Response Array: " + jsonArray);
                        List<Track> tracks = parseTracks(jsonArray);
                        callback.onSuccess(tracks);
                    } catch (JSONException e) {
                        Log.d(TAG, Objects.requireNonNull(e.getMessage()));
                        callback.onError("Failed to parse tracks.");
                    }
                } else {
                    callback.onError("Failed to fetch tracks: " + response.message());
                }
            }
        });
    }

    // Helper function to parse tracks from JSON response
    private List<Track> parseTracks(JSONArray jsonArray) throws JSONException {
        List<Track> tracks = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String trackId = jsonObject.getString("friendly_token");
            String trackTitle = jsonObject.getString("title");
            String url = jsonObject.getString("api_url");
            // Add any additional fields as needed
            tracks.add(new Track(trackId, trackTitle, url));
            Log.d(TAG, tracks.get(i).toString());
        }
        return tracks;
    }

    // Method to get video details from a given URL
    public void getVideoDetails(String url, VideoDetailsCallback callback) {
        // Build the request using the URL provided
        Log.d(TAG, "getVideoDetails url: " + url);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        // Asynchronous call to the endpoint
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage());
                callback.onError("Failed to fetch video details: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Parse the response body as JSON
                        Log.d(TAG, "getVideoDetails on response ");
                        String responseBody = response.body().string();
                        JSONObject videoDetails = new JSONObject(responseBody);
                        callback.onSuccess(videoDetails);
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse JSON response: " + e.getMessage());
                        callback.onError("Error parsing video details.");
                    }
                } else {
                    String errorMessage = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "Request unsuccessful: " + errorMessage);
                    callback.onError("Failed to fetch video details: " + errorMessage);
                }
            }
        });
    }
}

