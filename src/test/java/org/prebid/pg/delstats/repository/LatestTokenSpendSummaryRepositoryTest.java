package org.prebid.pg.delstats.repository;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.prebid.pg.delstats.persistence.LatestTokenSpendSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@SpringBootTest
public class LatestTokenSpendSummaryRepositoryTest {
    SoftAssertions softAssertions;

    @Autowired
    LatestTokenSpendSummaryRepository latestTokenSpendSummaryRepository;

    @Autowired
    EntityManager entityManager;

    Timestamp then;
    Timestamp then15minsAgo;
    Timestamp then30minsAgo;
    Timestamp now;

    @BeforeEach
    public void setup() {
        softAssertions = new SoftAssertions();

        then = Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS));
        then15minsAgo = Timestamp.from(Instant.now().minus(15, ChronoUnit.MINUTES));
        then30minsAgo = Timestamp.from(Instant.now().minus(30, ChronoUnit.MINUTES));
        now = Timestamp.from(Instant.now());
    }

    @AfterEach
    public void clean() {
        entityManager.clear();
    }

    @Test
    public void shouldLoadContextCleanly() {
        softAssertions.assertThat(entityManager).isNotNull();
        softAssertions.assertThat(latestTokenSpendSummaryRepository).isNotNull();
        softAssertions.assertAll();
    }

    @Test
    @Transactional
    public void shouldFindOldSummaryWhenDBHasAnOldSummary() {
        LatestTokenSpendSummary latestTokenSpendSummary = makeLatestTokenSpendSummary(then30minsAgo, then15minsAgo, then);
        entityManager.persist(latestTokenSpendSummary);
        softAssertions.assertThat(latestTokenSpendSummaryRepository.findOldRecords(now, PageRequest.of(0, 1)))
                .hasSize(1);
        softAssertions.assertThat(latestTokenSpendSummaryRepository.findOldRecords(then, PageRequest.of(0, 1)))
                .isEmpty();
        softAssertions.assertAll();
    }

    @Test
    @Transactional
    public void shouldRetrieveSince() {
        LatestTokenSpendSummary latestTokenSpendSummary = makeLatestTokenSpendSummary(then30minsAgo, then30minsAgo, then30minsAgo);
        entityManager.persist(latestTokenSpendSummary);
        softAssertions.assertThat(latestTokenSpendSummaryRepository.retrieveSince(then)).hasSize(1);
        softAssertions.assertThat(latestTokenSpendSummaryRepository.retrieveSince(then30minsAgo)).hasSize(1);
        softAssertions.assertThat(latestTokenSpendSummaryRepository.retrieveSince(then15minsAgo)).hasSize(0);
        softAssertions.assertAll();
    }

    public LatestTokenSpendSummary makeLatestTokenSpendSummary(Timestamp dataStart, Timestamp dataEnd, Timestamp reportTimestamp) {
        return LatestTokenSpendSummary.builder()
                .bidderCode("")
                .dataWindowStartTimestamp(dataStart)
                .dataWindowEndTimestamp(dataEnd)
                .extLineItemId("")
                .instanceId("")
                .lineItemId("")
                .region("")
                .reportTimestamp(reportTimestamp)
                .serviceInstanceId("")
                .summaryData("{}")
                .vendor("")
                .build();
    }
}
