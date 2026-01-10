package com.plexkhmerzoon;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Capacitor Plugin for ExoPlayer integration
 * Launches fullscreen video player activity with ExoPlayer
 */
@CapacitorPlugin(name = "ExoPlayer")
public class ExoPlayerPlugin extends Plugin {

    public static final int PLAYER_REQUEST_CODE = 1001;
    private PluginCall savedCall;

    @PluginMethod
    public void play(PluginCall call) {
        String url = call.getString("url");
        String title = call.getString("title", "");
        String subtitle = call.getString("subtitle", "");
        long startPosition = call.getLong("startPosition", 0L);
        
        if (url == null || url.isEmpty()) {
            call.reject("Video URL is required");
            return;
        }
        
        savedCall = call;
        
        Activity activity = getActivity();
        if (activity != null) {
            Intent intent = new Intent(activity, FullscreenPlayerActivity.class);
            intent.putExtra("VIDEO_URL", url);
            intent.putExtra("VIDEO_TITLE", title);
            intent.putExtra("VIDEO_SUBTITLE", subtitle);
            intent.putExtra("START_POSITION", startPosition);
            
            activity.startActivityForResult(intent, PLAYER_REQUEST_CODE);
        } else {
            call.reject("Activity not available");
        }
    }

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        super.handleOnActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PLAYER_REQUEST_CODE && savedCall != null) {
            JSObject result = new JSObject();
            
            if (data != null) {
                result.put("position", data.getLongExtra("POSITION", 0));
                result.put("duration", data.getLongExtra("DURATION", 0));
                result.put("completed", data.getBooleanExtra("COMPLETED", false));
            }
            
            savedCall.resolve(result);
            savedCall = null;
        }
    }
}
