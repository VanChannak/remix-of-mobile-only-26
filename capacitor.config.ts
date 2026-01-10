import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.plexkhmerzoon',
  appName: 'KHMERZOON',
  webDir: 'dist',
  server: {
    url: 'https://99f1a3c8-b604-4401-a730-39bc102a36cd.lovableproject.com?forceHideBadge=true',
    cleartext: true
  },
  android: {
    backgroundColor: '#00000000',
    allowMixedContent: true,
    // Enable hardware back button handling
    appendUserAgent: 'KHMERZOON-Native',
  },
  ios: {
    backgroundColor: '#00000000',
    contentInset: 'automatic',
    // Preferred content mode for iOS
    preferredContentMode: 'mobile',
  },
  plugins: {
    StatusBar: {
      overlaysWebView: true,
      style: 'DARK',
      backgroundColor: '#00000000'
    },
    GoogleAuth: {
      scopes: ['profile', 'email'],
      serverClientId: '944708960468-eqn03vl8rm1p24ghfbkt8m9enlijlld6.apps.googleusercontent.com',
      forceCodeForRefreshToken: true
    },
    AdMob: {
      appId: 'ca-app-pub-4789683198372521~7914037351',
      requestTrackingAuthorization: true,
      testDeviceIdentifiers: []
    },
    // Keyboard configuration for better native feel
    Keyboard: {
      resize: 'body',
      style: 'dark',
      resizeOnFullScreen: true
    },
    // App configuration
    App: {
      // Let JavaScript handle back button
      launchShowDuration: 0
    }
  }
};

export default config;

