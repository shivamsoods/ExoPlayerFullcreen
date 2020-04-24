package com.shivam.videoplayer;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestManager;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.Objects;

public class VideoPlayerRecyclerView extends RecyclerView {

    private static final String TAG = "VideoPlayerRecyclerView";


    private enum VolumeState {ON, OFF}

    private enum PlaybackState {PLAY, PAUSE}

    private enum ScreenState {FULL, SMALL}

    // ui
    private ImageView thumbnail, volumeControl, playbackControl, screenControl;
    private ProgressBar progressBar;
    private View viewHolderParent;
    private FrameLayout frameLayout;
    private PlayerView videoSurfaceView;
    private SimpleExoPlayer videoPlayer;
    private Dialog fullScreenDialog;
    private LinearLayout llPlayer, llParent, llControl;

    // vars
    private ArrayList<MediaObject> mediaObjects = new ArrayList<>();
    private int videoSurfaceDefaultHeight = 0;
    private int screenDefaultHeight = 0;
    private Context context;
    private int playPosition = -1;
    private boolean isVideoViewAdded;
    private RequestManager requestManager;


    // controlling playback state
    private VolumeState volumeState;
    private PlaybackState playbackState;
    private ScreenState screenState;

    public VideoPlayerRecyclerView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public VideoPlayerRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    private void init(@NonNull Context context) {
        this.context = context.getApplicationContext();
        Display display = ((WindowManager) Objects.requireNonNull(getContext().getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        videoSurfaceDefaultHeight = point.x;
        screenDefaultHeight = point.y;

        // videoSurfaceView = new PlayerView(this.context);
        //videoSurfaceView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        // 2. Create the player
        videoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
        // Bind the player to the view.
        //videoSurfaceView.setUseController(false);
        // videoSurfaceView.setPlayer(videoPlayer);


        setVolumeControl(VolumeState.ON);
        screenState = ScreenState.SMALL;


        addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    setPlaybackControl(PlaybackState.PLAY);

                    Log.d(TAG, "onScrollStateChanged: called.");
                    if (thumbnail != null) { // show the old thumbnail
                        thumbnail.setVisibility(VISIBLE);
                    }

                    // There's a special case when the end of the list has been reached.
                    // Need to handle that with this bit of logic
                    if (!recyclerView.canScrollVertically(1)) {
                        playVideo(true);
                    } else {
                        playVideo(false);
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }


        });

        addOnChildAttachStateChangeListener(new OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {

            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                if (viewHolderParent != null && viewHolderParent.equals(view)) {
                    resetVideoView();
                }

            }
        });

