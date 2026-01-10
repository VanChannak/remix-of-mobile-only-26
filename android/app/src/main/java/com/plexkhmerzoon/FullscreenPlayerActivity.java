package com.plexkhmerzoon;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.OptIn;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Fullscreen video player activity using ExoPlayer
 * Netflix-style UI with all controls
 * Auto-rotates to landscape for optimal video viewing
 */
@OptIn(markerClass = UnstableApi.class)
public class FullscreenPlayerActivity extends Activity implements Player.Listener {

    private ExoPlayer player;
    private PlayerView playerView;
    private DefaultTrackSelector trackSelector;
    private ProgressBar loadingProgress;
    private View controlsOverlay;
    
    // Top bar
    private ImageButton btnBack;
    private TextView tvTitle;
    private TextView tvSubtitle;
    private ImageButton btnSettings;
    private ImageButton btnEpisodes;
    
    // Center controls
    private ImageButton btnPlay;
    private ImageButton btnRewind;
    private ImageButton btnForward;
    
    // Bottom bar
    private SeekBar seekBar;
    private TextView tvPosition;
    private TextView tvDuration;
    private ImageButton btnLock;
    private ImageButton btnSpeed;
    private ImageButton btnQuality;
    private ImageButton btnAudio;
    private ImageButton btnSubtitle;
    private ImageButton btnFullscreen;
    
    // Panels
    private View settingsPanel;
    private View qualityPanel;
    private View speedPanel;
    private LinearLayout qualityList;
    private LinearLayout speedList;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateProgressRunnable;
    private Runnable hideControlsRunnable;
    private boolean controlsVisible = true;
    private boolean isLocked = false;
    private GestureDetector gestureDetector;
    private long startPosition = 0;
    private boolean playbackCompleted = false;
    
    // Available qualities
    private List<String> availableQualities = new ArrayList<>();
    private String currentQuality = "Auto";
    
    // Playback speeds
    private float[] speeds = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
    private float currentSpeed = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Force landscape orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        
        // Fullscreen immersive mode
        setupFullscreen();
        
