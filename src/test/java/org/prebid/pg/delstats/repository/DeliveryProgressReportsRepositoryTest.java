package org.prebid.pg.delstats.repository;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.prebid.pg.delstats.persistence.DeliveryReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

@SpringBootTest
public class DeliveryProgressReportsRepositoryTest {
    SoftAssertions softAssertions;

    @Autowired
    DeliveryProgressReportsRepository deliveryProgressReportsRepository;

    @Autowired
    EntityManager entityManager;

    Timestamp then;
    Timestamp then14minsAgo;
    Timestamp then15minsAgo;
    Timestamp then29minsAgo;
    Timestamp then30minsAgo;
    Timestamp now;
    Timestamp next15mins;

    @BeforeEach
    public void setup() {
        softAssertions = new SoftAssertions();

        then = Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS));
        then14minsAgo = Timestamp.from(Instant.now().minus(14, ChronoUnit.MINUTES));
        then15minsAgo = Timestamp.from(Instant.now().minus(15, ChronoUnit.MINUTES));
        then29minsAgo = Timestamp.from(Instant.now().minus(29, ChronoUnit.MINUTES));
        then30minsAgo = Timestamp.from(Instant.now().minus(30, ChronoUnit.MINUTES));
        now = Timestamp.from(Instant.now());
        next15mins = Timestamp.from(Instant.now().plus(15, ChronoUnit.MINUTES));
    }

    @AfterEach
    public void clean() {
        entityManager.clear();
    }

    @Test
    public void shouldLoadContextCleanly() {
        softAssertions.assertThat(entityManager).isNotNull();
        softAssertions.assertThat(deliveryProgressReportsRepository).isNotNull();
        softAssertions.assertAll();
    }

    @Test
    @Transactional
    public void shouldFindOldReportsWhenDBHasAnOldReport() {
        DeliveryReport deliveryReport = makeSimpleDeliveryReport(then);
        entityManager.persist(deliveryReport);
        softAssertions.assertThat(deliveryProgressReportsRepository.findOldRecords(now, PageRequest.of(0, 1)))
                .hasSize(1);
        softAssertions.assertThat(deliveryProgressReportsRepository.findOldRecords(then, PageRequest.of(0, 1)))
                .isEmpty();
        softAssertions.assertAll();
    }

    @Test
    @Transactional
    public void shouldRetrieveByBidderCodeWithTimeRangeReturnExpectedNumberOfRecords() {
        DeliveryReport deliveryReport15minsAgo = makeDeliveryReport("bidder", "1", "{}", then15minsAgo);
        DeliveryReport deliveryReport30minsAgo = makeDeliveryReport("bidder", "2", "{}", then30minsAgo);
        DeliveryReport deliveryReportNow = makeDeliveryReport("bidder", "3", "{}", now);

        entityManager.persist(deliveryReport30minsAgo);
        entityManager.persist(deliveryReport15minsAgo);
        entityManager.persist(deliveryReportNow);

        Pageable page1 = PageRequest.of(0, 10);
        Pageable page2 = PageRequest.of(0, 10);
        Pageable page3 = PageRequest.of(0, 10);
        softAssertions.assertThat(deliveryProgressReportsRepository.findByBidderCodeAndReportTimestampRange("bidder", then30minsAgo, then15minsAgo, page1))
                .hasSize(1);
        softAssertions.assertThat(deliveryProgressReportsRepository.findByBidderCodeAndReportTimestampRange("bidder", then15minsAgo, now, page2))
                .hasSize(1);
        softAssertions.assertThat(deliveryProgressReportsRepository.findByBidderCodeAndReportTimestampRange("bidder", then30minsAgo, now, page3))
                .hasSize(2);
        softAssertions.assertAll();
    }

    @Test
    @Transactional
    public void shouldFindSummaryDataInValidLineItemStatus() {
        DeliveryReport deliveryReport15minsAgo = makeDeliveryReport("bidder", "1", "{}", then15minsAgo);
        DeliveryReport deliveryReport30minsAgo = makeDeliveryReport("bidder", "2", "{}", then30minsAgo);
        DeliveryReport deliveryReportNow = makeDeliveryReport("bidder", "3", "{}", now);

        entityManager.persist(deliveryReport30minsAgo);
        entityManager.persist(deliveryReport15minsAgo);
        entityManager.persist(deliveryReportNow);

        PageRequest pageRequest = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("report_timestamp").ascending());

        softAssertions.assertThat(deliveryProgressReportsRepository.retrieveSummaryData(then15minsAgo, now, pageRequest))
                .hasSize(1);
        softAssertions.assertThat(deliveryProgressReportsRepository.retrieveSummaryData(then30minsAgo, now, pageRequest))
                .hasSize(2);
        softAssertions.assertAll();
    }

    @Transactional
    @Test
    void shouldGetLineItemReportsWithTimeRange() {
        DeliveryReport deliveryReport15minsAgo = makeDeliveryReportForLineItemSummaries(
                "bidder", "1", "{}", then15minsAgo, now, then);
        DeliveryReport deliveryReport30minsAgo = makeDeliveryReportForLineItemSummaries(
                "bidder", "2", "{}", then30minsAgo, then15minsAgo, then);
        DeliveryReport deliveryReportNow = makeDeliveryReportForLineItemSummaries(
                "bidder", "3", "{}", now, next15mins, then);

        entityManager.persist(deliveryReport30minsAgo);
        entityManager.persist(deliveryReport15minsAgo);
        entityManager.persist(deliveryReportNow);

        Pageable pageRequest = PageRequest.of(0, 1000);

        softAssertions.assertThat(deliveryProgressReportsRepository.getLineItemReportsWithTimeRange(
                        then30minsAgo.toInstant(), then15minsAgo.toInstant(), pageRequest).getContent())
                .hasSize(1);
        softAssertions.assertThat(deliveryProgressReportsRepository.getLineItemReportsWithTimeRange(
                        then30minsAgo.toInstant(), now.toInstant(), pageRequest).getContent())
                .hasSize(2);
        softAssertions.assertAll();
    }

    @Transactional
    @Test
    void shouldGetLineItemReportsByLineIdsWithTimeRange() {

        DeliveryReport deliveryReport15minsAgo = makeDeliveryReportForLineItemSummaries(
                "bidder", "1", "{}", then15minsAgo, now, then);
        DeliveryReport deliveryReport30minsAgo = makeDeliveryReportForLineItemSummaries(
                "bidder", "2", "{}", then30minsAgo, then15minsAgo, then);
        DeliveryReport deliveryReportNow = makeDeliveryReportForLineItemSummaries(
                "bidder", "3", "{}", now, next15mins, then);

        entityManager.persist(deliveryReport30minsAgo);
        entityManager.persist(deliveryReport15minsAgo);
        entityManager.persist(deliveryReportNow);

        Pageable pageRequest = PageRequest.of(0, 1000);

        List<String> lineItemIds = Arrays.asList("1", "2", "3", "4");
        softAssertions.assertThat(deliveryProgressReportsRepository.getLineItemReportsByLineIdsWithTimeRange(
                        lineItemIds,  then30minsAgo.toInstant(), then14minsAgo.toInstant(), pageRequest).getContent())
                .hasSize(2);

        lineItemIds = Arrays.asList("1", "3", "4");
        softAssertions.assertThat(deliveryProgressReportsRepository.getLineItemReportsByLineIdsWithTimeRange(
                        lineItemIds, then30minsAgo.toInstant(), then15minsAgo.toInstant(), pageRequest).getContent())
                .hasSize(0);

        lineItemIds = Arrays.asList("1", "2", "3", "4");
        softAssertions.assertThat(deliveryProgressReportsRepository.getLineItemReportsByLineIdsWithTimeRange(
                        lineItemIds, then29minsAgo.toInstant(), now.toInstant(), pageRequest).getContent())
                .hasSize(1);

        lineItemIds = Arrays.asList("3", "4");
        softAssertions.assertThat(deliveryProgressReportsRepository.getLineItemReportsByLineIdsWithTimeRange(
                lineItemIds, then29minsAgo.toInstant(), now.toInstant(), pageRequest).getContent())
                .hasSize(0);

        softAssertions.assertAll();
    }

    @Transactional
    @Test
    void shouldGetReportsWithinTimeRange() {
        DeliveryReport deliveryReportNowLastminuteWindow = makeDeliveryReportWithTimeWindow("bidder", "3", "{}", now, 1, 0);
        DeliveryReport deliveryReport15minsAgoLastMinuteWindow = makeDeliveryReportWithTimeWindow("bidder", "1", "{}", then15minsAgo, 1,0);
        DeliveryReport deliveryReport30minsAgoTwoMinuteAgoWindow = makeDeliveryReportWithTimeWindow("bidder", "1", "{}", then30minsAgo, 2,1);
        DeliveryReport deliveryReport30minsAgoLastMinuteWindow = makeDeliveryReportWithTimeWindow("bidder", "2", "{}", then30minsAgo, 1, 0);

        entityManager.persist(deliveryReportNowLastminuteWindow);
        entityManager.persist(deliveryReport15minsAgoLastMinuteWindow);
        entityManager.persist(deliveryReport30minsAgoLastMinuteWindow);
        entityManager.persist(deliveryReport30minsAgoTwoMinuteAgoWindow);

        Pageable pageRequest = PageRequest.of(0, 1000);

        softAssertions.assertThat(deliveryProgressReportsRepository.findWithinDataStartEnd(now, now, pageRequest)).hasSize(0);
        softAssertions.assertThat(deliveryProgressReportsRepository.findWithinDataStartEnd(then15minsAgo, now, pageRequest)).hasSize(1);
        softAssertions.assertThat(deliveryProgressReportsRepository.findWithinDataStartEnd(then30minsAgo, now, pageRequest)).hasSize(2);
        softAssertions.assertAll();
    }

    private DeliveryReport makeDeliveryReportWithTimeWindow(String bidderCode, String lineItemId, String lineItemStatus, Timestamp reportTimestamp, int startMinsBefore, int endMinsBefore) {
        return makeDeliveryReportForLineItemSummaries(bidderCode, lineItemId, lineItemStatus,
                offsetTimestamp(reportTimestamp, Calendar.MINUTE, -startMinsBefore),
                offsetTimestamp(reportTimestamp, Calendar.MINUTE, -endMinsBefore), reportTimestamp);
    }

    private static Timestamp offsetTimestamp(Timestamp timestamp, int calendarField, int offset) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp.getTime());
        cal.add(calendarField, offset);
        return new Timestamp(cal.getTime().getTime());
    }

    private DeliveryReport makeDeliveryReport(String bidderCode, String lineItemId, String lineItemStatus, Timestamp reportTimestamp) {
        return makeDeliveryReportForLineItemSummaries(bidderCode, lineItemId, lineItemStatus, now, then, reportTimestamp);
    }

    private DeliveryReport makeSimpleDeliveryReport(Timestamp reportTimestamp) {
        return makeDeliveryReportForLineItemSummaries("", "", "{}", now, then, reportTimestamp);
    }

    private DeliveryReport makeDeliveryReportForLineItemSummaries(String bidderCode,
                                                                  String lineItemId,
                                                                  String lineItemStatus,
                                                                  Timestamp startTime,
                                                                  Timestamp endTime,
                                                                  Timestamp reportTime) {
        return DeliveryReport.builder()
                .bidderCode(bidderCode)
                .clientAuctions(0)
                .dataWindowEndTimestamp(startTime)
                .dataWindowStartTimestamp(endTime)
                .extLineItemId("")
                .instanceId("")
                .lineItemId(lineItemId)
                .lineItemStatus(lineItemStatus)
                .region("")
                .reportId(UUID.randomUUID().toString())
                .reportTimestamp(reportTime)
                .vendor("")
                .build();
    }
}
