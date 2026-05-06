package com.xcan.naviauto;

import android.content.Context;
import android.media.AudioManager;
import android.view.KeyEvent;

final class MediaCommandDispatcher {
    void dispatch(Context context, MediaCommandType commandType) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return;
        }

        audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, commandType.keyCode));
        audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, commandType.keyCode));
    }
}
