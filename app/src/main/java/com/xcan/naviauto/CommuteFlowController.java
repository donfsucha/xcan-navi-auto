package com.xcan.naviauto;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

final class CommuteFlowController {
    interface Callback {
        void onStatus(String message);
    }

    private final DriveModeResolver driveModeResolver = new DriveModeResolver();
    private final MusicLauncher musicLauncher = new MusicLauncher();
    private final MediaCommandDispatcher mediaCommandDispatcher = new MediaCommandDispatcher();
    private final NavigationLauncher navigationLauncher = new NavigationLauncher();
    private final Handler handler = new Handler(Looper.getMainLooper());

    void start(Context context, UserSettings settings, DriveMode overrideMode, Callback callback) {
        DriveMode mode = overrideMode == null ? driveModeResolver.resolve(settings) : overrideMode;
        Place destination = driveModeResolver.destinationFor(mode, settings);

        if (mode == DriveMode.MANUAL) {
            notify(callback, "출근/퇴근 시간대가 아니므로 목적지 없이 음악과 내비 앱만 실행합니다.");
        }

        if ((mode == DriveMode.GO_TO_WORK || mode == DriveMode.GO_HOME)
                && (destination == null || !destination.hasAddress())) {
            notify(callback, mode.destinationLabel + " 주소가 비어 있습니다. 목적지에서 주소를 먼저 저장해 주세요.");
            return;
        }
        if ((mode == DriveMode.GO_TO_WORK || mode == DriveMode.GO_HOME)
                && !destination.hasCoordinates()) {
            notify(callback, mode.destinationLabel + " 좌표가 비어 있습니다. 목적지에서 지도 위치를 확인해 주세요.");
            return;
        }

        notify(callback, "1단계: " + settings.musicApp.label + " 실행");
        boolean musicStarted = musicLauncher.launch(context, settings.musicApp);
        if (!musicStarted) {
            notify(callback, settings.musicApp.label + "이 설치되어 있지 않아 설치 화면을 열었습니다.");
            return;
        }

        if (destination == null) {
            notify(callback, "2단계: " + settings.navigationApp.label + " 앱만 실행");
        } else {
            notify(callback, "2단계: " + settings.navigationApp.label + " 길안내 실행");
        }
        boolean navigationStarted = navigationLauncher.launch(context, settings.navigationApp, destination);
        if (!navigationStarted) {
            notify(callback, settings.navigationApp.label + " 실행에 실패했습니다. 앱 설치 상태를 확인해 주세요.");
            return;
        }

        long mediaDelay = Math.max(300L, settings.musicLaunchDelayMs);
        handler.postDelayed(() -> {
            if (settings.autoPlayMusicEnabled) {
                mediaCommandDispatcher.dispatch(context, settings.mediaCommandType);
                notify(callback, "음악 재생 버튼 전송: " + settings.mediaCommandType.label);
            } else {
                notify(callback, "음악 자동 재생 시도는 꺼져 있습니다.");
            }
        }, mediaDelay);
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
