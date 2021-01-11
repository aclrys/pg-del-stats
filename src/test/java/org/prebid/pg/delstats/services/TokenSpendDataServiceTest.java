package org.prebid.pg.delstats.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.pg.delstats.alerts.AlertName;
import org.prebid.pg.delstats.alerts.AlertPriority;
import org.prebid.pg.delstats.alerts.AlertProxyHttpClient;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.exception.DeliveryReportProcessingException;
import org.prebid.pg.delstats.metrics.GraphiteMetricsRecorder;
import org.prebid.pg.delstats.model.dto.Shutdown;
import org.prebid.pg.delstats.model.dto.Tracer;
import org.prebid.pg.delstats.persistence.LatestTokenSpendSummary;
import org.prebid.pg.delstats.repository.DeliveryProgressReportsRepository;
import org.prebid.pg.delstats.repository.LatestTokenSpendSummaryRepository;
import org.prebid.pg.delstats.repository.SystemStateRepository;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenSpendDataServiceTest {
    private SoftAssertions softAssertions;

    @Mock
    private LatestTokenSpendSummaryRepository latestTokenSpendSummaryRepository;

    @Mock
    private DeliveryProgressReportsRepository deliveryProgressReportsRepository;

    @Mock
    private SystemStateRepository systemStateRepository;

    @Mock
    private ServerConfiguration configuration;

    @Mock
    private SystemService systemService;

    @Mock
    private GraphiteMetricsRecorder graphiteMetricsRecorder;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Tracer tracer;

    @Mock
    private Shutdown shutdown;

    @Mock
    private AlertProxyHttpClient alertProxyHttpClient;

    private TokenSpendDataService tokenSpendDataService;

    private ObjectMapper om;

    private List<JsonNode> lineItemStatusDtos;
    private List<JsonNode> lineItemStatusEmptyDeliveryScheduleDtos;

    private String lineItemStatusArrayAsJsonString;
    private String lineItemStatusEmptyDeliveryScheduleArrayAsJsonString;

    private Instant now;
    private Timestamp nowT;
    private Timestamp thenT5;
    private Timestamp thenT1;

    @BeforeEach
    void setUpBeforeEach() throws Exception {
        softAssertions = new SoftAssertions();

        when(systemService.getRecorder()).thenReturn(graphiteMetricsRecorder);
        when(systemService.getObjectMapper()).thenReturn(objectMapper);
        when(systemService.getTracer()).thenReturn(tracer);
        when(systemService.getShutdown()).thenReturn(shutdown);
        when(systemService.getAlertProxyHttpClient()).thenReturn(alertProxyHttpClient);

        om = new ObjectMapper();

        ClassLoader testClassLoader = TokenSpendDataServiceTest.class.getClassLoader();
        InputStream inputStream = testClassLoader.getResourceAsStream("LineItemStatusTesting.json");

        tokenSpendDataService = new TokenSpendDataService(latestTokenSpendSummaryRepository,
                deliveryProgressReportsRepository, systemStateRepository, configuration, systemService);

        lineItemStatusDtos = om.readValue(inputStream, new TypeReference<List<JsonNode>>() {});
        lineItemStatusArrayAsJsonString = om.writeValueAsString(lineItemStatusDtos.get(0));

        inputStream = testClassLoader.getResourceAsStream("LineItemStatusEmptyDeliverySchedules.json");
        lineItemStatusEmptyDeliveryScheduleDtos = om.readValue(inputStream, new TypeReference<List<JsonNode>>() {
        });
        lineItemStatusEmptyDeliveryScheduleArrayAsJsonString = om.writeValueAsString(lineItemStatusEmptyDeliveryScheduleDtos.get(0));

        lenient().when(graphiteMetricsRecorder.getTokenSpendPerformanceTimer())
                .thenReturn(Optional.empty());
        lenient().when(graphiteMetricsRecorder.repositoryFetchTokenSpendTimer())
                .thenReturn(Optional.empty());
        lenient().when(graphiteMetricsRecorder.repositoryStoreTokenSpendTimer())
                .thenReturn(Optional.empty());
        lenient().when(configuration.getDeliveryReportsPageSize()).thenReturn(5);

        lenient().when(configuration.getDeliveryScheduleFieldName()).thenReturn("deliverySchedule");
        lenient().when(configuration.getPlanStartTimestampFieldName()).thenReturn("planStartTimeStamp");
        lenient().when(configuration.getPlanEndTimestampFieldName()).thenReturn("planExpirationTimeStamp");

        lenient().when(deliveryProgressReportsRepository.findByBidderCodeAndReportTimestampRange(anyString(), any(), any(), any()))
                .thenReturn(Page.empty());
        lenient().when(latestTokenSpendSummaryRepository.retrieveSince(any()))
                .thenReturn(new ArrayList<>());
        // TODO: this should be 'doCallRealMethod' but real issue is raiseEventAndLog should take a Logger
        lenient().doNothing().when(alertProxyHttpClient).raiseEventForExceptionAndLog(any(AlertName.class), anyString(), any(AlertPriority.class), any());
        lenient().doNothing().when(alertProxyHttpClient).raiseEventForExceptionAndLog(any(AlertName.class), anyString(), any(AlertPriority.class), any(), anyString());

        now = Instant.now();
        nowT = Timestamp.from(now);
        thenT1 = Timestamp.from(now.minus(1, ChronoUnit.MINUTES));
        thenT5 = Timestamp.from(now.minus(5, ChronoUnit.MINUTES));
    }

    @Test
    void shouldReturnNullForInvalidVendorRegionResultSet() {
        softAssertions.assertThat(tokenSpendDataService.processVendorRegionRecord(1)).isNull();
        softAssertions.assertThat(tokenSpendDataService.processVendorRegionRecord(new Object[]{})).isNull();
        softAssertions.assertThat(tokenSpendDataService.processVendorRegionRecord(new Object[]{ new Object()})).isNull();
        softAssertions.assertThat(tokenSpendDataService.processVendorRegionRecord(new Object[]{ new Object(), new Object()})).isNull();
        softAssertions.assertThat(tokenSpendDataService.processVendorRegionRecord(new Object[]{ "V", new Object()})).isNull();
        softAssertions.assertAll();
    }

    @Test
    void shouldNotThrowExceptionForValidRequestsGetTokenSpendSummary() {
        softAssertions.assertThatCode(() -> tokenSpendDataService.getTokenSpendSummary(null, null, null))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> tokenSpendDataService.getTokenSpendSummary(null, "v", "r"))
                .doesNotThrowAnyException();
        Timestamp then = new Timestamp(Instant.now().minus(1, ChronoUnit.MINUTES).getEpochSecond());
        softAssertions.assertThatCode(() -> tokenSpendDataService.getTokenSpendSummary(then.toString(), null, null))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> tokenSpendDataService.getTokenSpendSummary(then.toString(), "v", "r"))
                .doesNotThrowAnyException();
        softAssertions.assertAll();

        verify(latestTokenSpendSummaryRepository, times(2)).retrieveSince(any(Timestamp.class));
    }

    @Test
    void shouldNotThrowExceptionForAggregate() {
        List<Object> vendorRegionResults = new ArrayList<>();
        vendorRegionResults.add(new Object[] {"vendor", "region"});
        when(configuration.isAggregationEnabled()).thenReturn(true);
        when(deliveryProgressReportsRepository.getDistinctVendorRegion(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(Lists.emptyList())  // test nothing to aggregate
                .thenReturn(vendorRegionResults) // test something to aggregate and upsert works
                .thenReturn(vendorRegionResults) // test something to aggregate and upsert throws exception
                .thenThrow(new DeliveryReportProcessingException("")); // test getting vendor/regsions throws exception
        when(latestTokenSpendSummaryRepository.upsertTokenSummaries(eq("vendor"), eq("region"), any(), any()))
                .thenReturn(5)
                .thenThrow(new DataAccessResourceFailureException("")); // test getting vendor/regsions throws exception

        softAssertions.assertThatCode(() -> tokenSpendDataService.aggregate())
                .as("No records case - empty results from vendor/region query")
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> tokenSpendDataService.aggregate())
                .as( "Normal case: found records from a vendor/region pair and executed upsert")
                .doesNotThrowAnyException(); // everything ok vase
        softAssertions.assertThatCode(() -> tokenSpendDataService.aggregate())
                .as("Upsert threw an exception")
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> tokenSpendDataService.aggregate())
                .as("Vendor/Region query threw an exception")
                .doesNotThrowAnyException();
        softAssertions.assertAll();

        verify(deliveryProgressReportsRepository, times(4))
                .getDistinctVendorRegion(any(Timestamp.class), any(Timestamp.class));
        verify(systemStateRepository, times(2))
                .store(anyString(), anyString());
        verify(latestTokenSpendSummaryRepository, times(2))
                .upsertTokenSummaries(anyString(), anyString(), any(), any());
    }

    @Test
    public void shouldValidateLatestTokenSpendSummary() {
        softAssertions.assertThat(TokenSpendDataService.hasRequiredFields(
                LatestTokenSpendSummary.builder().vendor("vendor").build()))
                .isFalse();
        softAssertions.assertThat(TokenSpendDataService.hasRequiredFields(
                LatestTokenSpendSummary.builder().vendor("vendor").region("region").build()))
                .isFalse();
        softAssertions.assertThat(TokenSpendDataService.hasRequiredFields(
                LatestTokenSpendSummary.builder().vendor("vendor").region("region").instanceId("instance").build()))
                .isFalse();
        softAssertions.assertThat(TokenSpendDataService.hasRequiredFields(
                LatestTokenSpendSummary.builder().vendor("vendor").region("region").instanceId("instance")
                        .bidderCode("bidder").build()))
                .isFalse();
        softAssertions.assertThat(TokenSpendDataService.hasRequiredFields(
                LatestTokenSpendSummary.builder().vendor("vendor").region("region").instanceId("instance")
                        .bidderCode("bidder").lineItemId("lineItemId").build()))
                .isTrue();
        softAssertions.assertThat(TokenSpendDataService.hasRequiredFields(
                LatestTokenSpendSummary.builder().region("region").instanceId("instance")
                        .bidderCode("bidder").lineItemId("lineItemId").build()))
                .isFalse();
        softAssertions.assertAll();
    }

    @Test
    public void shouldNotGetExceptionWhenProcessingInvalidSummaryData() {
        when(latestTokenSpendSummaryRepository.retrieveSince(eq(nowT))).thenReturn(Lists.list(null));

        softAssertions.assertThatCode(() -> tokenSpendDataService.doGetTokenSpendSummaryDto(nowT, null, null))
                .doesNotThrowAnyException();
        softAssertions.assertAll();
    }

    @Test
    public void shouldNotContinueWithAggregateWhenDisabled() {
        // Force an exception when first mocked object called
        when(configuration.isAggregationEnabled()).thenReturn(Boolean.FALSE);

        softAssertions.assertThatCode(() -> tokenSpendDataService.aggregate()).doesNotThrowAnyException();
        softAssertions.assertAll();
    }

    @Test
    public void shouldMapTokenSpendSummaryLineDtoFromLatestTokenSpendSummary() {
        Timestamp now = Timestamp.from(Instant.now());
        LatestTokenSpendSummary latestTokenSpendSummaryValidJson = makeLatestTokenSpendSummary(now, "{}");
        LatestTokenSpendSummary latestTokenSpendSummaryInValidJson = makeLatestTokenSpendSummary(now, "{x]");

        softAssertions.assertThatCode(() -> tokenSpendDataService
                .mapTokenSpendSummaryLineDtoFromLatestTokenSpendSummary(latestTokenSpendSummaryValidJson))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> tokenSpendDataService
                .mapTokenSpendSummaryLineDtoFromLatestTokenSpendSummary(latestTokenSpendSummaryInValidJson))
                .doesNotThrowAnyException();
        softAssertions.assertAll();
    }

    private LatestTokenSpendSummary makeLatestTokenSpendSummary(Timestamp now, String summaryJson) {
        return LatestTokenSpendSummary.builder()
                .vendor("Vendor")
                .region("Region")
                .instanceId("InstanceId")
                .serviceInstanceId("ServiceInstanceId")
                .bidderCode("BidderCode")
                .lineItemId("LineItemId")
                .extLineItemId("ExtLineItemId")
                .dataWindowStartTimestamp(now)
                .dataWindowEndTimestamp(now)
                .reportTimestamp(now)
                .summaryData(summaryJson)
                .build();
    }
}
