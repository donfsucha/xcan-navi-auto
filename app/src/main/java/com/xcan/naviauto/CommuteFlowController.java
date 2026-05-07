package com.xcan.naviauto;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

final class CommuteFlowController {
    private static final long MIN_MEDIA_READY_DELAY_MS = 1500L;
    private static final long MEDIA_RETRY_INTERVAL_MS = 900L;
    private static final int MEDIA_PLAY_ATTEMPTS = 3;
    private static final long NAVIGATION_AFTER_MEDIA_DELAY_MS = 600L;
    private static final long NAVIGATION_WITHOUT_AUTOPLAY_DELAY_MS = 700L;

    interface Callback {
        void onStatus(String message);
    }

    private final DriveModeResolver driveModeResolver = new DriveModeResolver();
    private final MusicLauncher musicLauncher = new MusicLauncher();
    private final MediaCommandDispatcher mediaCommandDispatcher = new MediaCommandDispatcher();
    private final NavigationLauncher navigationLauncher = new NavigationLauncher();
    private final Handler handler = new Handler(Looper.getMainLooper());

    boolean start(Context context, UserSettings settings, DriveMode overrideMode, Callback callback) {
        DriveMode mode = overrideMode == null ? driveModeResolver.resolve(settings) : overrideMode;
        Place destination = driveModeResolver.destinationFor(mode, settings);

        if (mode == DriveMode.MANUAL) {
            notify(callback, "출근/퇴근 시간대가 아니므로 목적지 없이 음악과 내비 앱만 실행합니다.");
        }

        if ((mode == DriveMode.GO_TO_WORK || mode == DriveMode.GO_HOME)
                && (destination == null || !destination.hasAddress() || !destination.hasCoordinates())) {
            notify(callback, mode.destinationLabel + " 목적지가 없어 음악과 내비 앱만 실행합니다.");
            destination = null;
        }

        notify(callback, "1단계: " + settings.musicApp.label + " 실행");
        boolean musicStarted = musicLauncher.launch(context, settings.musicApp);
        if (!musicStarted) {
            notify(callback, settings.musicApp.label + "이 설치되어 있지 않아 설치 화면을 열었습니다.");
            return false;
        }

        Place navigationDestination = destination;
        long mediaDelay = Math.max(MIN_MEDIA_READY_DELAY_MS, settings.musicLaunchDelayMs);
        int mediaAttempts = scheduleMediaPlayback(context, settings, callback, mediaDelay);

        long navigationDelay = settings.autoPlayMusicEnabled
                ? mediaDelay + (long) (mediaAttempts - 1) * MEDIA_RETRY_INTERVAL_MS + NAVIGATION_AFTER_MEDIA_DELAY_MS
                : NAVIGATION_WITHOUT_AUTOPLAY_DELAY_MS;
        handler.postDelayed(() -> {
            if (navigationDestination == null) {
                notify(callback, "2단계: " + settings.navigationApp.label + " 앱만 실행");
            } else {
                notify(callback, "2단계: " + settings.navigationApp.label + " 길안내 실행");
            }
            boolean navigationStarted = navigationLauncher.launch(context, settings.navigationApp, navigationDestination);
            if (!navigationStarted) {
                notify(callback, settings.navigationApp.label + " 실행에 실패했습니다. 앱 설치 상태를 확인해 주세요.");
            }
        }, navigationDelay);
        return true;
    }

    private int scheduleMediaPlayback(Context context, UserSettings settings, Callback callback, long firstDelayMs) {
        if (!settings.autoPlayMusicEnabled) {
            handler.postDelayed(() -> notify(callback, "음악 자동 재생 시도는 꺼져 있습니다."), firstDelayMs);
            return 1;
        }

        int attempts = settings.mediaCommandType == MediaCommandType.PLAY ? MEDIA_PLAY_ATTEMPTS : 1;
        for (int i = 0; i < attempts; i++) {
            int attemptNumber = i + 1;
            long delay = firstDelayMs + (long) i * MEDIA_RETRY_INTERVAL_MS;
            handler.postDelayed(() -> {
                mediaCommandDispatcher.dispatch(context, settings.mediaCommandType);
                if (attempts == 1) {
                    notify(callback, "음악 재생 버튼 전송: " + settings.mediaCommandType.label);
                } else {
                    notify(callback, "음악 재생 버튼 전송: " + attemptNumber + "/" + attempts);
                }
            }, delay);
        }
        return attempts;
    }

    DriveMode currentMode(UserSettings settings) {
        return driveModeResolver.resolve(settings);
    }

    private void notify(Callback callback, String message) {
        if (callback != null) {
            callback.onStatus(message);
        }
    }
}
