package org.prebid.pg.delstats.utils;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.prebid.pg.delstats.exception.InvalidTimestampFormatException;

import java.sql.Timestamp;
import java.util.TimeZone;

public class TimestampUtilsTest {
    private static final String TEST_TIME = "2019-01-01 00:00:00";
    private static final String TEST_DATE = "2019-01-01";
    private static final String TEST_ISO_TIME = "2019-01-01T00:00:00.000-00:00";
    private static final String TEST_ISOZ_TIME = "2019-01-01T00:00:00.000Z";
    private static final String TEST_ISOZ_TIME_NO_MILLIS = "2019-01-01T00:00:00Z";

    private SoftAssertions softAssertions;

    private static TimeZone localTZ;

    @BeforeAll
    public static void initialize() {
        localTZ = TimeZone.getDefault();
    }

    @BeforeEach
    public void setup() {
        softAssertions = new SoftAssertions();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterAll
    public static void cleanup() {
        TimeZone.setDefault(localTZ);
    }

    @Test
    public void shouldConvertValidTimestampStrings() {
        softAssertions.assertThat(TimestampUtils.convertStringTimeToTimestamp(TEST_TIME, "test"))
                .isNotNull().isEqualTo(Timestamp.valueOf(TEST_TIME));
        softAssertions.assertThat(TimestampUtils.convertStringTimeToTimestamp(TEST_DATE, "test"))
                .isNotNull().isEqualTo(Timestamp.valueOf(TEST_TIME));
        softAssertions.assertThat(TimestampUtils.convertStringTimeToTimestamp(null, "test"))
                .isNull();

        String planStartTimeStamp = "2019-02-01T06:00:00.000Z";
        String planExpirationTimeStamp = "2019-02-01T06:59:59.999Z";
        String planUpdatedTimeStamp = "2019-01-31T22:16:44.000Z";

        softAssertions.assertThat(TimestampUtils.convertStringTimeToTimestamp(planStartTimeStamp, "testPlanStart")).isNotNull();
        softAssertions.assertThat(TimestampUtils.convertStringTimeToTimestamp(planExpirationTimeStamp, "testPlanEnd")).isNotNull();
        softAssertions.assertThat(TimestampUtils.convertStringTimeToTimestamp(planUpdatedTimeStamp, "testPlanUpdated")).isNotNull();

        softAssertions.assertAll();
    }

    @Test
    public void shouldConvertValidTimestampsRespectingTimeZones() {
        softAssertions.assertThat(TimestampUtils.convertStringTimeToTimestamp(TEST_ISO_TIME, "test"))
                .isNotNull().isEqualTo(Timestamp.valueOf(TEST_TIME));
        softAssertions.assertThat(TimestampUtils.convertStringTimeToTimestamp(TEST_ISOZ_TIME, "test"))
                .isNotNull().isEqualTo(Timestamp.valueOf(TEST_TIME));
        softAssertions.assertThat(TimestampUtils.convertStringTimeToTimestamp(TEST_ISOZ_TIME_NO_MILLIS, "test"))
                .isNotNull().isEqualTo(Timestamp.valueOf(TEST_TIME));
        softAssertions.assertAll();
    }

    @Test
    public void shouldThrowExceptionForInvalidTimestampStrings() {
        softAssertions.assertThatCode(() -> TimestampUtils.convertStringTimeToTimestamp("", "empty"))
                .isInstanceOf(InvalidTimestampFormatException.class);
        softAssertions.assertAll();
    }
}
