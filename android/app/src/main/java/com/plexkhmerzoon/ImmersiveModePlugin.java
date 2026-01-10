package com.plexkhmerzoon;

import android.util.Log;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "ImmersiveMode")
public class ImmersiveModePlugin extends Plugin {
    private static final String TAG = "ImmersiveModePlugin";

    @PluginMethod
    public void enterImmersive(PluginCall call) {
        Log.d(TAG, "enterImmersive called");
        try {
            getActivity().runOnUiThread(() -> {
                try {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        activity.enterImmersiveMode();
                        Log.d(TAG, "enterImmersiveMode executed successfully");
                        JSObject ret = new JSObject();
                        ret.put("success", true);
                        call.resolve(ret);
                    } else {
                        Log.e(TAG, "Activity is null");
                        call.reject("Activity not available");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in enterImmersive: " + e.getMessage());
                    call.reject("Failed to enter immersive mode: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in enterImmersive: " + e.getMessage());
            call.reject("Failed to enter immersive mode: " + e.getMessage());
        }
    }

    @PluginMethod
    public void exitImmersive(PluginCall call) {
        Log.d(TAG, "exitImmersive called");
        try {
            getActivity().runOnUiThread(() -> {
                try {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        activity.exitImmersiveMode();
                        Log.d(TAG, "exitImmersiveMode executed successfully");
                        JSObject ret = new JSObject();
                        ret.put("success", true);
                        call.resolve(ret);
                    } else {
                        Log.e(TAG, "Activity is null");
                        call.reject("Activity not available");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in exitImmersive: " + e.getMessage());
                    call.reject("Failed to exit immersive mode: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in exitImmersive: " + e.getMessage());
            call.reject("Failed to exit immersive mode: " + e.getMessage());
        }
    }
}
