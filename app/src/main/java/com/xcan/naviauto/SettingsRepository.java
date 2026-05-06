package com.xcan.naviauto;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

final class SettingsRepository {
    private static final String PREFS_NAME = "commute_auto_settings";
    private static final String HOME_ADDRESS = "home_address";
    private static final String WORK_ADDRESS = "work_address";
    private static final String HOME_LATITUDE = "home_latitude";
    private static final String HOME_LONGITUDE = "home_longitude";
    private static final String WORK_LATITUDE = "work_latitude";
    private static final String WORK_LONGITUDE = "work_longitude";
    private static final String CONFIRMED_HOME_ADDRESS = "confirmed_home_address";
    private static final String CONFIRMED_WORK_ADDRESS = "confirmed_work_address";
    private static final String CONFIRMED_HOME_LATITUDE = "confirmed_home_latitude";
    private static final String CONFIRMED_HOME_LONGITUDE = "confirmed_home_longitude";
    private static final String CONFIRMED_WORK_LATITUDE = "confirmed_work_latitude";
    private static final String CONFIRMED_WORK_LONGITUDE = "confirmed_work_longitude";
    private static final String START_FROM = "start_from";
    private static final String START_TO = "start_to";
    private static final String END_FROM = "end_from";
    private static final String END_TO = "end_to";
    private static final String NAVIGATION_APP = "navigation_app";
    private static final String MUSIC_APP = "music_app";
    private static final String AUTO_PLAY = "auto_play";
    private static final String MEDIA_COMMAND = "media_command";
    private static final String MUSIC_DELAY = "music_delay";
    private static final String WEEKEND_MODE = "weekend_mode";
    private static final String EXTRA_DESTINATION_COUNT = "extra_destination_count";
    private static final String EXTRA_DESTINATION_NAME = "extra_destination_name_";
    private static final String EXTRA_DESTINATION_ADDRESS = "extra_destination_address_";
    private static final String EXTRA_DESTINATION_LATITUDE = "extra_destination_latitude_";
    private static final String EXTRA_DESTINATION_LONGITUDE = "extra_destination_longitude_";

    private final SharedPreferences preferences;

    SettingsRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    UserSettings load() {
        String homeAddress = preferences.getString(HOME_ADDRESS, "");
        String workAddress = preferences.getString(WORK_ADDRESS, "");
        return new UserSettings(
                new Place("집", homeAddress, getHomeLatitude(), getHomeLongitude()),
                new Place("회사", workAddress, getWorkLatitude(), getWorkLongitude()),
                isHomePlaceConfirmed(homeAddress, getHomeLatitude(), getHomeLongitude()),
                isWorkPlaceConfirmed(workAddress, getWorkLatitude(), getWorkLongitude()),
                TimeRange.parse(
                        preferences.getString(START_FROM, "06:00"),
                        preferences.getString(START_TO, "10:00")
                ),
                TimeRange.parse(
                        preferences.getString(END_FROM, "17:00"),
                        preferences.getString(END_TO, "21:00")
                ),
                enumValue(NavigationApp.class, preferences.getString(NAVIGATION_APP, NavigationApp.NAVER_MAP.name())),
                enumValue(MusicApp.class, preferences.getString(MUSIC_APP, MusicApp.SAMSUNG_MUSIC.name())),
                preferences.getBoolean(AUTO_PLAY, true),
                enumValue(MediaCommandType.class, preferences.getString(MEDIA_COMMAND, MediaCommandType.PLAY.name())),
                preferences.getLong(MUSIC_DELAY, 1500L),
                preferences.getBoolean(WEEKEND_MODE, true)
        );
    }

    void confirmHomePlace(String address, double latitude, double longitude) {
        preferences.edit()
                .putString(CONFIRMED_HOME_ADDRESS, address.trim())
                .putLong(CONFIRMED_HOME_LATITUDE, Double.doubleToRawLongBits(latitude))
                .putLong(CONFIRMED_HOME_LONGITUDE, Double.doubleToRawLongBits(longitude))
                .apply();
    }

    void confirmWorkPlace(String address, double latitude, double longitude) {
        preferences.edit()
                .putString(CONFIRMED_WORK_ADDRESS, address.trim())
                .putLong(CONFIRMED_WORK_LATITUDE, Double.doubleToRawLongBits(latitude))
                .putLong(CONFIRMED_WORK_LONGITUDE, Double.doubleToRawLongBits(longitude))
                .apply();
    }

