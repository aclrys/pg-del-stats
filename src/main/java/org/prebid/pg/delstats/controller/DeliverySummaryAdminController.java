package org.prebid.pg.delstats.controller;

import com.codahale.metrics.Timer;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.exception.ApiNotActiveException;
import org.prebid.pg.delstats.exception.InvalidRequestException;
import org.prebid.pg.delstats.metrics.GraphiteMetricsRecorder;
import org.prebid.pg.delstats.model.dto.FreshnessStates;
import org.prebid.pg.delstats.model.dto.Shutdown;
import org.prebid.pg.delstats.services.DeliveryReportSummaryService;
import org.prebid.pg.delstats.services.SystemService;
import org.prebid.pg.delstats.services.SystemStateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * A controller for admin user to manage Delivery Progress Summary Report.
 */

@Slf4j
@RestController
@RequestMapping("${services.admin-base-url}")
@Api(tags = {"read-only"})
public class DeliverySummaryAdminController {

    private Shutdown shutdown;

    private final ServerConfiguration serverConfiguration;

    private final SystemStateService systemStateService;

    private final GraphiteMetricsRecorder recorder;

    private final DeliveryReportSummaryService deliveryReportSummaryService;

    public DeliverySummaryAdminController(
            DeliveryReportSummaryService deliveryReportSummaryService,
            ServerConfiguration serverConfiguration,
            SystemService systemService,
            SystemStateService systemStateService,
            GraphiteMetricsRecorder recorder
    ) {
        this.serverConfiguration = serverConfiguration;
        this.systemStateService = systemStateService;
        this.shutdown = systemService.getShutdown();
        this.recorder = recorder;
        this.deliveryReportSummaryService = deliveryReportSummaryService;
    }

    @GetMapping(path = "/v1/report/summary/freshness")
    public FreshnessStates getFreshness() {
        if (!serverConfiguration.isDeliverySummaryFreshnessApiEnabled()) {
            throw new ApiNotActiveException("/v1/report/summary/freshness is not active");
        }
        checkForShutdown();
        return systemStateService.getFreshnessStates();
    }

    @PostMapping(path = "/v1/create/line-item-summary")
    @ResponseStatus(HttpStatus.OK)
    public String recreateLineItemSummaryReport(
            @RequestParam(value = "startTime") String startTime,
            @RequestParam(value = "endTime") String endTime,
            @RequestParam(value = "overwrite", defaultValue = "false") boolean overwrite) {
        log.info("Received recreateLineItemSummary request. startTime:{}, endTime {}", startTime, endTime);
        if (!serverConfiguration.isRecreateLineItemSummaryApiEnabled()) {
            throw new ApiNotActiveException("/v1/create/line-item-summary is not active");
        }
        checkForShutdown();

        Optional<Timer.Context> timer = recorder.getRecreateLineItemSummaryTimer();
        Instant startTimestamp = toInstant(startTime).truncatedTo(ChronoUnit.MINUTES);
        Instant endTimestamp = toInstant(endTime).truncatedTo(ChronoUnit.MINUTES);
        validateRequest(startTimestamp, endTimestamp);

        int summariesSaved = deliveryReportSummaryService.recreateLineItemSummaryReport(
                Timestamp.from(startTimestamp), Timestamp.from(endTimestamp), overwrite);
        timer.ifPresent(Timer.Context::stop);
        return summariesSaved + " summaries were created.";
    }

    private void validateRequest(Instant startTime, Instant endTime) {
        long duration = Duration.between(startTime, endTime).toMinutes();
        if (duration != serverConfiguration.getDeliverySummaryServiceAggregateInterval()) {
            throw new InvalidRequestException("The duration between startTime and endTime is not "
                    + serverConfiguration.getDeliverySummaryServiceAggregateInterval() + " minutes.");
        }
        int startMinute = startTime.atZone(ZoneOffset.UTC).getMinute();
        if (startMinute % serverConfiguration.getDeliverySummaryServiceAggregateInterval() != 0) {
            throw new InvalidRequestException(
                    "startTime and endTime must be multiples of "
                            +
                    serverConfiguration.getDeliverySummaryServiceAggregateInterval() + " minutes"
            );
        }
        Instant now = Instant.now();
        Instant earliestTime =
                now.minus(serverConfiguration.getRecreateLineItemSummaryApiMaxLookBackInDays(), ChronoUnit.DAYS);
        if (startTime.isBefore(earliestTime)) {
            throw new InvalidRequestException(
                    "startTime is more than "
                            + serverConfiguration.getRecreateLineItemSummaryApiMaxLookBackInDays() + " days from now.");
        }
        if (!endTime.isBefore(now)) {
            throw new InvalidRequestException("endTime should not be in future time");
        }
    }

    private Instant toInstant(String timeString) {
        try {
            return Instant.parse(timeString);
        } catch (Exception ex) {
            throw new InvalidRequestException(
                    "Timestamps must be in ISO-8601 format. For example, 2019-02-01T03:05:00.000Z");
        }
    }

    private void checkForShutdown() {
        if (shutdown.isInitiating()) {
            throw new IllegalStateException("Service shutdown has been initiated");
        }
    }

}

