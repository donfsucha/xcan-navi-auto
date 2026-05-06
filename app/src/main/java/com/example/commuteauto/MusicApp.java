package com.example.commuteauto;

enum MusicApp {
    SAMSUNG_MUSIC("삼성뮤직", "com.sec.android.app.music"),
    MELON("멜론", "com.iloen.melon"),
    YOUTUBE_MUSIC("유튜브 뮤직", "com.google.android.apps.youtube.music"),
    SPOTIFY("스포티파이", "com.spotify.music");

    final String label;
    final String packageName;

    MusicApp(String label, String packageName) {
        this.label = label;
        this.packageName = packageName;
    }

    @Override
    public String toString() {
        return label;
    }
}