    boolean isHomePlaceConfirmed(String address, Double latitude, Double longitude) {
        return isConfirmedPlace(
                address,
                latitude,
                longitude,
                preferences.getString(CONFIRMED_HOME_ADDRESS, ""),
                getDouble(CONFIRMED_HOME_LATITUDE),
                getDouble(CONFIRMED_HOME_LONGITUDE)
        );
    }

    boolean isWorkPlaceConfirmed(String address, Double latitude, Double longitude) {
        return isConfirmedPlace(
                address,
                latitude,
                longitude,
                preferences.getString(CONFIRMED_WORK_ADDRESS, ""),
                getDouble(CONFIRMED_WORK_LATITUDE),
                getDouble(CONFIRMED_WORK_LONGITUDE)
        );
    }

    void save(
            String homeAddress,
            String workAddress,
            double homeLatitude,
            double homeLongitude,
            double workLatitude,
            double workLongitude,
            String startFrom,
            String startTo,
            String endFrom,
            String endTo,
            NavigationApp navigationApp,
            MusicApp musicApp,
            boolean autoPlay,
            MediaCommandType mediaCommandType,
            long musicDelay,
            boolean weekendMode
    ) {
        TimeRange.parse(startFrom, startTo);
        TimeRange.parse(endFrom, endTo);

        preferences.edit()
                .putString(HOME_ADDRESS, homeAddress.trim())
                .putString(WORK_ADDRESS, workAddress.trim())
                .putLong(HOME_LATITUDE, Double.doubleToRawLongBits(homeLatitude))
                .putLong(HOME_LONGITUDE, Double.doubleToRawLongBits(homeLongitude))
                .putLong(WORK_LATITUDE, Double.doubleToRawLongBits(workLatitude))
                .putLong(WORK_LONGITUDE, Double.doubleToRawLongBits(workLongitude))
                .putString(START_FROM, startFrom.trim())
                .putString(START_TO, startTo.trim())
                .putString(END_FROM, endFrom.trim())
                .putString(END_TO, endTo.trim())
                .putString(NAVIGATION_APP, navigationApp.name())
                .putString(MUSIC_APP, musicApp.name())
                .putBoolean(AUTO_PLAY, autoPlay)
                .putString(MEDIA_COMMAND, mediaCommandType.name())
                .putLong(MUSIC_DELAY, musicDelay)
                .putBoolean(WEEKEND_MODE, weekendMode)
                .apply();
    }

    void saveOperationPreferences(
            String startFrom,
            String startTo,
            String endFrom,
            String endTo,
            NavigationApp navigationApp,
            MusicApp musicApp,
            boolean autoPlay,
            MediaCommandType mediaCommandType,
            long musicDelay,
            boolean weekendMode
    ) {
        TimeRange.parse(startFrom, startTo);
        TimeRange.parse(endFrom, endTo);

        preferences.edit()
                .putString(START_FROM, startFrom.trim())
                .putString(START_TO, startTo.trim())
                .putString(END_FROM, endFrom.trim())
                .putString(END_TO, endTo.trim())
                .putString(NAVIGATION_APP, navigationApp.name())
                .putString(MUSIC_APP, musicApp.name())
                .putBoolean(AUTO_PLAY, autoPlay)
                .putString(MEDIA_COMMAND, mediaCommandType.name())
                .putLong(MUSIC_DELAY, musicDelay)
                .putBoolean(WEEKEND_MODE, weekendMode)
                .apply();
    }

