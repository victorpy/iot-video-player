// PlaylistAdapter.java
package com.victorpy.iotvideoapp.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.victorpy.iotvideoapp.R;
import com.victorpy.iotvideoapp.models.Playlist;

import java.util.List;

public class PlaylistsAdapter extends RecyclerView.Adapter<PlaylistsAdapter.PlaylistViewHolder> {

    private final String TAG = "PlaylistAdapter";
    private final List<Playlist> playlists;
    private final OnItemClickListener listener;

    // Interface for handling click events
    public interface OnItemClickListener {
        void onItemClick(Playlist playlist);
    }

    public PlaylistsAdapter(List<Playlist> playlists, OnItemClickListener listener) {
        Log.d(TAG, "In PlaylistsAdapter Constructor");
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        Log.d(TAG, "Binding playlist: ");
        holder.bind(playlist, listener);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private final TextView playlistName;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            playlistName = itemView.findViewById(R.id.playlist_name);
        }

        public void bind(Playlist playlist, OnItemClickListener listener) {
            playlistName.setText(playlist.getName());
            itemView.setOnClickListener(v -> listener.onItemClick(playlist));
        }
    }
}
