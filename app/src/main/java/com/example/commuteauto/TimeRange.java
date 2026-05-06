package com.example.commuteauto;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

final class TimeRange {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    final LocalTime start;
    final LocalTime end;

    TimeRange(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    boolean contains(LocalTime time) {
        if (start.equals(end)) {
            return true;
        }
        if (start.isBefore(end)) {
            return !time.isBefore(start) && !time.isAfter(end);
        }
        return !time.isBefore(start) || !time.isAfter(end);
    }

    String display() {
        return FORMATTER.format(start) + " ~ " + FORMATTER.format(end);
    }

    static TimeRange parse(String startText, String endText) {
        return new TimeRange(parseTime(startText), parseTime(endText));
    }

    static LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value.trim(), FORMATTER);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("시간은 HH:mm 형식으로 입력해 주세요. 예: 07:30");
        }
    }
}
