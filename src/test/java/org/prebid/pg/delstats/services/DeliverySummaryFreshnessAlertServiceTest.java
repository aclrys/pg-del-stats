package org.prebid.pg.delstats.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.pg.delstats.alerts.AlertProxyHttpClient;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.model.dto.Shutdown;
import org.prebid.pg.delstats.persistence.SystemState;
import org.prebid.pg.delstats.repository.SystemStateConstants;
import org.prebid.pg.delstats.repository.SystemStateRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DeliverySummaryFreshnessAlertServiceTest {

    @Mock
    private SystemStateRepository systemStateRepoMock;

    @Mock
    private SystemService systemServiceMock;

    @Mock
    private AlertProxyHttpClient alertClientMock;

    private DeliverySummaryFreshnessAlertService alertService;

    private ServerConfiguration serverConfig;

    private Shutdown shutdown;

    @BeforeEach
    void setup() {
        serverConfig = new ServerConfiguration();
        serverConfig.setDeliverySummaryFreshnessAlertEnabled(true);
        serverConfig.setDeliverySummaryServiceAggregateInterval(5);
        serverConfig.setDeliverySummaryServiceMaxAggregateIntervals(2);
        shutdown = new Shutdown();
        given(systemServiceMock.getShutdown()).willReturn(shutdown);
        alertService = new DeliverySummaryFreshnessAlertService(
                systemStateRepoMock, serverConfig, systemServiceMock, alertClientMock);
    }

    @Test
    void shouldNotCheckFreshnessIfDisabled() {
        shutdown.setInitiating(false);
        serverConfig.setDeliverySummaryFreshnessAlertEnabled(false);
        alertService.checkFreshness();
        verify(systemStateRepoMock, times(0)).retrieveByTag(any());
    }

    @Test
    void shouldNotCheckFreshnessIfShutdown() {
        shutdown.setInitiating(true);
        alertService.checkFreshness();
        verify(systemStateRepoMock, times(0)).retrieveByTag(any());
    }

    @Test
    void shouldCheckFreshnessFireAlertIfLatestReportNotRun() {
        shutdown.setInitiating(false);
        Instant now = Instant.now();
        SystemState state = SystemState.builder()
                .tag(SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY)
                .val(now.minus(30, ChronoUnit.MINUTES).toString())
                .build();
        given(systemStateRepoMock.retrieveByTag(any())).willReturn(state);
        given(systemStateRepoMock.getLatestDeliverySummaryReportStates(any())).willReturn(Collections.emptyList());
        alertService.checkFreshness();
        verify(alertClientMock, times(1)).sendNotification(any(), any(), any());
    }

    @Test
    void shouldCheckFreshnessNotFireAlertIfLatestReportRunSuccessfully() {
        shutdown.setInitiating(false);
        Instant now = Instant.now();
        SystemState state = SystemState.builder()
                .tag(SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY)
                .val(now.minus(5, ChronoUnit.MINUTES).toString())
                .build();
        given(systemStateRepoMock.retrieveByTag(any())).willReturn(state);
        given(systemStateRepoMock.getLatestDeliverySummaryReportStates(any())).willReturn(Collections.emptyList());
        alertService.checkFreshness();
        verify(alertClientMock, times(0)).sendNotification(any(), any(), any());
    }

    @Test
    void shouldCheckFreshnessFireAlertIfMissingReportForLastHour() {
        shutdown.setInitiating(false);
        Instant now = Instant.now();
        SystemState state = SystemState.builder()
                .tag(SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY)
                .val(now.minus(5, ChronoUnit.MINUTES).toString())
                .build();
        given(systemStateRepoMock.retrieveByTag(any())).willReturn(state);
        List<SystemState> latestStates = new ArrayList<>();
        latestStates.add(SystemState.builder()
                .tag(String.format(SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY_INTERVAL_END_FORMAT, "10"))
                .val(now.minus(5, ChronoUnit.MINUTES).toString())
                .build());
        latestStates.add(SystemState.builder()
                .tag(String.format(SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY_INTERVAL_END_FORMAT, "40"))
                .val(now.minus(40, ChronoUnit.MINUTES).toString())
                .build());
        given(systemStateRepoMock.getLatestDeliverySummaryReportStates(any())).willReturn(latestStates);
        alertService.checkFreshness();
        verify(alertClientMock, times(1)).sendNotification(any(), any(), any());
    }

    @Test
    void shouldCheckFreshnessNotFireAlertIfReportForLastHourRunSuccessfully() {
        shutdown.setInitiating(false);
        Instant now = Instant.now();
        SystemState state = SystemState.builder()
                .tag(SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY)
                .val(now.minus(5, ChronoUnit.MINUTES).toString())
                .build();
        given(systemStateRepoMock.retrieveByTag(any())).willReturn(state);
        List<SystemState> latestStates = new ArrayList<>();
        latestStates.add(SystemState.builder()
                .tag(String.format(SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY_INTERVAL_END_FORMAT, "10"))
                .val(now.minus(5, ChronoUnit.MINUTES).toString())
                .build());
        latestStates.add(SystemState.builder()
                .tag(String.format(SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY_INTERVAL_END_FORMAT, "40"))
                .val(now.minus(10, ChronoUnit.MINUTES).toString())
                .build());
        given(systemStateRepoMock.getLatestDeliverySummaryReportStates(any())).willReturn(latestStates);
        alertService.checkFreshness();
        verify(alertClientMock, times(0)).sendNotification(any(), any(), any());
    }

    @Test
    void shouldCheckFreshnessFireAlertIfReportNeverRunAfterServerStart() {
        shutdown.setInitiating(false);
        Instant now = Instant.now();
        given(systemStateRepoMock.retrieveByTag(any())).willReturn(null);
        given(systemServiceMock.getStartupTime()).willReturn(now.minus(30, ChronoUnit.MINUTES));
        alertService.checkFreshness();
        verify(alertClientMock, times(1)).sendNotification(any(), any(), any());
    }

}


