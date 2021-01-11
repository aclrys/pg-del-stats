package org.prebid.pg.delstats.metrics;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public class SimplisticExpiringSetTest {
    @Test
    public void shouldProvideAccurateSetCountsAndExpireEntries() {
        SoftAssertions softAssertions = new SoftAssertions();

        SimplisticExpiringSet<Integer> virtuallyNeverExpiringSet = new SimplisticExpiringSet(1, ChronoUnit.DAYS);

        softAssertions.assertThat(virtuallyNeverExpiringSet.addAll(Arrays.asList(1,2,3))).isTrue();
        // Force expiration once
        virtuallyNeverExpiringSet.getRecent(Instant.now().plus(3, ChronoUnit.DAYS));
        softAssertions.assertThat(virtuallyNeverExpiringSet.addAll(Arrays.asList(1,2,4))).isTrue();
        softAssertions.assertThat(virtuallyNeverExpiringSet.size()).isEqualTo(4);
        // Force expiration once
        virtuallyNeverExpiringSet.getRecent(Instant.now().plus(6, ChronoUnit.DAYS));
        softAssertions.assertThat(virtuallyNeverExpiringSet.size()).isEqualTo(3);
        // Force expiration once
        virtuallyNeverExpiringSet.getRecent(Instant.now().plus(9, ChronoUnit.DAYS));
        softAssertions.assertThat(virtuallyNeverExpiringSet.size()).isEqualTo(0);
        softAssertions.assertAll();
    }
}
