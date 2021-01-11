package org.prebid.pg.delstats.services;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.model.dto.Shutdown;
import org.prebid.pg.delstats.persistence.SystemState;
import org.prebid.pg.delstats.repository.SystemStateRepository;
import org.prebid.pg.delstats.utils.MockSystemService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryReportSummaryServiceSchedulerTest {
    SoftAssertions softAssertions;

    @Mock
    DeliveryReportSummaryService deliveryReportSummaryService;

    @Mock
    SystemStateRepository systemStateRepository;

    @Mock
    ServerConfiguration serverConfiguration;

    @Mock
    Shutdown shutdown;

    DeliveryReportSummaryServiceScheduler deliveryReportSummaryServiceScheduler;

    @BeforeEach
    public void init() {
        softAssertions = new SoftAssertions();

        when(serverConfiguration.getDeliverySummaryServiceAggregateInterval()).thenReturn(5);

        deliveryReportSummaryServiceScheduler = new DeliveryReportSummaryServiceScheduler(deliveryReportSummaryService, systemStateRepository, serverConfiguration, new MockSystemService(shutdown));
    }

    @Test
    void shouldProvideProperFirstStartTime() {
        Clock clock = Clock.tick(Clock.systemDefaultZone(), Duration.of(5, ChronoUnit.MINUTES));

        int minback = serverConfiguration.getDeliverySummaryServiceAggregateInterval();

        when(serverConfiguration.getDeliverySummaryServiceMaxAggregateIntervals()).thenReturn(3L);

        Instant now = clock.instant();
        softAssertions.assertThat(deliveryReportSummaryServiceScheduler.startTime(now, Instant.EPOCH.toString()))
                .matches(instant -> instant.getEpochSecond() % (5 * 60) == 0)
                .isBefore(now);
        softAssertions.assertThat(deliveryReportSummaryServiceScheduler.startTime(now, now.minus(
                    serverConfiguration.getDeliverySummaryServiceAggregateInterval()-1, ChronoUnit.MINUTES).toString()))
                .matches(instant -> instant.getEpochSecond() % (5 * 60) == 0)
                .isEqualTo(now.minus(serverConfiguration.getDeliverySummaryServiceAggregateInterval(), ChronoUnit.MINUTES))
                .isAfter(now.minus(minback+1, ChronoUnit.MINUTES));
        softAssertions.assertThat(deliveryReportSummaryServiceScheduler.startTime(
                now.plus(serverConfiguration.getDeliverySummaryServiceAggregateInterval(), ChronoUnit.MINUTES), now.toString()))
                .matches(instant -> instant.getEpochSecond() % (5 * 60) == 0)
                .isEqualTo(now)
                .isAfter(now.minus(minback, ChronoUnit.MINUTES));
        softAssertions.assertAll();
    }

    @Test
    void shouldNotRunDeliveryReportSummaryJobIfServiceDisabled() {
        when(shutdown.isInitiating()).thenReturn(Boolean.FALSE);
        when(serverConfiguration.isDeliverySummaryServiceEnabled()).thenReturn(false);
        softAssertions.assertThatCode(() -> deliveryReportSummaryServiceScheduler.runDeliveryReportSummaryJob()).doesNotThrowAnyException();
        softAssertions.assertAll();
        verify(deliveryReportSummaryService, times(0)).runDeliveryReportSummary(any(), any(), anyBoolean());
    }

    @Test
    void shouldNotRunDeliveryReportSummaryJobIfShutdown() {
        when(shutdown.isInitiating()).thenReturn(Boolean.TRUE);
        deliveryReportSummaryServiceScheduler.runDeliveryReportSummaryJob();
        verify(deliveryReportSummaryService, times(0)).runDeliveryReportSummary(any(), any(), anyBoolean());
    }

    @Test
    void shouldNotRunDeliveryReportSummaryJobIfInWaitingPeriod() {
        when(shutdown.isInitiating()).thenReturn(Boolean.FALSE);
        when(serverConfiguration.isDeliverySummaryServiceEnabled()).thenReturn(true);
        Clock mockClock = Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(-2));
        deliveryReportSummaryServiceScheduler.setClock(mockClock);

        Instant now = Instant.now();
        deliveryReportSummaryServiceScheduler.runDeliveryReportSummaryJob();
        verify(deliveryReportSummaryService, times(0)).runDeliveryReportSummary(any(), any(), anyBoolean());
    }

    @Test
    void shouldRunDeliveryReportSummaryJobIfServiceEnabled() {
        when(shutdown.isInitiating()).thenReturn(Boolean.FALSE);
        when(serverConfiguration.isDeliverySummaryServiceEnabled()).thenReturn(true);
        when(serverConfiguration.getDeliverySummaryServiceMaxAggregateIntervals()).thenReturn(3L);

        Clock mockClock = Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(60*60+20));
        deliveryReportSummaryServiceScheduler.setClock(mockClock);
        when(systemStateRepository.retrieveByTag(anyString())).thenReturn(SystemState.builder().val(Instant.now().minusSeconds(60*60).toString()).build());
        softAssertions.assertThatCode(() -> deliveryReportSummaryServiceScheduler.runDeliveryReportSummaryJob()).doesNotThrowAnyException();
        softAssertions.assertAll();
        verify(deliveryReportSummaryService, atLeast(1)).runDeliveryReportSummary(any(), any(), anyBoolean());
    }
}