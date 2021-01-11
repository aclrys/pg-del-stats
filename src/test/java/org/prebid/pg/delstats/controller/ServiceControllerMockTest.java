package org.prebid.pg.delstats.controller;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.pg.delstats.config.ApplicationConfiguration.CsvMapperFactory;
import org.prebid.pg.delstats.config.DeploymentConfiguration;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.exception.DeliveryReportProcessingException;
import org.prebid.pg.delstats.exception.DeliveryReportValidationException;
import org.prebid.pg.delstats.model.dto.DeliveryReportFromPbsDto;
import org.prebid.pg.delstats.model.dto.DeliveryReportToPlannerAdapterDto;
import org.prebid.pg.delstats.model.dto.TokenSpendSummaryDto;
import org.prebid.pg.delstats.services.DeliveryReportSummaryService;
import org.prebid.pg.delstats.services.DeliveryReportsDataService;
import org.prebid.pg.delstats.services.TokenSpendDataService;
import org.prebid.pg.delstats.utils.MockSystemService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ServiceControllerMockTest {

    private static final String TEST_BIDDER_CODE = "bidder";
    private static final String TEST_START_TIME = "2019-01-01 00:00:00";
    private static final String TEST_END_TIME = "2019-09-01 00:00:00";
    private static final String TEST_SINCE_TIME = "2019-01-01 00:00:00";
    private static final String TEST_LINE_ITEM_IDS = "foo,bar";

    SoftAssertions softAssertions;

    @Mock
    private TokenSpendDataService tokenSpendDataService;

    @Mock
    private DeliveryReportsDataService deliveryReportsDataService;

    @Mock
    private DeliveryReportSummaryService deliverySummaryService;

    @Mock
    private CsvMapperFactory csvMapperFactory;

    @Mock
    private ServerConfiguration serverConfiguration;

    @Mock
    private DeploymentConfiguration deploymentConfiguration;

    private ServiceController serviceController;

    @BeforeEach
    public void setup() {
        softAssertions = new SoftAssertions();

        lenient().when(csvMapperFactory.getCsvMapper()).thenReturn(new CsvMapper());

        serviceController = new ServiceController(tokenSpendDataService, deliveryReportsDataService,
                deliverySummaryService, csvMapperFactory, serverConfiguration, deploymentConfiguration,
                new MockSystemService());
    }

    @Test
    public void shouldCallDeliveryReportDataService() {
        Authentication authentication = mock(Authentication.class);
        UserDetails userValid = User.builder().username("mock").password("mock").authorities(TEST_BIDDER_CODE).build();
        UserDetails userInvalid = User.builder().username("mock").password("mock").authorities("bad").build();

        when(authentication.getPrincipal()).thenReturn(userValid, userValid, userInvalid, userValid, userValid, userInvalid);

        when(deliveryReportsDataService.retrieveByBidderCode(anyString(), anyString(), anyString()))
                .thenReturn(DeliveryReportToPlannerAdapterDto.builder().deliveryReports(Collections.emptyList()).build());
        DeliveryReportFromPbsDto deliveryReportFromPbsDto = makeReportBuider()
                        .reportId(UUID.randomUUID().toString())
                        .vendor("bidder")
                        .region("us-east-1")
                        .instanceId("instance")
                        .reportTimeStamp(Timestamp.valueOf(TEST_END_TIME))
                        .dataWindowStartTimeStamp(Timestamp.valueOf(TEST_START_TIME))
                        .dataWindowEndTimeStamp(Timestamp.valueOf(TEST_END_TIME))
                        .build();

        given(serverConfiguration.isPaApiEnabled()).willReturn(true);

        softAssertions.assertThatCode(() -> serviceController.storeReport(deliveryReportFromPbsDto))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> serviceController.storeReport(null))
                .isExactlyInstanceOf(DeliveryReportValidationException.class);

        softAssertions.assertThatCode(() -> serviceController.getDeliveryReports(authentication, TEST_BIDDER_CODE, TEST_START_TIME, TEST_END_TIME))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> serviceController.getDeliveryReports(authentication, TEST_BIDDER_CODE, "", ""))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> serviceController.getDeliveryReports(authentication, TEST_BIDDER_CODE, "", ""))
                .isInstanceOf(DeliveryReportValidationException.class);


        softAssertions.assertThatCode(() -> serviceController.getDeliveryReportsV2(authentication, TEST_BIDDER_CODE, TEST_START_TIME, TEST_END_TIME))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> serviceController.getDeliveryReportsV2(authentication, TEST_BIDDER_CODE, "", ""))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> serviceController.getDeliveryReportsV2(authentication, TEST_BIDDER_CODE, "", ""))
                .isInstanceOf(DeliveryReportValidationException.class);
        softAssertions.assertAll();

        verify(deliveryReportsDataService, times(1))
                .storeReport(isNotNull());

        verify(deliveryReportsDataService, times(1))
                .retrieveByBidderCode(eq(TEST_BIDDER_CODE), eq(TEST_START_TIME), eq(TEST_END_TIME));
        verify(deliveryReportsDataService, times(1))
                .retrieveByBidderCode(eq(TEST_BIDDER_CODE), eq(""), eq(""));
    }

    @Test
    public void shouldCallTokenSpendDataService() {
        TokenSpendSummaryDto tokenSpendSummaryDto = TokenSpendSummaryDto.builder()
                .tokenSpendSummaryLines(Lists.emptyList())
                .build();
        when(tokenSpendDataService.getTokenSpendSummary(anyString(), isNull(), isNull())).thenReturn(tokenSpendSummaryDto);
        when(tokenSpendDataService.getTokenSpendSummary(isNull(), isNull(), isNull())).thenReturn(tokenSpendSummaryDto);

        given(serverConfiguration.isTokenSpendApiEnabled()).willReturn(true);

        softAssertions.assertThatCode(() -> serviceController.getTokenSpendReports(TEST_SINCE_TIME, null ,null))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> serviceController.getTokenSpendReports("", null, null))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> serviceController.getTokenSpendReports(null, null, null))
                .doesNotThrowAnyException();

        softAssertions.assertAll();

        verify(tokenSpendDataService, times(1))
                .getTokenSpendSummary(eq(TEST_SINCE_TIME), isNull(), isNull());
        verify(tokenSpendDataService, times(1))
                .getTokenSpendSummary(eq(""), isNull(), isNull());
        verify(tokenSpendDataService, times(1))
                .getTokenSpendSummary(isNull(), isNull(), isNull());

    }

    @Test
    public void shouldReturnProcessingExceptionWhenTokenSpendSummaryThrowsException() {
        given(serverConfiguration.isTokenSpendApiEnabled()).willReturn(true);
        when(tokenSpendDataService.getTokenSpendSummary(eq("oops"), isNull(), isNull()))
                .thenThrow(new RuntimeException("oops"));

        softAssertions.assertThatCode(() -> serviceController.getTokenSpendReports("oops", null, null))
                .isExactlyInstanceOf(DeliveryReportProcessingException.class);

        softAssertions.assertAll();
    }

    @Test
    void shouldCallGetLineItemSummaryReportFromGetLineItemSummaries() {
        String metrics = "sentToBidder,sentToClient";
        int interval = 60;
        String startTime = "2020-01-01T01:00:00.000Z";
        Set<String> metricsSet = new LinkedHashSet<>(Arrays.asList("sentToBidder", "sentToClient"));

        given(serverConfiguration.getLineItemSummaryMaxTimeRangeSeconds()).willReturn(86400);
        given(serverConfiguration.isSumApiEnabled()).willReturn(true);

        CsvMapper mockCvsMapper = mock(CsvMapper.class);
        ObjectWriter mockObjectWriter = mock(ObjectWriter.class);
        when(csvMapperFactory.getCsvMapper()).thenReturn(mockCvsMapper);
        when(mockCvsMapper.writerFor(eq(List.class))).thenReturn(mockObjectWriter);
        when(mockObjectWriter.with(any(CsvSchema.class))).thenReturn(mockObjectWriter);

        softAssertions.assertThatCode(() -> serviceController.getLineItemSummary(
                        TEST_LINE_ITEM_IDS, startTime, metrics))
                .doesNotThrowAnyException();
        softAssertions.assertAll();
        verify(deliverySummaryService, times(1))
                .getLineItemSummaryReport(
                        eq(TEST_LINE_ITEM_IDS),
                        eq(Instant.parse(startTime)),
                        any(),
                        eq(metricsSet),
                        eq(interval)
                );
    }

    @Test
    public void shouldGetExceptionWhenMetadataInvalid() {
        Timestamp now = Timestamp.from(Instant.now());
        when(serverConfiguration.isValidationEnabled()).thenReturn(false).thenReturn(true);
        DeliveryReportFromPbsDto deliveryReportFromPbsDto1 = makeReportBuider().build();
        DeliveryReportFromPbsDto deliveryReportFromPbsDto2 = makeReportBuider()
                .reportId("test").build();
        DeliveryReportFromPbsDto deliveryReportFromPbsDto3 = makeReportBuider()
                .reportId("test").instanceId("test").build();
        DeliveryReportFromPbsDto deliveryReportFromPbsDto4 = makeReportBuider()
                .reportId("test").instanceId("test").vendor("test").build();
        DeliveryReportFromPbsDto deliveryReportFromPbsDto5 = makeReportBuider()
                .reportId("test").instanceId("test").vendor("test").region("test").build();
        DeliveryReportFromPbsDto deliveryReportFromPbsDto6 = makeReportBuider()
                .reportId("test").instanceId("test").vendor("test").region("test")
                .reportTimeStamp(Timestamp.from(Instant.now())).build();
        DeliveryReportFromPbsDto deliveryReportFromPbsDto7 = makeReportBuider()
                .reportId("test").instanceId("test").vendor("test").region("test")
                .reportTimeStamp(Timestamp.from(Instant.now()))
                .dataWindowStartTimeStamp(Timestamp.from(Instant.now())).build();
        DeliveryReportFromPbsDto deliveryReportFromPbsDto8 = makeReportBuider()
                .reportId("test").instanceId("test").vendor("test").region("test")
                .reportTimeStamp(now)
                .dataWindowStartTimeStamp(Timestamp.from(Instant.now().plusSeconds(5000)))
                .dataWindowEndTimeStamp(now).build();
        DeliveryReportFromPbsDto deliveryReportFromPbsDto9 = makeReportBuider()
                .reportId("test").instanceId("test").vendor("test").region("test")
                .reportTimeStamp(now)
                .dataWindowStartTimeStamp(now)
                .dataWindowEndTimeStamp(now).build();
        DeliveryReportFromPbsDto validDeliveryReportFromPbsDto = makeReportBuider()
                .reportId("test").instanceId("test").vendor("test").region("test")
                .reportTimeStamp(now)
                .dataWindowStartTimeStamp(Timestamp.from(Instant.now().minusSeconds(5)))
                .dataWindowEndTimeStamp(now).build();

        softAssertions.assertThatCode(() -> serviceController.storeReport(deliveryReportFromPbsDto1))
                .as("Never throw an exception if validation disabled.")
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> serviceController.storeReport(deliveryReportFromPbsDto1))
                .as("Missing all fields, but first report id")
                .isExactlyInstanceOf(DeliveryReportValidationException.class);
        softAssertions.assertThatCode(() -> serviceController.storeReport(deliveryReportFromPbsDto2))
                .as("Missing many fields, but now instance id")
                .isExactlyInstanceOf(DeliveryReportValidationException.class);
        softAssertions.assertThatCode(() -> serviceController.storeReport(deliveryReportFromPbsDto3))
                .as("Missing many fields, but now vendor id")
                .isExactlyInstanceOf(DeliveryReportValidationException.class);
        softAssertions.assertThatCode(() -> serviceController.storeReport(deliveryReportFromPbsDto4))
                .as("Missing many fields, but now region id")
                .isExactlyInstanceOf(DeliveryReportValidationException.class);
        softAssertions.assertThatCode(() -> serviceController.storeReport(deliveryReportFromPbsDto5))
                .as("Missing many fields, but now report timestamp")
                .isExactlyInstanceOf(DeliveryReportValidationException.class);
        softAssertions.assertThatCode(() -> serviceController.storeReport(deliveryReportFromPbsDto6))
                .as("Missing all fields, but first data start window")
                .isExactlyInstanceOf(DeliveryReportValidationException.class);
        softAssertions.assertThatCode(() -> serviceController.storeReport(deliveryReportFromPbsDto7))
                .as("Missing all fields, but first data end window")
                .isExactlyInstanceOf(DeliveryReportValidationException.class);
        softAssertions.assertThatCode(() -> serviceController.storeReport(deliveryReportFromPbsDto8))
                .as("All meta data fields, but first data end window before start")
                .isExactlyInstanceOf(DeliveryReportValidationException.class);
        softAssertions.assertThatCode(() -> serviceController.storeReport(deliveryReportFromPbsDto9))
                .as("All meta data fields, but first data end window equals start")
                .isExactlyInstanceOf(DeliveryReportValidationException.class);
        softAssertions.assertThatCode(() -> serviceController.storeReport(validDeliveryReportFromPbsDto))
                .as("All meta data fields and valid")
                .doesNotThrowAnyException();
        softAssertions.assertAll();

    }

    DeliveryReportFromPbsDto.DeliveryReportFromPbsDtoBuilder makeReportBuider() {
        return DeliveryReportFromPbsDto.builder().lineItemStatus(Collections.emptyList());
    }
}