        // Prevent screenshots/recording
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        );
        
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Set content view
        setContentView(R.layout.activity_fullscreen_player);
        
        // Initialize views
        initViews();
        
        // Setup gesture detection for tap to show/hide controls
        setupGestureDetector();
        
        // Get video info from intent
        Intent intent = getIntent();
        String videoUrl = intent.getStringExtra("VIDEO_URL");
        String title = intent.getStringExtra("VIDEO_TITLE");
        String subtitle = intent.getStringExtra("VIDEO_SUBTITLE");
        startPosition = intent.getLongExtra("START_POSITION", 0);
        
        if (title != null && !title.isEmpty()) {
            tvTitle.setText(title);
        }
        if (subtitle != null && !subtitle.isEmpty() && tvSubtitle != null) {
            tvSubtitle.setText(subtitle);
            tvSubtitle.setVisibility(View.VISIBLE);
        }
        
        // Initialize ExoPlayer
        initPlayer(videoUrl);
    }

    private void setupFullscreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }
        
        View decorView = window.getDecorView();
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, decorView);
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
    }

    private void initViews() {
        playerView = findViewById(R.id.player_view);
        loadingProgress = findViewById(R.id.loading_progress);
        controlsOverlay = findViewById(R.id.controls_overlay);
        
        // Top bar
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        btnSettings = findViewById(R.id.btn_settings);
        btnEpisodes = findViewById(R.id.btn_episodes);
        
        // Center controls
        btnPlay = findViewById(R.id.btn_play);
        btnRewind = findViewById(R.id.btn_rewind);
        btnForward = findViewById(R.id.btn_forward);
        
        // Bottom bar
        seekBar = findViewById(R.id.seek_bar);
        tvPosition = findViewById(R.id.tv_position);
        tvDuration = findViewById(R.id.tv_duration);
        btnLock = findViewById(R.id.btn_lock);
        btnSpeed = findViewById(R.id.btn_speed);
        btnQuality = findViewById(R.id.btn_quality);
        btnFullscreen = findViewById(R.id.btn_fullscreen);
        
        // Panels
        settingsPanel = findViewById(R.id.settings_panel);
        qualityPanel = findViewById(R.id.quality_panel);
        speedPanel = findViewById(R.id.speed_panel);
        qualityList = findViewById(R.id.quality_list);
        speedList = findViewById(R.id.speed_list);

        // Back button
        btnBack.setOnClickListener(v -> finishPlayback());

        // Play/Pause button
        btnPlay.setOnClickListener(v -> togglePlayPause());

        // Rewind 10 seconds
        btnRewind.setOnClickListener(v -> {
            if (player != null) {
                long newPosition = Math.max(0, player.getCurrentPosition() - 10000);
                player.seekTo(newPosition);
                showControls();
            }
        });

        // Forward 10 seconds
        btnForward.setOnClickListener(v -> {
            if (player != null) {
                long duration = player.getDuration();
                long newPosition = Math.min(duration, player.getCurrentPosition() + 10000);
                player.seekTo(newPosition);
                showControls();
            }
        });
        
        // Settings button
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                toggleSettingsPanel();
            });
        }
        
        // Episodes button (finishes activity with result to show episodes)
        if (btnEpisodes != null) {
            btnEpisodes.setOnClickListener(v -> {
                Intent result = new Intent();
                result.putExtra("ACTION", "SHOW_EPISODES");
                if (player != null) {
                    result.putExtra("POSITION", player.getCurrentPosition());
                }
                setResult(RESULT_OK, result);
                finish();
            });
        }
        
        // Lock button
        if (btnLock != null) {
            btnLock.setOnClickListener(v -> toggleLock());
        }
        
        // Speed button
        if (btnSpeed != null) {
            btnSpeed.setOnClickListener(v -> showSpeedPanel());
        }
        
        // Quality button
        if (btnQuality != null) {
            btnQuality.setOnClickListener(v -> showQualityPanel());
        }
        
        // Fullscreen toggle (already fullscreen, this could toggle fit/fill)
        if (btnFullscreen != null) {
            btnFullscreen.setOnClickListener(v -> {
                // Toggle between fit and fill
                if (playerView != null) {
                    int currentMode = playerView.getResizeMode();
                    if (currentMode == androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                        playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL);
                    } else {
                        playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    }
                }
            });
        }

        // SeekBar listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    long duration = player.getDuration();
                    long newPosition = (duration * progress) / 1000;
                    player.seekTo(newPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(hideControlsRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                scheduleHideControls();
            }
        });

        // Hide controls runnable
        hideControlsRunnable = () -> {
            if (player != null && player.isPlaying() && !isLocked) {
                hideControls();
            }
        };

        // Progress update runnable
        updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                updateProgress();
                handler.postDelayed(this, 1000);
            }
        };
        
        // Close panels when clicking outside
        if (settingsPanel != null) {
            settingsPanel.setOnClickListener(v -> hideAllPanels());
        }
        
        // Build speed list
        buildSpeedList();
    }
    
    private void buildSpeedList() {
        if (speedList == null) return;
        speedList.removeAllViews();
        
        for (float speed : speeds) {
            TextView tv = new TextView(this);
            tv.setText(speed == 1.0f ? "Normal" : speed + "x");
            tv.setTextColor(speed == currentSpeed ? Color.RED : Color.WHITE);
            tv.setTextSize(16);
            tv.setPadding(32, 24, 32, 24);
            tv.setOnClickListener(v -> {
                setPlaybackSpeed(speed);
                hideAllPanels();
            });
            speedList.addView(tv);
        }
    }
    
    private void buildQualityList() {
        if (qualityList == null) return;
        qualityList.removeAllViews();
        
        // Add Auto option
        TextView autoTv = new TextView(this);
        autoTv.setText("Auto");
        autoTv.setTextColor(currentQuality.equals("Auto") ? Color.RED : Color.WHITE);
        autoTv.setTextSize(16);
        autoTv.setPadding(32, 24, 32, 24);
        autoTv.setOnClickListener(v -> {
            setQuality("Auto", -1, -1);
            hideAllPanels();
        });
        qualityList.addView(autoTv);
        
        // Add detected qualities
        if (player != null && trackSelector != null) {
            Tracks tracks = player.getCurrentTracks();
            for (Tracks.Group trackGroup : tracks.getGroups()) {
                if (trackGroup.getType() == C.TRACK_TYPE_VIDEO) {
                    for (int i = 0; i < trackGroup.length; i++) {
                        if (trackGroup.isTrackSupported(i)) {
                            int height = trackGroup.getTrackFormat(i).height;
                            int width = trackGroup.getTrackFormat(i).width;
                            String quality = height + "p";
                            
                            TextView tv = new TextView(this);
                            tv.setText(quality);
                            tv.setTextColor(currentQuality.equals(quality) ? Color.RED : Color.WHITE);
                            tv.setTextSize(16);
                            tv.setPadding(32, 24, 32, 24);
                            final int trackIndex = i;
                            final TrackGroup group = trackGroup.getMediaTrackGroup();
                            tv.setOnClickListener(v -> {
                                setQuality(quality, height, width);
                                hideAllPanels();
                            });
                            qualityList.addView(tv);
                        }
                    }
                }
            }
        }
    }
    
    private void setPlaybackSpeed(float speed) {
        currentSpeed = speed;
        if (player != null) {
            player.setPlaybackSpeed(speed);
        }
        buildSpeedList();
    }
    
    private void setQuality(String quality, int height, int width) {
        currentQuality = quality;
        if (trackSelector != null) {
            if (quality.equals("Auto")) {
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .clearVideoSizeConstraints()
                        .build()
                );
            } else {
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .setMaxVideoSize(width, height)
                        .setMinVideoSize(width, height)
                        .build()
                );
            }
        }
        buildQualityList();
    }
    
    private void toggleSettingsPanel() {
        if (settingsPanel != null) {
            if (settingsPanel.getVisibility() == View.VISIBLE) {
                hideAllPanels();
            } else {
                settingsPanel.setVisibility(View.VISIBLE);
                handler.removeCallbacks(hideControlsRunnable);
            }
        }
    }
    
    private void showQualityPanel() {
        hideAllPanels();
        if (qualityPanel != null) {
            buildQualityList();
            qualityPanel.setVisibility(View.VISIBLE);
            handler.removeCallbacks(hideControlsRunnable);
        }
    }
    
    private void showSpeedPanel() {
        hideAllPanels();
        if (speedPanel != null) {
            buildSpeedList();
            speedPanel.setVisibility(View.VISIBLE);
            handler.removeCallbacks(hideControlsRunnable);
        }
    }
    
    private void hideAllPanels() {
        if (settingsPanel != null) settingsPanel.setVisibility(View.GONE);
        if (qualityPanel != null) qualityPanel.setVisibility(View.GONE);
        if (speedPanel != null) speedPanel.setVisibility(View.GONE);
        scheduleHideControls();
    }
    
    private void toggleLock() {
        isLocked = !isLocked;
        if (btnLock != null) {
            btnLock.setImageResource(isLocked ? R.drawable.ic_lock : R.drawable.ic_unlock);
        }
        updateControlsForLock();
    }
    
    private void updateControlsForLock() {
        int visibility = isLocked ? View.GONE : View.VISIBLE;
        
        if (btnPlay != null) btnPlay.setVisibility(visibility);
        if (btnRewind != null) btnRewind.setVisibility(visibility);
        if (btnForward != null) btnForward.setVisibility(visibility);
        if (btnSettings != null) btnSettings.setVisibility(visibility);
        if (btnEpisodes != null) btnEpisodes.setVisibility(visibility);
        if (seekBar != null) seekBar.setVisibility(visibility);
        if (btnSpeed != null) btnSpeed.setVisibility(visibility);
        if (btnQuality != null) btnQuality.setVisibility(visibility);
        if (btnFullscreen != null) btnFullscreen.setVisibility(visibility);
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleControlsVisibility();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isLocked) return true;
                
                // Double tap left side = rewind, right side = forward
                float x = e.getX();
                float screenWidth = playerView.getWidth();
                
                if (player != null) {
                    if (x < screenWidth / 2) {
                        // Rewind
                        long newPosition = Math.max(0, player.getCurrentPosition() - 10000);
                        player.seekTo(newPosition);
                    } else {
                        // Forward
                        long duration = player.getDuration();
                        long newPosition = Math.min(duration, player.getCurrentPosition() + 10000);
                        player.seekTo(newPosition);
                    }
                }
                return true;
            }
        });

        playerView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void initPlayer(String url) {
        if (url == null || url.isEmpty()) {
            finishPlayback();
            return;
        }
        
        // Create track selector for quality control
        trackSelector = new DefaultTrackSelector(this);
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setMaxVideoSizeSd() // Start with SD to load faster
                .build()
        );

        player = new ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build();
        playerView.setPlayer(player);
        playerView.setUseController(false); // We use custom controls
        player.addListener(this);

        // Create media source based on URL type
        MediaSource mediaSource = createMediaSource(url);
        player.setMediaSource(mediaSource);
        player.prepare();
        
        // Seek to start position if provided
        if (startPosition > 0) {
            player.seekTo(startPosition);
        }
        
        player.setPlayWhenReady(true);
        
        // Start progress updates
        handler.post(updateProgressRunnable);
        
        // Schedule hiding controls
        scheduleHideControls();
    }

    private MediaSource createMediaSource(String url) {
        Uri uri = Uri.parse(url);
        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setUserAgent("Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36");
        
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this, httpDataSourceFactory);

        String path = url.toLowerCase();
        
        if (path.contains(".m3u8") || path.contains("hls")) {
            // HLS
            return new HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri));
        } else if (path.contains(".mpd") || path.contains("dash")) {
            // DASH
            return new DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri));
        } else {
            // Progressive (MP4, etc.)
            return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri));
        }
    }

    private void togglePlayPause() {
        if (player != null) {
            if (player.isPlaying()) {
                player.pause();
                btnPlay.setImageResource(R.drawable.ic_play);
            } else {
                player.play();
                btnPlay.setImageResource(R.drawable.ic_pause);
                scheduleHideControls();
            }
        }
    }

    private void toggleControlsVisibility() {
        if (controlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void showControls() {
        controlsOverlay.setVisibility(View.VISIBLE);
        controlsOverlay.animate().alpha(1f).setDuration(200).start();
        controlsVisible = true;
        scheduleHideControls();
    }

    private void hideControls() {
        hideAllPanels();
        controlsOverlay.animate().alpha(0f).setDuration(200).withEndAction(() -> {
            controlsOverlay.setVisibility(View.GONE);
        }).start();
        controlsVisible = false;
    }

    private void scheduleHideControls() {
        handler.removeCallbacks(hideControlsRunnable);
        handler.postDelayed(hideControlsRunnable, 4000);
    }

    private void updateProgress() {
        if (player != null) {
            long position = player.getCurrentPosition();
            long duration = player.getDuration();
            
            tvPosition.setText(formatTime(position));
            tvDuration.setText(formatTime(duration));
            
            if (duration > 0) {
                int progress = (int) ((position * 1000) / duration);
                seekBar.setProgress(progress);
            }
            
            // Update buffered position
            int bufferedPercent = player.getBufferedPercentage();
            seekBar.setSecondaryProgress(bufferedPercent * 10);
        }
    }

    private String formatTime(long millis) {
        if (millis < 0) millis = 0;
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
        }
    }

    private void finishPlayback() {
        Intent result = new Intent();
        if (player != null) {
            result.putExtra("POSITION", player.getCurrentPosition());
            result.putExtra("DURATION", player.getDuration());
            result.putExtra("COMPLETED", playbackCompleted);
        }
        setResult(RESULT_OK, result);
        finish();
    }

    // Player.Listener callbacks
    @Override
    public void onPlaybackStateChanged(int playbackState) {
        switch (playbackState) {
            case Player.STATE_BUFFERING:
                loadingProgress.setVisibility(View.VISIBLE);
                break;
            case Player.STATE_READY:
                loadingProgress.setVisibility(View.GONE);
                btnPlay.setImageResource(player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
                // Update quality list when tracks are ready
                buildQualityList();
                break;
            case Player.STATE_ENDED:
                playbackCompleted = true;
                finishPlayback();
                break;
            case Player.STATE_IDLE:
                break;
        }
    }

    @Override
    public void onPlayerError(PlaybackException error) {
        loadingProgress.setVisibility(View.GONE);
        // Could show error message here
        finishPlayback();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateProgressRunnable);
        handler.removeCallbacks(hideControlsRunnable);
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (settingsPanel != null && settingsPanel.getVisibility() == View.VISIBLE) {
            hideAllPanels();
            return;
        }
        if (qualityPanel != null && qualityPanel.getVisibility() == View.VISIBLE) {
            hideAllPanels();
            return;
        }
        if (speedPanel != null && speedPanel.getVisibility() == View.VISIBLE) {
            hideAllPanels();
            return;
        }
        finishPlayback();
    }
}
