package com.shivam.videoplayer;


import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestManager;

class VideoPlayerViewHolder extends RecyclerView.ViewHolder {

    private TextView title;
    ImageView thumbnail, volumeControl,playbackControl,screenControl;
    FrameLayout mediaContainer;
    ProgressBar progressBar;
    private View parent;
    RequestManager requestManager;
    LinearLayout llPlayer,llParent,llControl;

    VideoPlayerViewHolder(@NonNull View itemView) {
        super(itemView);
        parent = itemView;
        thumbnail = itemView.findViewById(R.id.thumbnail);
        title = itemView.findViewById(R.id.title);
        progressBar = itemView.findViewById(R.id.progressBar);
        volumeControl = itemView.findViewById(R.id.volume_control);
        playbackControl=itemView.findViewById(R.id.playback_control);
        mediaContainer=itemView.findViewById(R.id.media_container);
        screenControl=itemView.findViewById(R.id.fullscreen_control);
        llPlayer=itemView.findViewById(R.id.ll_player);
        llParent=itemView.findViewById(R.id.parent);
        llControl=itemView.findViewById(R.id.ll_control);
    }

    void onBind(MediaObject mediaObject, RequestManager requestManager) {
        this.requestManager = requestManager;
        parent.setTag(this);
        title.setText(mediaObject.getTitle());
        this.requestManager
                .load(mediaObject.getThumbnail())
                .into(thumbnail);
    }

}