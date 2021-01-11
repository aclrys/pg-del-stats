package org.prebid.pg.delstats.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.prebid.pg.delstats.config.DeploymentConfiguration;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.metrics.GraphiteMetricsRecorder;
import org.prebid.pg.delstats.model.dto.DeliveryReportFromPbsDto;
import org.prebid.pg.delstats.model.dto.DeliveryReportLineStatsDto;
import org.prebid.pg.delstats.model.dto.LineItemSummaryReport;
import org.prebid.pg.delstats.persistence.DeliveryReport;
import org.prebid.pg.delstats.repository.DeliveryProgressReportsRepository;
import org.prebid.pg.delstats.utils.LineItemStatusUtilsTest;
import org.prebid.pg.delstats.utils.MockSystemService;
import org.prebid.pg.delstats.utils.ResourceUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryReportsDataServiceTest {
    private SoftAssertions softAssertions;

    private DeliveryProgressReportsRepository deliveryProgressReportsRepository;

    private DeliveryReportProcessor deliveryReportProcessor;

    private DeliveryReportsDataService deliveryReportsDataService;

    private ServerConfiguration configuration;

    private DeploymentConfiguration deploymentConfiguration;

    private GraphiteMetricsRecorder graphiteMetricsRecorder;

    private ObjectMapper objectMapper;

    private int saveAllCounter;

    private List<DeliveryReportLineStatsDto> statsDtos;

    @BeforeEach
    public void setUpBeforeEach() throws Exception {
        softAssertions = new SoftAssertions();

        deliveryProgressReportsRepository = mock(DeliveryProgressReportsRepository.class);
        configuration = mock(ServerConfiguration.class);
        deploymentConfiguration = mock(DeploymentConfiguration.class);
        graphiteMetricsRecorder = mock(GraphiteMetricsRecorder.class);

        SystemService systemService = new MockSystemService();
        deliveryReportProcessor = new DeliveryReportProcessor(configuration, systemService);
        objectMapper = systemService.getObjectMapper();

        lenient().when(configuration.getLineItemBidderCodeSeparator()).thenReturn("PG-");
        lenient().when(configuration.getDeliveryReportInstanceNameCacheExpirationUnit()).thenReturn(ChronoUnit.MINUTES);
        lenient().when(configuration.isValidationEnabled()).thenReturn(true);
        lenient().when(configuration.getLineItemSummaryMaxTimeRangeSeconds()).thenReturn(86400);
        lenient().when(configuration.getBidderAliasMappingString()).thenReturn("A:B");
        lenient().when(deploymentConfiguration.getProfile()).thenReturn(DeploymentConfiguration.ProfileType.PROD);
        lenient().when(deliveryProgressReportsRepository.saveAll(anyIterable())).thenAnswer(invocationOnMock -> {
            List<DeliveryReport> iterable = ((List<DeliveryReport>) invocationOnMock.getArguments()[0]);
            saveAllCounter += iterable.size();
            return iterable;
        });

        deliveryReportsDataService =
                new DeliveryReportsDataService(deliveryProgressReportsRepository, deliveryReportProcessor,
                        configuration, new MockSystemService(deploymentConfiguration));

        when(deliveryProgressReportsRepository.findByBidderCodeAndReportTimestampRange(anyString(), any(Timestamp.class), any(Timestamp.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        lenient().when(graphiteMetricsRecorder.repositoryFetchDeliveryReportLinesTimer())
                .thenReturn(Optional.empty());
        lenient().when(graphiteMetricsRecorder.getDeliveryReportsPerformanceTimer())
                .thenReturn(Optional.empty());
        when(configuration.getDeliveryReportsPageSize()).thenReturn(5);

        statsDtos = new LinkedList<>();
        for(int i = 0; i < 3; i++) {
            statsDtos.add(makeDeliveryStats("b1", "l1"));
            statsDtos.add(makeDeliveryStats("b2", "l1"));
            statsDtos.add(makeDeliveryStats("b1", "l2"));
            statsDtos.add(makeDeliveryStats("b2", "l2"));
            statsDtos.add(makeDeliveryStats("b3", "l1"));
            statsDtos.add(makeDeliveryStats("b3", "l2"));
            statsDtos.add(makeDeliveryStats("b3", "l3"));
        }

    }

    @Test
    public void shouldStoreDeliveryStatsFromDto() throws Exception {
        List<JsonNode> lineItemStatusDtoList = new ArrayList<>();
        Timestamp now = Timestamp.from(Instant.now());
        for (int i = 0; i < 3; i++) {
            lineItemStatusDtoList.add(makeLineItemStatusDto(i));
        }
        Timestamp testTime = Timestamp.from(Instant.now());
        deliveryReportsDataService.storeReport(
                DeliveryReportFromPbsDto.builder()
                        .lineItemStatus(lineItemStatusDtoList)
                        .dataWindowStartTimeStamp(now)
                        .dataWindowEndTimeStamp(now)
                        .build(), testTime.toString(), "");
        deliveryReportsDataService.storeReport(
                DeliveryReportFromPbsDto.builder()
                        .lineItemStatus(lineItemStatusDtoList)
                        .dataWindowStartTimeStamp(now)
                        .dataWindowEndTimeStamp(now)
                        .build());

        verify(deliveryProgressReportsRepository, times(2)).saveAll(anyIterable());
    }

    @Test
    public void shouldSaveCorrectNumberOfLineItemStatusRecordsFromDeliveryReportWithBadRecord() throws Exception {
        String testingDeliveryProgressReport = ResourceUtil.readFromClasspath("DeliveryProgressReportTestExample.json");
        DeliveryReportFromPbsDto deliveryReportFromPbsDto = objectMapper.readValue(testingDeliveryProgressReport, DeliveryReportFromPbsDto.class);
        softAssertions.assertThatCode(() -> deliveryReportsDataService.storeLines(
                deliveryReportFromPbsDto,
                Timestamp.from(Instant.now()))).doesNotThrowAnyException();
        List<JsonNode> badLineItemStatusList = new LinkedList<>();
        badLineItemStatusList.add(deliveryReportFromPbsDto.getLineItemStatus().get(0));
        badLineItemStatusList.add(objectMapper.readTree("{}"));
        badLineItemStatusList.add(deliveryReportFromPbsDto.getLineItemStatus().get(0));
        DeliveryReportFromPbsDto badReportFromPbsDto = DeliveryReportFromPbsDto.builder()
                .dataWindowEndTimeStamp(deliveryReportFromPbsDto.getDataWindowEndTimeStamp())
                .dataWindowStartTimeStamp(deliveryReportFromPbsDto.getDataWindowStartTimeStamp())
                .reportId(deliveryReportFromPbsDto.getReportId())
                .instanceId(deliveryReportFromPbsDto.getInstanceId())
                .reportTimeStamp(deliveryReportFromPbsDto.getReportTimeStamp())
                .vendor(deliveryReportFromPbsDto.getVendor())
                .region(deliveryReportFromPbsDto.getRegion())
                .clientAuctions(deliveryReportFromPbsDto.getClientAuctions())
                .lineItemStatus(badLineItemStatusList)
                .build();
        int savesSoFar = saveAllCounter;
        softAssertions.assertThatCode(
                () -> deliveryReportsDataService.storeReport(badReportFromPbsDto))
                .doesNotThrowAnyException();

        softAssertions.assertThat(saveAllCounter).isEqualTo(savesSoFar+2);
        softAssertions.assertAll();
    }

    @Test
    public void shouldThrowExceptionFromStoreLineWhenRepoThrowsExceptions() throws Exception {
        Timestamp now = Timestamp.from(Instant.now());
        InputStream inputStream = LineItemStatusUtilsTest.class.getClassLoader().getResourceAsStream("LineItemStatus.json");

        List<JsonNode> lineItemStatusDtos = objectMapper.readValue(inputStream, new TypeReference<List<JsonNode>>(){});
        DeliveryReportFromPbsDto deliveryReportFromPbsDto = DeliveryReportProcessorTest.
                makeDeliveryReportFromPbsDtoWithLineItemStatus("reportId", "vendor", "instanceId",
                        Timestamp.from(Instant.now().minusSeconds(60)), now, lineItemStatusDtos);
        when(deliveryProgressReportsRepository.saveAll(anyIterable()))
                .thenThrow(new TypeMismatchDataAccessException("first"))
                .thenThrow(new RuntimeException("second"));
        softAssertions.assertThatCode(() -> deliveryReportsDataService.storeLines(deliveryReportFromPbsDto, now))
                .isExactlyInstanceOf(TypeMismatchDataAccessException.class).hasMessage("first");
        softAssertions.assertThatCode(() -> deliveryReportsDataService.storeLines(deliveryReportFromPbsDto, now))
                .isExactlyInstanceOf(RuntimeException.class).hasMessage("second");
        softAssertions.assertAll();
    }

    @Test
    public void shouldNotThrowExceptionCallingRetrieveByBidderCode() {
        // Placeholder test
        String start = "2019-01-07 00:00:00";
        String end = "2019-01-07 01:00:00";
        softAssertions.assertThatCode(() -> deliveryReportsDataService.retrieveByBidderCode("",
                start, end))
                .doesNotThrowAnyException();
        softAssertions.assertAll();
    }

    @Test
    public void shouldMapDeliveryReport() throws Exception {
        softAssertions.assertThat(deliveryReportsDataService.mapDeliveryReportToDeliveryReportLine(
                makeDeliveryReport(Timestamp.from(Instant.now()), loadTestData("line_item_summary/LineItemStatus3.json")))
                        .getLineItemStatus()).contains("\"lineItemSource\": \"bidder\"");
        softAssertions.assertAll();
    }

    @Test
    void shouldGetLineItemSummariesWithLineItemIds() throws Exception {
        Instant baseTime = Instant.now();
        Instant start = baseTime.minusSeconds(360);
        Instant end = baseTime.plusSeconds(360);
        Timestamp startTime = Timestamp.from(baseTime.minusSeconds(180));
        Timestamp endTime = Timestamp.from(baseTime.plusSeconds(180));
        given(configuration.getLineItemSummaryPagerSize()).willReturn(100);
        given(deliveryProgressReportsRepository.getLineItemReportsByLineIdsWithTimeRange(any(), any(), any(), any()))
                .willReturn(buildReportPage(startTime, endTime));

        List<Map<String, Object>> result = deliveryReportsDataService.getLineItemSummaryReport(
                "bidderPG-1111", start, end, LineItemSummaryReport.getMetricsFields(), 60, 0);

        verify(deliveryProgressReportsRepository, times(1))
                .getLineItemReportsByLineIdsWithTimeRange(any(), any(), any(), any());
        assertLineItemSummaries(result.get(0));
        softAssertions.assertAll();
    }

    @Test
    void shouldGetLineItemSummariesForCsvFormat() throws Exception {
        Instant baseTime = Instant.now();
        Instant start = baseTime.minusSeconds(360);
        Instant end = baseTime.plusSeconds(3600);
        Timestamp startTime = Timestamp.from(baseTime.minusSeconds(180));
        Timestamp endTime = Timestamp.from(baseTime.plusSeconds(180));

        given(configuration.getLineItemSummaryPagerSize()).willReturn(100);
        given(deliveryProgressReportsRepository.getLineItemReportsWithTimeRange(any(), any(), any()))
                .willReturn(buildReportPage(startTime, endTime));

        List<Map<String, Object>> result = deliveryReportsDataService.getLineItemSummaryReport(
                null, start, end, LineItemSummaryReport.getMetricsFields(), 60, 0);
        softAssertions.assertThat(result).hasSize(1);
        verify(deliveryProgressReportsRepository, times(1))
                .getLineItemReportsWithTimeRange(any(), any(),any());
        assertLineItemSummaries(result.get(0));
        softAssertions.assertAll();
    }

    @Test
    void shouldGetLineItemSummariesForCsvFormatWithEmptyInterval() throws Exception {
        Instant baseTime = Instant.now();
        Instant start = baseTime.minusSeconds(180);
        Instant end = baseTime.plusSeconds(3600);
        Timestamp startTime = Timestamp.from(baseTime.minusSeconds(180));
        Timestamp endTime = Timestamp.from(baseTime.plusSeconds(180));

        given(configuration.getLineItemSummaryPagerSize()).willReturn(100);
        given(deliveryProgressReportsRepository.getLineItemReportsWithTimeRange(any(), any(), any()))
                .willReturn(buildReportPage(startTime, endTime));

        List<Map<String, Object>> summaryList = deliveryReportsDataService.getLineItemSummaryReport(
                null, start, end, LineItemSummaryReport.getMetricsFields(), 2, 0);
        verify(deliveryProgressReportsRepository, times(1))
                .getLineItemReportsWithTimeRange(any(), any(),any());

        softAssertions.assertThat(summaryList.get(0).size()).isEqualTo(1);
        softAssertions.assertThat(summaryList.get(0).get(LineItemSummaryReport.INTERVAL)).isEqualTo(0);
        assertLineItemSummaries(summaryList.get(3));
        softAssertions.assertAll();
    }

    private String loadTestData(String path) throws Exception {
        File resource1 = new ClassPathResource(path).getFile();
        return new String(Files.readAllBytes(resource1.toPath()));
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

    private JsonNode makeLineItemStatusDto(Integer lineId) throws Exception {
        return objectMapper.readTree(
                String.format(
                        "{\"%s\":\"%s\",\"%s\":\"%s\"}",
                        "lineItemSource", "bidder", "lineItemId", "PG-" + lineId
                )
        );
    }

    private DeliveryReport makeDeliveryReport(Timestamp now, String lineItemStatus) throws Exception {
        DeliveryReport deliveryReport = new DeliveryReport();
        deliveryReport.setReportTimestamp(now);
        deliveryReport.setLineItemStatus(lineItemStatus);
        deliveryReport.setLineItemId("lineItemId");
        deliveryReport.setBidderCode("bidderCode");
        deliveryReport.setExtLineItemId("extLineItemId");
        deliveryReport.setDataWindowEndTimestamp(now);
        deliveryReport.setDataWindowStartTimestamp(now);
        deliveryReport.setInstanceId("instance");
        deliveryReport.setRegion("region");
        deliveryReport.setReportId("reportId");
        deliveryReport.setVendor("vendor");
        deliveryReport.setClientAuctions(1);
        return deliveryReport;
    }

    private DeliveryReportLineStatsDto makeDeliveryStats(String bidderCode, String lineItemId) {
        Timestamp now = Timestamp.from(Instant.now());
        return DeliveryReportLineStatsDto.builder()
                .bidderCode(bidderCode)
                .lineItemId(lineItemId)
                .clientAuctions(0)
                .accountAuctions(1)
                .sentToBidder(2)
                .sentToClient(3)
                .sentToClientAsTopMatch(5)
                .winEvents(7)
                .reportTimestamp(now)
                .dataWindowStartTimestamp(now)
                .dataWindowEndTimestamp(now)
                .build();
    }

    private Map<String, Object> buildExpectedSummary() {
        Map<String, Object> expectedSummary = new HashMap<>();
        expectedSummary.put(LineItemSummaryReport.ACCOUNT_AUCTIONS, 900);
        expectedSummary.put(LineItemSummaryReport.DOMAIN_MATCHED, 900);
        expectedSummary.put(LineItemSummaryReport.TARGET_MATCHED, 700);
        expectedSummary.put(LineItemSummaryReport.TARGET_MATCHED_BUT_FCAPPED, 3);
        expectedSummary.put(LineItemSummaryReport.TARGET_MATCHED_BUT_FCAP_LOOKUP_FAILED, 1);
        expectedSummary.put(LineItemSummaryReport.SENT_TO_BIDDER, 600);
        expectedSummary.put(LineItemSummaryReport.SENT_TO_BIDDER_AS_TOP_MATCH, 500);
        expectedSummary.put(LineItemSummaryReport.RECEIVED_FROM_BIDDER, 700);
        expectedSummary.put(LineItemSummaryReport.RECEIVED_FROM_BIDDER_INVALIDATED, 2);
        expectedSummary.put(LineItemSummaryReport.SENT_TO_CLIENT, 700);
        expectedSummary.put(LineItemSummaryReport.SENT_TO_CLIENT_AS_TOP_MATCH, 600);
        expectedSummary.put(LineItemSummaryReport.PACING_DEFERRED, 9);
        expectedSummary.put(LineItemSummaryReport.LINE_ITEM_ID, "bidderPG-1111");
        expectedSummary.put(LineItemSummaryReport.EXT_LINE_ITEM_ID, "1111");
        expectedSummary.put(LineItemSummaryReport.WIN_EVENTS, 30);
        return expectedSummary;
    }

    private void assertLineItemSummaries(Map<String, Object> resultSummary) {
        Map<String, Object> expectedSummary = buildExpectedSummary();
        for (String key : expectedSummary.keySet()) {
            softAssertions.assertThat(resultSummary.get(key)).isEqualTo(expectedSummary.get(key));
        }
    }

}
