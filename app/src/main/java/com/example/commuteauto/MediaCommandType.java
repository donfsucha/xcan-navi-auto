package com.example.commuteauto;

import android.view.KeyEvent;

enum MediaCommandType {
    PLAY("재생", KeyEvent.KEYCODE_MEDIA_PLAY),
    PLAY_PAUSE("재생/일시정지", KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);

    final String label;
    final int keyCode;

    MediaCommandType(String label, int keyCode) {
        this.label = label;
        this.keyCode = keyCode;
    }

    @Override
    public String toString() {
        return label;
    }
}
