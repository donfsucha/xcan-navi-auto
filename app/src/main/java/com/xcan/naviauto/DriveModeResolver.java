package com.xcan.naviauto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

final class DriveModeResolver {
    DriveMode resolve(UserSettings settings) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (settings.weekendModeEnabled && isWeekend(today)) {
            return DriveMode.WEEKEND;
        }
        if (settings.commuteStartRange.contains(now)) {
            return DriveMode.GO_TO_WORK;
        }
        if (settings.commuteEndRange.contains(now)) {
            return DriveMode.GO_HOME;
        }
        return DriveMode.MANUAL;
    }

    Place destinationFor(DriveMode mode, UserSettings settings) {
        if (mode == DriveMode.GO_TO_WORK) {
            return settings.workPlace;
        }
        if (mode == DriveMode.GO_HOME) {
            return settings.homePlace;
        }
        return null;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}
