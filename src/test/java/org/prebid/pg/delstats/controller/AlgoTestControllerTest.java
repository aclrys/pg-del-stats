package org.prebid.pg.delstats.controller;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.pg.delstats.config.ApplicationConfiguration.CsvMapperFactory;
import org.prebid.pg.delstats.config.DeploymentConfiguration;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.exception.SystemInitializationException;
import org.prebid.pg.delstats.model.dto.Tracer;
import org.prebid.pg.delstats.repository.DeliveryProgressReportsRepository;
import org.prebid.pg.delstats.repository.LatestTokenSpendSummaryRepository;
import org.prebid.pg.delstats.services.DeliveryReportSummaryService;
import org.prebid.pg.delstats.services.DeliveryReportsDataService;
import org.prebid.pg.delstats.services.TokenSpendDataService;

import java.sql.Timestamp;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AlgoTestControllerTest {
    private SoftAssertions softAssertions;

    @Mock
    private DeploymentConfiguration deploymentConfiguration;

    @Mock
    private TokenSpendDataService tokenSpendDataService;

    @Mock
    private LatestTokenSpendSummaryRepository latestTokenSpendSummaryRepository;

    @Mock
    private DeliveryProgressReportsRepository deliveryReportSummariesRepository;

    @Mock
    private DeliveryReportsDataService deliveryReportsDataService;

    @Mock
    private DeliveryReportSummaryService reportSummaryService;

    @Mock
    private Tracer tracer;

    @Mock
    private ServerConfiguration serverConfiguration;

    @Mock
    private CsvMapperFactory csvMapperFactory;

    @InjectMocks
    private AlgoTestController algoTestController;

    private Timestamp now;

    private String nowString;

    private String timeInputString;

    @BeforeEach
    public void setup() {
        softAssertions = new SoftAssertions();
        now = Timestamp.from(Instant.now());
        nowString = now.toString();
        timeInputString = nowString.replace(" ", "T") + "Z";
    }

    @Test
    public void shouldThrowSystemInitializationExceptionWhenStartedInNonAlgoMode() {
        when(deploymentConfiguration.getProfile())
                .thenReturn(DeploymentConfiguration.ProfileType.PROD)
                .thenReturn(DeploymentConfiguration.ProfileType.ALGOTEST);
        softAssertions.assertThatCode(() -> algoTestController.init())
                .isInstanceOf(SystemInitializationException.class);
        softAssertions.assertThatCode(() ->algoTestController.init())
                .doesNotThrowAnyException();

        softAssertions.assertAll();
    }

    @Test
    public void shouldCallStoreReport() {
        when(deploymentConfiguration.getProfile()).thenReturn(DeploymentConfiguration.ProfileType.ALGOTEST);
        softAssertions.assertThatCode(() -> algoTestController.storeReport(null, nowString))
                .doesNotThrowAnyException();

        softAssertions.assertAll();

        verify(deliveryReportsDataService, times(1)).storeReport(isNull(), eq(nowString), anyString());
    }

    @Test
    public void shouldCallAggregate() {
        when(deploymentConfiguration.getProfile()).thenReturn(DeploymentConfiguration.ProfileType.ALGOTEST);
        softAssertions.assertThatCode(() -> algoTestController.aggregate(nowString, nowString))
                .doesNotThrowAnyException();

        softAssertions.assertAll();

        verify(tokenSpendDataService, times(1)).aggregate(eq(nowString), eq(nowString), anyString(), anyString());
    }

    @Test
    public void shouldCallTokenSpendSummaryWhenGetReportCalled() {
        when(deploymentConfiguration.getProfile()).thenReturn(DeploymentConfiguration.ProfileType.ALGOTEST);
        softAssertions.assertThatCode(() -> algoTestController
                .getReport(timeInputString, null, null, timeInputString))
                .doesNotThrowAnyException();
        softAssertions.assertAll();

        verify(tokenSpendDataService, times(1)).getTokenSpendSummary(eq(""), isNull(), isNull(), eq(timeInputString));
    }

    @Test
    public void shouldThrowIllegalStateExceptionWhenStoreReportCalledInProfileProd() {
        when(deploymentConfiguration.getProfile()).thenReturn(DeploymentConfiguration.ProfileType.PROD);
        softAssertions.assertThatCode(() -> algoTestController.storeReport(null, nowString))
                .isInstanceOf(IllegalStateException.class);

        softAssertions.assertAll();

        verify(deliveryReportsDataService, never()).storeReport(isNull(), eq(nowString), anyString());
    }

    @Test
    public void shouldThrowIllegalStateExceptionWhenAggregateCalledInProfileProd() {
        when(deploymentConfiguration.getProfile()).thenReturn(DeploymentConfiguration.ProfileType.PROD);
        softAssertions.assertThatCode(() -> algoTestController.aggregate(nowString, nowString))
                .isInstanceOf(IllegalStateException.class);

        softAssertions.assertAll();

        verify(tokenSpendDataService, never()).aggregate(eq(nowString), eq(nowString), anyString(), anyString());
    }

    @Test
    public void shouldCallSummaryForV2GetReports() {
        when(deploymentConfiguration.getProfile())
                .thenReturn(DeploymentConfiguration.ProfileType.PROD)
                .thenReturn(DeploymentConfiguration.ProfileType.ALGOTEST);
        softAssertions.assertThatCode(() -> algoTestController.getDeliveryReportsV2(null, "", nowString, nowString, nowString))
                .isInstanceOf(IllegalStateException.class);
        softAssertions.assertThatCode(() -> algoTestController.getDeliveryReportsV2(null, "", nowString, nowString, nowString))
                .doesNotThrowAnyException();
        softAssertions.assertAll();
        verify(reportSummaryService, times(1))
                .getDeliverySummaryReport(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void shouldThrowIllegalStateExceptionWhenCleanupCalledInProfileProd() {
        when(deploymentConfiguration.getProfile()).thenReturn(DeploymentConfiguration.ProfileType.PROD);
        softAssertions.assertThatCode(() -> algoTestController.cleanup())
                .isInstanceOf(IllegalStateException.class);

        softAssertions.assertAll();

        verify(latestTokenSpendSummaryRepository, never()).deleteAll();
        verify(deliveryReportSummariesRepository, never()).deleteAll();
    }

    @Test
    public void shouldCallCleanupWhenCleanupCalledInProfileAlgo() {
        when(deploymentConfiguration.getProfile()).thenReturn(DeploymentConfiguration.ProfileType.ALGOTEST);
        softAssertions.assertThatCode(() -> algoTestController.cleanup())
                .doesNotThrowAnyException();
        softAssertions.assertAll();

        verify(latestTokenSpendSummaryRepository, times(1)).deleteAll();
        verify(deliveryReportSummariesRepository, times(1)).deleteAll();
    }

    @Test
    public void shouldCallReportSummaryServiceWhenAggregateLineItemSummaryCalled() {
        softAssertions.assertThatCode(() -> algoTestController
                .aggregateLineItemSummary(timeInputString, timeInputString, timeInputString))
                .doesNotThrowAnyException();
        softAssertions.assertAll();

        verify(reportSummaryService, times(1))
                .runDeliveryReportSummary(any(), any(), anyBoolean());
    }

    @Test
    public void shouldCallReportDataServiceWhenGetLineItemSummaryCalled() {
        Integer interval = 1;
        algoTestController.overrideCsvMapper(new CsvMapper());
        softAssertions.assertThatCode(() -> algoTestController
                .getLineItemSummary("1", timeInputString, timeInputString, null, interval ))
                .doesNotThrowAnyException();
        softAssertions.assertAll();

        verify(deliveryReportsDataService, times(1))
                .getLineItemSummaryReport(eq("1"), any(), any(), any(), eq(interval), any());
    }
}
