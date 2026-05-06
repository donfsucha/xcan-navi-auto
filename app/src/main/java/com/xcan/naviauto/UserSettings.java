package com.xcan.naviauto;

final class UserSettings {
    final Place homePlace;
    final Place workPlace;
    final boolean homeAddressConfirmed;
    final boolean workAddressConfirmed;
    final TimeRange commuteStartRange;
    final TimeRange commuteEndRange;
    final NavigationApp navigationApp;
    final MusicApp musicApp;
    final boolean autoPlayMusicEnabled;
    final MediaCommandType mediaCommandType;
    final long musicLaunchDelayMs;
    final boolean weekendModeEnabled;

    UserSettings(
            Place homePlace,
            Place workPlace,
            boolean homeAddressConfirmed,
            boolean workAddressConfirmed,
            TimeRange commuteStartRange,
            TimeRange commuteEndRange,
            NavigationApp navigationApp,
            MusicApp musicApp,
            boolean autoPlayMusicEnabled,
            MediaCommandType mediaCommandType,
            long musicLaunchDelayMs,
            boolean weekendModeEnabled
    ) {
        this.homePlace = homePlace;
        this.workPlace = workPlace;
        this.homeAddressConfirmed = homeAddressConfirmed;
        this.workAddressConfirmed = workAddressConfirmed;
        this.commuteStartRange = commuteStartRange;
        this.commuteEndRange = commuteEndRange;
        this.navigationApp = navigationApp;
        this.musicApp = musicApp;
        this.autoPlayMusicEnabled = autoPlayMusicEnabled;
        this.mediaCommandType = mediaCommandType;
        this.musicLaunchDelayMs = musicLaunchDelayMs;
        this.weekendModeEnabled = weekendModeEnabled;
    }
}