        videoPlayer.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, int reason) {

            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

            }

            @Override
            public void onLoadingChanged(boolean isLoading) {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {

                    case Player.STATE_BUFFERING:
                        setPlaybackControl(PlaybackState.PAUSE);
                        Log.e(TAG, "onPlayerStateChanged: Buffering video.");
                        if (progressBar != null) {
                            progressBar.setVisibility(VISIBLE);
                        }
                        if (llControl != null) {
                            llControl.setVisibility(View.GONE);
                        }

                        break;
                    case Player.STATE_ENDED:
                        Log.d(TAG, "onPlayerStateChanged: Video ended.");
                        videoPlayer.seekTo(0);
                        break;
                    case Player.STATE_IDLE:

                        break;

                    case Player.STATE_READY:
                        Log.e(TAG, "onPlayerStateChanged: Ready to play.");
                        if (progressBar != null) {
                            progressBar.setVisibility(GONE);
                        }
                        if (llControl != null) {
                            llControl.setVisibility(View.VISIBLE);
                        }
                        if (!isVideoViewAdded) {
                            addVideoView();
                            setPlaybackControl(PlaybackState.PLAY);
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {

            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {

            }

            @Override
            public void onPositionDiscontinuity(int reason) {

            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

            }

            @Override
            public void onSeekProcessed() {

            }
        });

        initFulScreenDialog();

    }

    public void playVideo(boolean isEndOfList) {

        int targetPosition;

        if (!isEndOfList) {
            int startPosition = ((LinearLayoutManager) Objects.requireNonNull(getLayoutManager())).findFirstVisibleItemPosition();
            int endPosition = ((LinearLayoutManager) getLayoutManager()).findLastVisibleItemPosition();

            // if there is more than 2 list-items on the screen, set the difference to be 1
            if (endPosition - startPosition > 1) {
                endPosition = startPosition + 1;
            }

            // something is wrong. return.
            if (startPosition < 0 || endPosition < 0) {
                return;
            }

            // if there is more than 1 list-item on the screen
            if (startPosition != endPosition) {
                int startPositionVideoHeight = getVisibleVideoSurfaceHeight(startPosition);
                int endPositionVideoHeight = getVisibleVideoSurfaceHeight(endPosition);

                targetPosition = startPositionVideoHeight > endPositionVideoHeight ? startPosition : endPosition;
            } else {
                targetPosition = startPosition;
            }
        } else {
            targetPosition = mediaObjects.size() - 1;
        }

        Log.d(TAG, "playVideo: target position: " + targetPosition);

        // video is already playing so return
        if (targetPosition == playPosition) {
            return;
        }

        // set the position of the list-item that is to be played
        playPosition = targetPosition;
//        if (videoSurfaceView == null) {
//            return;
//        }

        // remove any old surface views from previously playing videos
        if(videoSurfaceView != null){
            videoSurfaceView.setVisibility(INVISIBLE);
            removeVideoView(videoSurfaceView);
        }

        int currentPosition = targetPosition - ((LinearLayoutManager) Objects.requireNonNull(getLayoutManager())).findFirstVisibleItemPosition();

        View child = getChildAt(currentPosition);
        if (child == null) {
            return;
        }

        VideoPlayerViewHolder holder = (VideoPlayerViewHolder) child.getTag();
        if (holder == null) {
            playPosition = -1;
            return;
        }
        thumbnail = holder.thumbnail;
        progressBar = holder.progressBar;
        volumeControl = holder.volumeControl;
        viewHolderParent = holder.itemView;
        requestManager = holder.requestManager;
        frameLayout = holder.mediaContainer;
        playbackControl = holder.playbackControl;
        screenControl = holder.screenControl;
        llPlayer = holder.llPlayer;
        llParent = holder.llParent;
        llControl = holder.llControl;
        videoSurfaceView = holder.playerView;


        videoSurfaceView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
        videoSurfaceView.setPlayer(videoPlayer);

        volumeControl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVolume();
            }
        });
        playbackControl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayback();
            }
        });

        screenControl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleScreen();
            }
        });


        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(
                context, Util.getUserAgent(context, "RecyclerView VideoPlayer"));
        String mediaUrl = mediaObjects.get(targetPosition).getMedia_url();
        if (mediaUrl != null) {
            MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(mediaUrl));
            videoPlayer.prepare(videoSource);
            videoPlayer.setPlayWhenReady(true);
        }
    }

    /**
     * Returns the visible region of the video surface on the screen.
     * if some is cut off, it will return less than the @videoSurfaceDefaultHeight
     *
     * @param playPosition
     * @return
     */
    private int getVisibleVideoSurfaceHeight(int playPosition) {
        int at = playPosition - ((LinearLayoutManager) Objects.requireNonNull(getLayoutManager())).findFirstVisibleItemPosition();
        Log.d(TAG, "getVisibleVideoSurfaceHeight: at: " + at);

        View child = getChildAt(at);
        if (child == null) {
            return 0;
        }

        int[] location = new int[2];
        child.getLocationInWindow(location);

        if (location[1] < 0) {
            return location[1] + videoSurfaceDefaultHeight;
        } else {
            return screenDefaultHeight - location[1];
        }
    }

    // Remove the old player
    private void removeVideoView(PlayerView videoView) {
        ViewGroup parent = (ViewGroup) videoView.getParent();
        if (parent == null) {
            return;
        }

        int index = parent.indexOfChild(videoView);
        if (index >= 0) {
            parent.removeViewAt(index);
            isVideoViewAdded = false;
            viewHolderParent.setOnClickListener(null);
        }

    }


    private void addVideoView() {
        frameLayout.removeView(videoSurfaceView);
        frameLayout.addView(videoSurfaceView);
        isVideoViewAdded = true;
        videoSurfaceView.requestFocus();
        videoSurfaceView.setVisibility(VISIBLE);
        videoSurfaceView.setAlpha(1);
        thumbnail.setVisibility(GONE);
    }

    private void resetVideoView() {
        if (isVideoViewAdded) {
            removeVideoView(videoSurfaceView);
            playPosition = -1;
            videoSurfaceView.setVisibility(INVISIBLE);
            thumbnail.setVisibility(VISIBLE);
        }
    }

    public void releasePlayer() {

        if (videoPlayer != null) {
            videoPlayer.release();
            videoPlayer = null;
        }

        viewHolderParent = null;
    }

    public void pausePlayer() {
        setPlaybackControl(PlaybackState.PAUSE);
    }

    public void resumePlayer() {
        setPlaybackControl(PlaybackState.PLAY);
    }


    private void togglePlayback() {
        if (videoPlayer != null && isVideoViewAdded) {
            if (playbackState == PlaybackState.PLAY) {
                Log.d(TAG, "togglePlaybackState: enabling Pause.");
                Toast.makeText(context, "Pausing", Toast.LENGTH_SHORT).show();
                setPlaybackControl(PlaybackState.PAUSE);

            } else if (playbackState == PlaybackState.PAUSE) {
                Log.d(TAG, "togglePlaybackState: enabling Play.");
                Toast.makeText(context, "Playing", Toast.LENGTH_SHORT).show();

                setPlaybackControl(PlaybackState.PLAY);

            }
        }
    }

    private void setPlaybackControl(PlaybackState state) {
        playbackState = state;

        if (state == PlaybackState.PLAY) {
            videoPlayer.setPlayWhenReady(true);
            animatePlaybackControl();

        } else if (state == PlaybackState.PAUSE) {
            videoPlayer.setPlayWhenReady(false);
            animatePlaybackControl();

        }
    }

    private void animatePlaybackControl() {
        if (playbackControl != null) {

            // playbackControl.bringToFront();
            if (playbackState == PlaybackState.PLAY) {
                requestManager.load(R.drawable.ic_pause_black_24dp)
                        .into(playbackControl);

            } else if (playbackState == PlaybackState.PAUSE) {
                requestManager.load(R.drawable.ic_play_arrow_black_24dp)
                        .into(playbackControl);

            }

//            playbackControl.animate().cancel();
//            playbackControl.setAlpha(1f);
//            playbackControl.animate()
//                    .alpha(0f)
//                    .setDuration(600).setStartDelay(1000);

        }
    }


    private void toggleVolume() {
        if (videoPlayer != null && isVideoViewAdded) {
            if (volumeState == VolumeState.OFF) {
                Log.d(TAG, "togglePlaybackState: enabling volume.");
                setVolumeControl(VolumeState.ON);
                Toast.makeText(context, "Mute OFF", Toast.LENGTH_SHORT).show();


            } else if (volumeState == VolumeState.ON) {
                Log.d(TAG, "togglePlaybackState: disabling volume.");
                setVolumeControl(VolumeState.OFF);
                Toast.makeText(context, "Mute ON", Toast.LENGTH_SHORT).show();


            }
        }
    }

    private void setVolumeControl(VolumeState state) {
        volumeState = state;
        if (state == VolumeState.OFF) {
            videoPlayer.setVolume(0f);
            animateVolumeControl();
        } else if (state == VolumeState.ON) {
            videoPlayer.setVolume(1f);
            animateVolumeControl();
        }
    }

    private void animateVolumeControl() {
        if (volumeControl != null) {
//            volumeControl.bringToFront();
//            volumeControl.setAlpha(1f);
            if (volumeState == VolumeState.OFF) {
                requestManager.load(R.drawable.ic_volume_off_grey_24dp)
                        .into(volumeControl);
            } else if (volumeState == VolumeState.ON) {
                requestManager.load(R.drawable.ic_volume_up_grey_24dp)
                        .into(volumeControl);
            }
//            volumeControl.animate().cancel();
//            volumeControl.setAlpha(1f);
//            volumeControl.animate()
//                    .alpha(0f)
//                    .setDuration(600).setStartDelay(1000);
        }
    }


    private void toggleScreen() {
        if (videoPlayer != null && isVideoViewAdded) {
            if (screenState == ScreenState.FULL) {
                Toast.makeText(context, "Small Screen", Toast.LENGTH_SHORT).show();
                setScreenControl(ScreenState.SMALL);
                animateScreenControl();
            } else if (screenState == ScreenState.SMALL) {
                Toast.makeText(context, "Big Screen", Toast.LENGTH_SHORT).show();
                setScreenControl(ScreenState.FULL);
                animateScreenControl();

            }
        }
    }

    private void setScreenControl(ScreenState state) {

        if (state == ScreenState.SMALL) {
            Log.d(TAG, "setScreenControl: Going Small screen");
            closeFullScreenDialog();
            screenState = ScreenState.SMALL;

        } else if (state == ScreenState.FULL) {
            Log.d(TAG, "setScreenControl: Going full screen");
            openFullScreenDialog();
            screenState = ScreenState.FULL;

        }

    }

    private void animateScreenControl() {
        if (screenControl != null) {
//            screenControl.bringToFront();
//            screenControl.setAlpha(1f);
            if (screenState == ScreenState.FULL) {
                requestManager.load(R.drawable.ic_fullscreen_exit_black_24dp)
                        .into(screenControl);
            } else if (screenState == ScreenState.SMALL) {
                requestManager.load(R.drawable.ic_fullscreen_black_24dp)
                        .into(screenControl);
            }
//            screenControl.animate().cancel();
//            screenControl.setAlpha(1f);
//            screenControl.animate()
//                    .alpha(0f)
//                    .setDuration(600).setStartDelay(1000);
        }
    }


    private void initFulScreenDialog() {
        fullScreenDialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
            @Override
            public void onBackPressed() {
                closeFullScreenDialog();
                screenState = ScreenState.SMALL;
                super.onBackPressed();
            }
        };
        fullScreenDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.GRAY));
    }

    private void openFullScreenDialog() {
        // removeVideoView(videoSurfaceView);
        //((ViewGroup) videoSurfaceView.getParent()).removeView(videoSurfaceView);
        ((ViewGroup) llPlayer.getParent()).removeView(llPlayer);


        videoSurfaceView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        fullScreenDialog.addContentView(llPlayer, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        fullScreenDialog.show();
    }

    private void closeFullScreenDialog() {
        //((ViewGroup) videoSurfaceView.getParent()).removeView(videoSurfaceView);
        ((ViewGroup) llPlayer.getParent()).removeView(llPlayer);

        fullScreenDialog.dismiss();
        videoSurfaceView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

        llParent.addView(llPlayer);
        llParent.requestFocus();

        //frameLayout.addView(videoSurfaceView);
        //frameLayout.addView(llControl);
        //videoSurfaceView.requestFocus();

    }


    public void setMediaObjects(ArrayList<MediaObject> mediaObjects) {
        this.mediaObjects = mediaObjects;
    }


}