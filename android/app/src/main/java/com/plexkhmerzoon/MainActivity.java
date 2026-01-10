package com.plexkhmerzoon;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.graphics.Color;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private boolean isImmersiveMode = false;
    private WindowInsetsControllerCompat insetsControllerCompat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Register plugins before calling super.onCreate
        registerPlugin(ImmersiveModePlugin.class);
        registerPlugin(ExoPlayerPlugin.class);
        
        super.onCreate(savedInstanceState);
        
        // Check if running on an emulator and close immediately if true
        if (isEmulator()) {
            finish();
            return;
        }
        
        // Enable FLAG_SECURE to prevent screenshots and screen recording
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        );
        
        // Enable edge-to-edge display - content extends behind system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        // Make status bar fully transparent
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        
        // Make navigation bar fully transparent (100% transparency)
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        
        // For Android 10+ (API 29+), ensure navigation bar is truly transparent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        
        // Initialize the compat insets controller for reliable system bar control
        View decorView = getWindow().getDecorView();
        insetsControllerCompat = WindowCompat.getInsetsController(getWindow(), decorView);

        // Re-apply hiding when system bars change (helps OEM devices)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            decorView.setOnApplyWindowInsetsListener((v, insets) -> {
                if (insets != null && insets.isVisible(WindowInsets.Type.navigationBars())) {
                    v.post(this::hideNavigationBar);
                }
                return v.onApplyWindowInsets(insets);
            });
        } else {
            decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
                if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                    decorView.postDelayed(this::hideNavigationBar, 250);
                }
            });
        }

        // Set light status bar icons (dark icons on light background) based on theme
        if (insetsControllerCompat != null) {
            // Default to light icons for dark backgrounds
            insetsControllerCompat.setAppearanceLightStatusBars(false);
            // Light navigation bar icons for dark backgrounds
            insetsControllerCompat.setAppearanceLightNavigationBars(false);
        }

        // Hide navigation bar permanently on startup
        hideNavigationBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure nav bar stays hidden when returning to the app
        hideNavigationBar();
    }

    /**
     * Hide navigation bar permanently for all Android versions.
     * Note: On some devices, users can still reveal it temporarily via swipe.
     */
    private void hideNavigationBar() {
        runOnUiThread(() -> {
            try {
                Window window = getWindow();
                View decorView = window.getDecorView();

                // Prefer compat controller (works across versions and is recommended by Android docs)
                WindowInsetsControllerCompat controller =
                        insetsControllerCompat != null
                                ? insetsControllerCompat
                                : WindowCompat.getInsetsController(window, decorView);

                if (controller != null) {
                    controller.setSystemBarsBehavior(
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                    controller.hide(WindowInsetsCompat.Type.navigationBars());
                }

                // Extra legacy sticky flags for API < 30 (some OEMs require it)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    decorView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Enter immersive fullscreen mode - hides status bar and navigation bar
     * Used when video player enters fullscreen
     */
    public void enterImmersiveMode() {
        isImmersiveMode = true;
        runOnUiThread(() -> {
            try {
                Window window = getWindow();
                View decorView = window.getDecorView();
                
                // Use WindowInsetsControllerCompat for reliable cross-version support
                if (insetsControllerCompat != null) {
                    insetsControllerCompat.hide(WindowInsetsCompat.Type.systemBars());
                    insetsControllerCompat.setSystemBarsBehavior(
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+ (API 30+) - Use WindowInsetsController as fallback
                    WindowInsetsController insetsController = window.getInsetsController();
                    if (insetsController != null) {
                        insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                        insetsController.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        );
                    }
                } else {
                    // Legacy support for older Android versions (API < 30)
                    decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                    );
                }
                
                // Keep screen on during immersive mode
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                
                // Force layout update
                decorView.requestLayout();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Exit immersive mode - shows status bar and navigation bar
     * Used when video player exits fullscreen
     */
    public void exitImmersiveMode() {
        isImmersiveMode = false;
        runOnUiThread(() -> {
            try {
                Window window = getWindow();
                View decorView = window.getDecorView();
                
                // Use WindowInsetsControllerCompat for reliable cross-version support
                if (insetsControllerCompat != null) {
                    insetsControllerCompat.show(WindowInsetsCompat.Type.systemBars());
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+ (API 30+) - Use WindowInsetsController as fallback
                    WindowInsetsController insetsController = window.getInsetsController();
                    if (insetsController != null) {
                        insetsController.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    }
                } else {
                    // Legacy support for older Android versions
                    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                }
                
                // Keep navigation bar hidden in normal UI
                hideNavigationBar();

                // Allow screen to turn off
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                decorView.requestLayout();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Always re-hide navigation bar when window regains focus
            hideNavigationBar();
            // Re-apply immersive mode if it was active
            if (isImmersiveMode) {
                enterImmersiveMode();
            }
        }
    }

    /**
     * Detect if running on an emulator (BlueStacks, Genymotion, Android SDK emulator, etc.)
     * Returns true if emulator is detected
     */
    private boolean isEmulator() {
        return (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.PRODUCT.contains("sdk_google")
                || android.os.Build.PRODUCT.contains("google_sdk")
                || android.os.Build.PRODUCT.contains("sdk")
                || android.os.Build.PRODUCT.contains("sdk_x86")
                || android.os.Build.PRODUCT.contains("vbox86p")
                || android.os.Build.PRODUCT.contains("emulator")
                || android.os.Build.PRODUCT.contains("simulator");
    }
}
