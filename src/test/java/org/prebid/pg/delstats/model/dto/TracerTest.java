package org.prebid.pg.delstats.model.dto;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TracerTest {
    private SoftAssertions softAssertions;

    private Tracer tracer;

    @BeforeEach
    public void init() {
        softAssertions = new SoftAssertions();

        tracer = new Tracer();
    }

    @Test
    void setTracer() {
        Filters filter = Filters.builder()
                .accountId("testAI")
                .bidderCode("testBC")
                .lineItemId("testLI")
                .region("testR")
                .vendor("testV")
                .build();
        Tracer tracerEnabled = Tracer.builder()
                .enabled(true)
                .durationInSeconds(5)
                .filters(filter)
                .expiresAt(Instant.MAX)
                .raw(true)
                .build();
        softAssertions.assertThatCode(() -> tracer.setTracer(tracerEnabled)).doesNotThrowAnyException();
        softAssertions.assertThat(tracer)
                .hasFieldOrPropertyWithValue("durationInSeconds", 5)
                .hasFieldOrPropertyWithValue("enabled", Boolean.TRUE)
                .hasFieldOrPropertyWithValue("raw", Boolean.TRUE);
        softAssertions.assertThat(tracer.isActive()).isTrue();
        softAssertions.assertThat(tracer.isActiveAndRaw()).isTrue();
        softAssertions.assertThat(tracer.isMatchingOn("testV", "testR", "testBC", "testLI")).isTrue();
        softAssertions.assertThat(tracer.matchAccount("testAI")).isTrue();
        softAssertions.assertThat(tracer.matchBidderCode("testBC")).isTrue();
        softAssertions.assertThat(tracer.matchLineItemId("testLI")).isTrue();
        softAssertions.assertThat(tracer.matchRegion("testR")).isTrue();
        softAssertions.assertThat(tracer.matchVendor("testV")).isTrue();
        softAssertions.assertThat(tracer.match("testV", "testR", "testBC", "testLI", "testAI")).isTrue();
        softAssertions.assertAll();
    }
}