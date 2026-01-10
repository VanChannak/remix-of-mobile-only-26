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
    appendUserAgent: 'KHMERZOON-Native',
  },
  ios: {
    backgroundColor: '#00000000',
    contentInset: 'automatic',
    preferredContentMode: 'mobile',
  },
  plugins: {
    StatusBar: {
      overlaysWebView: true,
      style: 'DARK',
      backgroundColor: '#00000000'
    },
    Keyboard: {
      resize: 'body',
      style: 'dark',
      resizeOnFullScreen: true
    },
    App: {
      launchShowDuration: 0
    }
  }
};

export default config;

