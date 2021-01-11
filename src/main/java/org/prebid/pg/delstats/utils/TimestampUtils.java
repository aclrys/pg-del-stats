package org.prebid.pg.delstats.utils;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.prebid.pg.delstats.exception.InvalidTimestampFormatException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.TimeZone;

public class TimestampUtils {
    private static final DateFormatter ISO_DATE_FORMATTER = new DateFormatter();
    private static final StdDateFormat STD_DATE_FORMAT = new StdDateFormat();

    static {
        ISO_DATE_FORMATTER.setIso(DateTimeFormat.ISO.DATE_TIME);
        ISO_DATE_FORMATTER.setLenient(true);
        ISO_DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
        STD_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private TimestampUtils() {
    }

    public static Timestamp convertStringTimeToTimestamp(String time, long secsFromNowIfUndefined, String paramName) {
        return StringUtils.isEmpty(time) ? secondsFromNow(secsFromNowIfUndefined)
                : TimestampUtils.convertStringTimeToTimestamp(time, paramName);
    }

    public static Timestamp convertStringTimeToTimestamp(String time, String paramName) {
        if (time == null) {
            return null;
        }
        try {
            return Timestamp.from(STD_DATE_FORMAT.parse(time).toInstant());
        } catch (Exception e) {
            // Try another conversion
        }
        try {
            return Timestamp.valueOf(time);
        } catch (Exception e) {
            // Try another conversion
        }
        try {
            return new Timestamp(Date.valueOf(time).getTime());
        } catch (Exception e) {
            // Try another conversion
        }
        try {
            return new Timestamp(ISO_DATE_FORMATTER.parse(time, Locale.US).getTime());
        } catch (Exception e) {
            // Try another conversion
        }
        throw new InvalidTimestampFormatException("Invalid time format provided for " + paramName);
    }

    public static Timestamp secondsFromNow(long seconds) {
        return secondsFromThen(seconds, Instant.now());
    }

    public static Timestamp secondsFromThen(long seconds, Instant then) {
        return Timestamp.from(then.minus(seconds, ChronoUnit.SECONDS));
    }
}
