package com.victorpy.iotvideoapp.adapters;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.victorpy.iotvideoapp.R;
import com.victorpy.iotvideoapp.models.Video;

import java.util.ArrayList;
import java.util.List;

public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.VideoViewHolder> {

    private final String TAG = "VideoListAdapter";
    private ArrayList<Video> videoList;
    private final OnItemClickListener listener;
    private int selectedPosition = -1;
    private int previousPosition = 0;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public VideoListAdapter(ArrayList<Video> videoList, OnItemClickListener listener) {
        Log.d(TAG, "In Constructor...");
        this.videoList = new ArrayList<>(videoList);
        this.listener = listener;
    }

    // Method to update the list data and refresh the RecyclerView
    public void updateVideoList(ArrayList<Video> newVideoList) {
        Log.d(TAG, "Updating video list with new size: " + newVideoList.size());
        Log.d(TAG, "Before update: videoList reference = " + System.identityHashCode(this.videoList));

        int curSize = this.videoList.size();
        this.videoList.clear();  // Clear the old list
        this.videoList.addAll(newVideoList);

        Log.d(TAG, "After update: videoList reference = " + System.identityHashCode(this.videoList));

        //notifyDataSetChanged(); // Refresh the adapter data
        notifyItemRangeInserted(curSize, newVideoList.size());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder called");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Video video = videoList.get(position);
        Log.d(TAG, "Binding video at position: " + position + " with title: " + video.getTitle());
        holder.bind(video, position);
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "Adapter item count: " + videoList.size());
        return videoList.size();
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {
        private final TextView videoTitle;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoTitle = itemView.findViewById(R.id.video_title);
        }

        public void bind(Video video, int position) {
            videoTitle.setText(video.getTitle());
            // Highlight the selected item
            Log.d(TAG, "At bind selected position: " + selectedPosition);
            if (position == selectedPosition) {
                itemView.setBackgroundColor(Color.LTGRAY); // Change to the desired highlight color
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT); // Default background color
            }

            itemView.setOnClickListener(v -> {
                // Update the selected position when an item is clicked
                Log.d(TAG, "Item clicked at position: " + position);
                setSelectedPosition(position);
                listener.onItemClick(position);
            });
        }
    }
    // Optional: Method to manually set the selected position if needed externally
    public void setSelectedPosition(int position) {
        Log.d(TAG, "Set selected position: " + position);
        if(selectedPosition >= 0)
            previousPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previousPosition);
        notifyItemChanged(selectedPosition);
    }

    public int getPreviousPosition(){
        return previousPosition;
    }
}

