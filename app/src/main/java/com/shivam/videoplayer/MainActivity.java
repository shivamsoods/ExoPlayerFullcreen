package com.shivam.videoplayer;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private VideoPlayerRecyclerView mRecyclerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = findViewById(R.id.recycler_view);

        initRecyclerView();
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        ArrayList<MediaObject> mediaObjects = new ArrayList<>(Arrays.asList(Resources.MEDIA_OBJECTS));
        mRecyclerView.setMediaObjects(mediaObjects);

        VideoPlayerRecyclerAdapter adapter = new VideoPlayerRecyclerAdapter(mediaObjects, initGlide());
        mRecyclerView.setAdapter(adapter);
    }

    private RequestManager initGlide() {
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.white_background)
                .error(R.drawable.white_background);

        return Glide.with(this)
                .setDefaultRequestOptions(options);
    }


    @Override
    protected void onDestroy() {
        if (mRecyclerView != null)
            mRecyclerView.releasePlayer();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (mRecyclerView != null)
            mRecyclerView.pausePlayer();
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (mRecyclerView != null)
            mRecyclerView.resumePlayer();
        super.onResume();
    }

    @Override
    protected void onStop() {
        if (mRecyclerView != null)
            mRecyclerView.pausePlayer();
        super.onStop();
    }
}