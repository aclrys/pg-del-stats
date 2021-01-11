package org.prebid.pg.delstats.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.exception.ApiNotActiveException;
import org.prebid.pg.delstats.exception.InvalidRequestException;
import org.prebid.pg.delstats.metrics.GraphiteMetricsRecorder;
import org.prebid.pg.delstats.model.dto.FreshnessStates;
import org.prebid.pg.delstats.model.dto.Shutdown;
import org.prebid.pg.delstats.services.DeliveryReportSummaryService;
import org.prebid.pg.delstats.services.SystemStateService;
import org.prebid.pg.delstats.utils.MockSystemService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DeliverySummaryAdminControllerTest {

    private static final int TIME_RANGE = 5;

    @Mock
    private ServerConfiguration serverConfiguration;

    @Mock
    private SystemStateService systemStateServiceMock;

    @Mock
    private DeliveryReportSummaryService deliveryReportSummaryServiceMock;

    @Mock
    private GraphiteMetricsRecorder recorderMock;

    private DeliverySummaryAdminController deliverySummaryAdminController;

    private Shutdown shutdown;

    @BeforeEach
    public void setup() {
        shutdown = new Shutdown();
        this.deliverySummaryAdminController = new DeliverySummaryAdminController(
                deliveryReportSummaryServiceMock,
                serverConfiguration,
                new MockSystemService(shutdown),
                systemStateServiceMock,
                recorderMock);
    }

    @Test
    void shouldGetFreshness() {
        given(serverConfiguration.isDeliverySummaryFreshnessApiEnabled()).willReturn(true);
        given(systemStateServiceMock.getFreshnessStates()).willReturn(FreshnessStates.builder().build());
        deliverySummaryAdminController.getFreshness();
        verify(systemStateServiceMock, times(1)).getFreshnessStates();
    }

    @Test
    void shouldGetFreshnessThrowsExceptionIfApiNotEnabled() {
        given(serverConfiguration.isDeliverySummaryFreshnessApiEnabled()).willReturn(false);
        assertThrows(ApiNotActiveException.class, () -> {
            deliverySummaryAdminController.getFreshness();
        });
    }

    @Test
    void shouldGetFreshnessThrowsExceptionIfShuttingDown() {
        given(serverConfiguration.isDeliverySummaryFreshnessApiEnabled()).willReturn(true);
        shutdown.setInitiating(true);
        assertThrows(IllegalStateException.class, () -> {
            deliverySummaryAdminController.getFreshness();
        });
    }

    @Test
    void shouldRecreateLineItemSummaryReport() {
        Instant endTime = Instant.now().truncatedTo(ChronoUnit.HOURS);
        Instant startTime = endTime.minus(TIME_RANGE, ChronoUnit.MINUTES);
        given(serverConfiguration.isRecreateLineItemSummaryApiEnabled()).willReturn(true);
        given(serverConfiguration.getDeliverySummaryServiceAggregateInterval()).willReturn(TIME_RANGE);
        given(serverConfiguration.getRecreateLineItemSummaryApiMaxLookBackInDays()).willReturn(7);
        deliverySummaryAdminController.recreateLineItemSummaryReport(startTime.toString(), endTime.toString(), false);
        verify(deliveryReportSummaryServiceMock, times(1))
                .recreateLineItemSummaryReport(any(), any(), anyBoolean());
    }

    @Test
    void shouldRecreateLineItemSummaryReportThrowsExceptionIfApiNotEnabled() {
        given(serverConfiguration.isRecreateLineItemSummaryApiEnabled()).willReturn(false);
        Instant endTime = Instant.now().truncatedTo(ChronoUnit.HOURS);
        Instant startTime = endTime.minus(TIME_RANGE, ChronoUnit.MINUTES);

        assertThrows(ApiNotActiveException.class, () -> {
            deliverySummaryAdminController.recreateLineItemSummaryReport(
                    startTime.toString(), endTime.toString(), false);
        });
    }

    @Test
    void shouldRecreateLineItemSummaryReportThrowsInvalidRequestExceptionIfEndTimeMoreThan7DaysAgo() {
        Instant endTime = Instant.now().truncatedTo(ChronoUnit.HOURS).minus(8, ChronoUnit.DAYS);
        Instant startTime = endTime.minus(TIME_RANGE, ChronoUnit.MINUTES);
        given(serverConfiguration.getDeliverySummaryServiceAggregateInterval()).willReturn(TIME_RANGE);
        given(serverConfiguration.isRecreateLineItemSummaryApiEnabled()).willReturn(true);
        assertThrows(InvalidRequestException.class, () -> {
            deliverySummaryAdminController.recreateLineItemSummaryReport(
                    startTime.toString(), endTime.toString(), false);
        });
    }

    @Test
    void shouldRecreateLineItemSummaryReportThrowsInvalidRequestExceptionIfTimeRangeNotCorrect() {
        Instant endTime = Instant.now().truncatedTo(ChronoUnit.HOURS);
        Instant startTime = endTime.minus(10, ChronoUnit.MINUTES);
        given(serverConfiguration.isRecreateLineItemSummaryApiEnabled()).willReturn(true);
        given(serverConfiguration.getDeliverySummaryServiceAggregateInterval()).willReturn(TIME_RANGE);
        assertThrows(InvalidRequestException.class, () -> {
            deliverySummaryAdminController.recreateLineItemSummaryReport(
                    startTime.toString(), endTime.toString(), false);
        });
    }

    @Test
    void shouldRecreateLineItemSummaryReportThrowsInvalidRequestExceptionIfStartTimeNotValid() {
        Instant endTime = Instant.now().truncatedTo(ChronoUnit.HOURS).plus(2, ChronoUnit.MINUTES);
        Instant startTime = endTime.minus(TIME_RANGE, ChronoUnit.MINUTES);
        given(serverConfiguration.isRecreateLineItemSummaryApiEnabled()).willReturn(true);
        given(serverConfiguration.getDeliverySummaryServiceAggregateInterval()).willReturn(TIME_RANGE);
        assertThrows(InvalidRequestException.class, () -> {
            deliverySummaryAdminController.recreateLineItemSummaryReport(
                    startTime.toString(), endTime.toString(), false);
        });
    }

    @Test
    void shouldRecreateLineItemSummaryReportThrowExceptionIfEndTimeNotValid() {
        Instant endTime = Instant.now().truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS);
        Instant startTime = endTime.minus(TIME_RANGE, ChronoUnit.MINUTES);

        given(serverConfiguration.isRecreateLineItemSummaryApiEnabled()).willReturn(true);
        given(serverConfiguration.getDeliverySummaryServiceAggregateInterval()).willReturn(TIME_RANGE);
        assertThrows(InvalidRequestException.class, () -> {
            deliverySummaryAdminController.recreateLineItemSummaryReport(
                    startTime.toString(), endTime.toString(), false);
        });
    }

    @Test
    void shouldRecreateLineItemSummaryReportThrowsExceptionIfBadTimeFormat() {
        given(serverConfiguration.isRecreateLineItemSummaryApiEnabled()).willReturn(true);

        assertThrows(InvalidRequestException.class, () -> {
            deliverySummaryAdminController.recreateLineItemSummaryReport("startTime", "endTime", false);
        });
    }

}

