package org.prebid.pg.delstats.services;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.exception.DeliveryReportProcessingException;
import org.prebid.pg.delstats.exception.InvalidRequestException;
import org.prebid.pg.delstats.model.dto.DeliveryReportSummaryToPlannerAdapterDto;
import org.prebid.pg.delstats.model.dto.PlanDataSummary;
import org.prebid.pg.delstats.persistence.DeliveryReport;
import org.prebid.pg.delstats.persistence.DeliveryReportSummary;
import org.prebid.pg.delstats.persistence.SystemState;
import org.prebid.pg.delstats.repository.DeliveryProgressReportsRepository;
import org.prebid.pg.delstats.repository.DeliveryReportSummaryRepository;
import org.prebid.pg.delstats.repository.SystemStateRepository;
import org.prebid.pg.delstats.utils.MockSystemService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.io.File;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeliveryReportSummaryServiceTest {

    private SoftAssertions softAssertions;

    @Mock
    private DeliveryReportSummaryRepository deliveryReportSummaryRepositoryMock;

    @Mock
    private DeliveryReportProcessor deliveryReportProcessorMock;

    @Mock
    private SystemStateRepository systemStateRepositoryMock;

    @Mock
    private DeliveryProgressReportsRepository deliveryProgressReportsRepositoryMock;

    @Mock
    private ServerConfiguration configMock;

    private DeliveryReportsDataService deliveryDataService;

    private DeliveryReportSummaryService reportSummaryService;

    private Instant now = Instant.now();

    private Instant future = now.plus(60, ChronoUnit.SECONDS);

    private Instant nextHour = now.plus(1, ChronoUnit.HOURS);

    @BeforeEach
    public void setUpBeforeEach() {
        softAssertions = new SoftAssertions();

        lenient().when(configMock.getLineItemSummaryPagerSize()).thenReturn(10);
        lenient().when(configMock.getDeliveryReportsPageSize()).thenReturn(10);
        lenient().when(configMock.getDeliveryReportInstanceNameCacheExpirationUnit()).thenReturn(ChronoUnit.MINUTES);
        lenient().when(configMock.getLineItemSummaryMaxTimeRangeSeconds()).thenReturn(86400);
        lenient().when(configMock.isDeliverySummaryServiceEnabled()).thenReturn(true);
        lenient().when(configMock.getDeliverySummaryServiceAggregateInterval()).thenReturn(60);

        SystemService systemService = new MockSystemService();

        deliveryDataService = new DeliveryReportsDataService(deliveryProgressReportsRepositoryMock, deliveryReportProcessorMock, configMock, systemService);
        reportSummaryService = new DeliveryReportSummaryService(deliveryReportSummaryRepositoryMock, deliveryDataService, systemStateRepositoryMock, configMock, systemService);
    }

    @Test
    public void shouldBuildReportWithoutException() {
        Instant now = Instant.now();
        Set<String> metrics = Collections.singleton("winEvents");
        List<DeliveryReportSummary> deliveryReportSummaries = Lists.newArrayList(
                buildDeliveryReportSummary("lineX", now.minusSeconds(120)),
                buildDeliveryReportSummary("lineX", now.minusSeconds(60)),
                buildDeliveryReportSummary("lineY", now.minusSeconds(120)),
                buildDeliveryReportSummary("lineY", now.minusSeconds(60))
        );
        softAssertions.assertThatCode(() -> reportSummaryService
                .buildLineItemSummaryIntervalReport(deliveryReportSummaries, metrics, 1))
                .doesNotThrowAnyException();
        softAssertions.assertAll();
    }

    @Test
    void shouldNotThrowExceptionRetrievingLineItemSummariesFromMocks() {
        String lineItemIds = "lineX";
        Set<String> metrics = Collections.singleton("winEvents");
        Page<DeliveryReportSummary> mockPage = buildSummaryPage(lineItemIds, now);
        when(deliveryReportSummaryRepositoryMock.findByLineIdAndReportWindowHour(anyList(), any(), any(), any()))
                .thenReturn(mockPage);
        softAssertions.assertThatCode(() ->
                reportSummaryService.getLineItemSummaryReport(lineItemIds, now.minusSeconds(120), now, metrics, 5))
                .doesNotThrowAnyException();
        softAssertions.assertAll();
    }

    @Test
    public void shouldGroupByIntervals() {
        List<DeliveryReportSummary> deliveryReportSummaries = Lists.newArrayList(
                buildDeliveryReportSummary("lineX", now.minusSeconds(60)),
                buildDeliveryReportSummary("lineX", now),
                buildDeliveryReportSummary("lineY", now.minusSeconds(60)),
                buildDeliveryReportSummary("lineY", now));
        softAssertions.assertThat(reportSummaryService
                .groupByIntervals(deliveryReportSummaries,1, now.minusSeconds(180), now.plusSeconds(60)))
                .hasSize(4);
        softAssertions.assertAll();
    }

    @Test
    void shouldNotThrowExceptionBuildingSummariesFromNonEmptyList() {
        List<DeliveryReportSummary> deliveryReportSummaries = Lists.newArrayList(
                buildDeliveryReportSummary("lineX", now.plusSeconds(60)),
                buildDeliveryReportSummary("lineX", now),
                buildDeliveryReportSummary("lineY", now.plusSeconds(60)),
                buildDeliveryReportSummary("lineY", now));
        softAssertions.assertThatCode(() ->
                reportSummaryService.buildSummaries("lineX", now, nextHour, Collections.emptySet(), 60, deliveryReportSummaries))
                .doesNotThrowAnyException();
        softAssertions.assertAll();
    }

    @Test
    void shouldNotThrowExceptionWhenRunningDeliveryReportSummary() {
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp then = Timestamp.from(now.toInstant().minusSeconds(300));
        softAssertions.assertThatCode(() -> reportSummaryService.runDeliveryReportSummary(now, then, true))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRunDeliveryReportSummary() {
        int saved = 5;
        given(deliveryReportSummaryRepositoryMock.insertLineSummariesDirectly(any(), any())).willReturn(saved);
        Timestamp end = Timestamp.from(Instant.now());
        Timestamp start = Timestamp.from(end.toInstant().minusSeconds(300));
        reportSummaryService.runDeliveryReportSummary(start, end, true);
        verify(systemStateRepositoryMock, times(2)).store(any(), any());
        verify(systemStateRepositoryMock, times(1))
                .saveDeliverySummaryReportStates(any(), any(), any(), any(), any());
    }

    @Test
    void shouldRunDeliveryReportSummaryThrowsDeliveryReportProcessingException() {
        given(deliveryReportSummaryRepositoryMock.insertLineSummariesDirectly(any(), any()))
                .willThrow(new IllegalArgumentException());
        Timestamp end = Timestamp.from(Instant.now());
        Timestamp start = Timestamp.from(end.toInstant().minusSeconds(300));
        assertThrows(DeliveryReportProcessingException.class, () -> {
            reportSummaryService.runDeliveryReportSummary(start, end, true);
        });
    }

    @Test
    void shouldRecreateLineItemSummaryReport() {
        Timestamp end = Timestamp.from(Instant.now());
        Timestamp start = Timestamp.from(end.toInstant().minusSeconds(300));
        reportSummaryService.recreateLineItemSummaryReport(start, end, true);
        verify(deliveryReportSummaryRepositoryMock, times(1)).insertLineSummariesDirectly(any(), any());
    }

    @Test
    void shouldRecreateLineItemSummaryReportIfNotOverwrite() {
        Timestamp end = Timestamp.from(Instant.now());
        Timestamp start = Timestamp.from(end.toInstant().minusSeconds(300));
        given(deliveryReportSummaryRepositoryMock.countByReportWindow(any(), any())).willReturn(0);

        reportSummaryService.recreateLineItemSummaryReport(start, end, false);
        verify(deliveryReportSummaryRepositoryMock, times(1)).insertLineSummariesDirectly(any(), any());
    }

    @Test
    void shouldRecreateLineItemSummaryReportThrowsExceptionIfNotOverwriteExistingReports() {
        Timestamp end = Timestamp.from(Instant.now());
        Timestamp start = Timestamp.from(end.toInstant().minusSeconds(300));
        given(deliveryReportSummaryRepositoryMock.countByReportWindow(any(), any())).willReturn(2);

        assertThrows(InvalidRequestException.class, () -> {
            reportSummaryService.recreateLineItemSummaryReport(start, end, false);
        });
    }

    @Test
    void shouldGetDeliveryReport() {
        testGetDeliveryReport(null, null);
    }

    @Test
    void shouldGetDeliveryReportIfMissingStartTime() {
        testGetDeliveryReport(null, now.toString());
    }

    @Test
    void shouldGetDeliveryReportIfMissingEndTime() {
        testGetDeliveryReport(Instant.now().minus(1, ChronoUnit.HOURS).toString(), null);
    }

    private void testGetDeliveryReport(String start, String end) {
        Instant now = Instant.now();
        SystemState state = SystemState.builder()
                .val(Timestamp.from(now).toString())
                .build();
        List<PlanDataSummary> summaries = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            summaries.add(new PlanDataSummary(String.valueOf(i), "10,20,30,40"));
        }
        given(systemStateRepositoryMock.retrieveByTag(any())).willReturn(state);
        Page<DeliveryReportSummary> page =  buildSummaryPage("1,2,3", now);
        given(deliveryReportSummaryRepositoryMock.findByReportWindowHour(any(), any(), any()))
                .willReturn(page);
        given(deliveryReportSummaryRepositoryMock.findPlanDataByReportWindowHour(any(), any())).willReturn(summaries);
        DeliveryReportSummaryToPlannerAdapterDto dto = reportSummaryService.getDeliverySummaryReport(start, end);
        assertThat(dto.getLineDeliverySummaries().size()).isEqualTo(3);
    }

    @Test
    void shouldGetDeliveryReportIfGivenValidTimesEvenWhenNoPreviousStateFound() {
        Instant end = Instant.now();
        Instant start = end.minusSeconds(300);
        DeliveryReportSummaryToPlannerAdapterDto dto =
                reportSummaryService.getDeliverySummaryReport(start.toString(), end.toString(), "", "test");
        assertThat(dto.getLineDeliverySummaries()).isNotNull();
    }

    @Test
    void shouldGetLineItemSummaryReport() {
        Page<DeliveryReportSummary> page = new PageImpl<>(Collections.emptyList());
        given(deliveryReportSummaryRepositoryMock.findByReportWindowHour(any(), any(), any()))
                .willReturn(page);
        Instant end = Instant.now();
        Instant start = end.minusSeconds(300);
        List<Map<String, Object>> result =
                reportSummaryService.getLineItemSummaryReport("", start, end, Collections.emptySet(), 1);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void shouldGetLineItemSummaryReportReturnEmptyIfThrowsException() {
        Page<DeliveryReportSummary> page = new PageImpl<>(Collections.emptyList());
        given(deliveryReportSummaryRepositoryMock.findByReportWindowHour(any(), any(), any()))
                .willThrow(new IllegalArgumentException());
        Instant end = Instant.now();
        Instant start = end.minusSeconds(300);
        List<Map<String, Object>> result =
                reportSummaryService.getLineItemSummaryReport("", start, end, Collections.emptySet(), 1);
        assertThat(result.isEmpty()).isTrue();
    }

    private Page<DeliveryReportSummary> buildSummaryPage(String lineItemIds, Instant now) {
        List<DeliveryReportSummary> mockResults = new LinkedList<>();
        for (String lineItemId : lineItemIds.split(","))
            mockResults.add(buildDeliveryReportSummary(lineItemId, now));
        return new PageImpl<>(mockResults);
    }

    private DeliveryReportSummary buildDeliveryReportSummary(String lineItemId, Instant now) {
        return DeliveryReportSummary.builder()
                .id(1001)
                .bidderCode("bc")
                .lineItemId(lineItemId)
                .extLineItemId(lineItemId)
                .lineItemSource("test")
                .accountAuctions(11)
                .domainMatched(7)
                .targetMatched(6)
                .targetMatchedButFcapped(0)
                .targetMatchedButFcapLookupFailed(0)
                .pacingDeferred(2)
                .sentToBidder(5)
                .sentToClient(1)
                .sentToClientAsTopMatch(1)
                .sentToBidderAsTopMatch(3)
                .receivedFromBidder(2)
                .receivedFromBidderInvalidated(1)
                .winEvents(2)
                .dataWindowStartTimestamp(now.minusSeconds(60))
                .dataWindowEndTimestamp(now)
                .reportWindowStartTimestamp(now.minusSeconds(60))
                .reportWindowEndTimestamp(now)
                .createdAt(now)
                .build();
    }

    private Page<DeliveryReport> buildReportPage(Timestamp startTime, Timestamp endTime) throws Exception {
        DeliveryReport report1 = DeliveryReport.builder()
                .reportId("11")
                .lineItemId("bidderPG-1111")
                .dataWindowStartTimestamp(startTime)
                .dataWindowEndTimestamp(endTime)
                .lineItemStatus(loadTestData("line_item_summary/LineItemStatus1.json"))
                .build();
        DeliveryReport report2 = DeliveryReport.builder()
                .reportId("22")
                .lineItemId("bidderPG-1111")
                .dataWindowStartTimestamp(startTime)
                .dataWindowEndTimestamp(endTime)
                .lineItemStatus(loadTestData("line_item_summary/LineItemStatus2.json"))
                .build();
        List<DeliveryReport> reports = new ArrayList<>();
        reports.add(report1);
        reports.add(report2);
        return new PageImpl<>(reports);
    }

    private String loadTestData(String path) throws Exception {
        File resource1 = new ClassPathResource(path).getFile();
        return new String(Files.readAllBytes(resource1.toPath()));
    }

}

