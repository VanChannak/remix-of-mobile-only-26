import { Capacitor, registerPlugin } from '@capacitor/core';

/**
 * Interface for the native ExoPlayer plugin
 * Used to play videos in fullscreen landscape mode on Android
 */
interface ExoPlayerPlugin {
  play(options: {
    url: string;
    title?: string;
    subtitle?: string;
    startPosition?: number;
  }): Promise<{
    position: number;
    duration: number;
    completed: boolean;
  }>;
}

// Only register plugin on Android native
const ExoPlayer = Capacitor.getPlatform() === 'android' && Capacitor.isNativePlatform()
  ? registerPlugin<ExoPlayerPlugin>('ExoPlayer')
  : null;

/**
 * Check if ExoPlayer is available (Android native only)
 */
export function isExoPlayerAvailable(): boolean {
  return Capacitor.getPlatform() === 'android' && Capacitor.isNativePlatform() && ExoPlayer !== null;
}

/**
 * Play video using native ExoPlayer in fullscreen landscape mode
 * @param url - Video URL (HLS, DASH, or MP4)
 * @param title - Video title to display
 * @param subtitle - Optional subtitle (e.g., episode info)
 * @param startPosition - Start position in milliseconds
 * @returns Promise with playback result (position, duration, completed)
 */
export async function playWithExoPlayer(
  url: string,
  title?: string,
  subtitle?: string,
  startPosition?: number
): Promise<{ position: number; duration: number; completed: boolean } | null> {
  if (!isExoPlayerAvailable() || !ExoPlayer) {
    console.log('ExoPlayer not available - not on Android native');
    return null;
  }

  try {
    console.log('Playing with ExoPlayer:', { url, title, startPosition });
    const result = await ExoPlayer.play({
      url,
      title: title || '',
      subtitle: subtitle || '',
      startPosition: startPosition || 0
    });
    console.log('ExoPlayer playback result:', result);
    return result;
  } catch (error) {
    console.error('ExoPlayer error:', error);
    throw error;
  }
}

/**
 * Hook to use ExoPlayer
 */
export function useExoPlayer() {
  return {
    isAvailable: isExoPlayerAvailable(),
    play: playWithExoPlayer
  };
}
