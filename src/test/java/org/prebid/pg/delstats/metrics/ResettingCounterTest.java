package org.prebid.pg.delstats.metrics;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

public class ResettingCounterTest {

    @Test
    public void shouldResetCountBetweenMarks() {
        SoftAssertions softAssertions = new SoftAssertions();
        ResettingCounter counter = new ResettingCounter();

        softAssertions.assertThat(counter.getCount()).isEqualTo(0);
        counter.inc();
        softAssertions.assertThat(counter.getCount()).isEqualTo(1);
        counter.inc(3);
        softAssertions.assertThat(counter.getCount()).isEqualTo(3);
        counter.inc(2);
        softAssertions.assertThat(counter.getCount()).isEqualTo(2);

        softAssertions.assertAll();
    }
}