    List<DestinationEntry> loadExtraDestinations() {
        int count = preferences.getInt(EXTRA_DESTINATION_COUNT, 0);
        List<DestinationEntry> destinations = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String name = preferences.getString(EXTRA_DESTINATION_NAME + index, "");
            String address = preferences.getString(EXTRA_DESTINATION_ADDRESS + index, "");
            Double latitude = getDouble(EXTRA_DESTINATION_LATITUDE + index);
            Double longitude = getDouble(EXTRA_DESTINATION_LONGITUDE + index);
            if ((name != null && !name.trim().isEmpty()) || (address != null && !address.trim().isEmpty())) {
                destinations.add(new DestinationEntry(name, address, latitude, longitude));
            }
        }
        return destinations;
    }

    void saveExtraDestinations(List<DestinationEntry> destinations) {
        SharedPreferences.Editor editor = preferences.edit();
        writeExtraDestinations(editor, destinations).apply();
    }

    void saveDestinationPlaces(
            String homeAddress,
            String workAddress,
            Double homeLatitude,
            Double homeLongitude,
            Double workLatitude,
            Double workLongitude,
            List<DestinationEntry> extraDestinations
    ) {
        SharedPreferences.Editor editor = preferences.edit()
                .putString(HOME_ADDRESS, homeAddress.trim())
                .putString(WORK_ADDRESS, workAddress.trim());
        putOptionalDouble(editor, HOME_LATITUDE, homeLatitude);
        putOptionalDouble(editor, HOME_LONGITUDE, homeLongitude);
        putOptionalDouble(editor, WORK_LATITUDE, workLatitude);
        putOptionalDouble(editor, WORK_LONGITUDE, workLongitude);
        writeExtraDestinations(editor, extraDestinations).apply();
    }

    private SharedPreferences.Editor writeExtraDestinations(
            SharedPreferences.Editor editor,
            List<DestinationEntry> destinations
    ) {
        int previousCount = preferences.getInt(EXTRA_DESTINATION_COUNT, 0);
        for (int index = 0; index < previousCount; index++) {
            editor.remove(EXTRA_DESTINATION_NAME + index)
                    .remove(EXTRA_DESTINATION_ADDRESS + index)
                    .remove(EXTRA_DESTINATION_LATITUDE + index)
                    .remove(EXTRA_DESTINATION_LONGITUDE + index);
        }

        int savedCount = 0;
        for (DestinationEntry destination : destinations) {
            if (destination == null || (!destination.hasName() && !destination.hasAddress())) {
                continue;
            }
            editor.putString(EXTRA_DESTINATION_NAME + savedCount, destination.name)
                    .putString(EXTRA_DESTINATION_ADDRESS + savedCount, destination.address);
            if (destination.latitude != null && destination.longitude != null) {
                editor.putLong(EXTRA_DESTINATION_LATITUDE + savedCount, Double.doubleToRawLongBits(destination.latitude))
                        .putLong(EXTRA_DESTINATION_LONGITUDE + savedCount, Double.doubleToRawLongBits(destination.longitude));
            }
            savedCount++;
        }
        return editor.putInt(EXTRA_DESTINATION_COUNT, savedCount);
    }

    private void putOptionalDouble(SharedPreferences.Editor editor, String key, Double value) {
        if (value == null) {
            editor.remove(key);
        } else {
            editor.putLong(key, Double.doubleToRawLongBits(value));
        }
    }

    String getStartFrom() {
        return preferences.getString(START_FROM, "06:00");
    }

    String getStartTo() {
        return preferences.getString(START_TO, "10:00");
    }

    String getEndFrom() {
        return preferences.getString(END_FROM, "17:00");
    }

    String getEndTo() {
        return preferences.getString(END_TO, "21:00");
    }

    long getMusicDelay() {
        return preferences.getLong(MUSIC_DELAY, 1500L);
    }

    Double getHomeLatitude() {
        return getDouble(HOME_LATITUDE);
    }

    Double getHomeLongitude() {
        return getDouble(HOME_LONGITUDE);
    }

    Double getWorkLatitude() {
        return getDouble(WORK_LATITUDE);
    }

    Double getWorkLongitude() {
        return getDouble(WORK_LONGITUDE);
    }

    private boolean isConfirmedPlace(
            String address,
            Double latitude,
            Double longitude,
            String confirmedAddress,
            Double confirmedLatitude,
            Double confirmedLongitude
    ) {
        String current = address == null ? "" : address.trim();
        String confirmed = confirmedAddress == null ? "" : confirmedAddress.trim();
        return !current.isEmpty()
                && current.equals(confirmed)
                && latitude != null
                && longitude != null
                && confirmedLatitude != null
                && confirmedLongitude != null
                && nearlySame(latitude, confirmedLatitude)
                && nearlySame(longitude, confirmedLongitude);
    }

    private boolean nearlySame(double first, double second) {
        return Math.abs(first - second) < 0.0000001;
    }

    private Double getDouble(String key) {
        if (!preferences.contains(key)) {
            return null;
        }
        return Double.longBitsToDouble(preferences.getLong(key, 0L));
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return type.getEnumConstants()[0];
        }
    }
}
