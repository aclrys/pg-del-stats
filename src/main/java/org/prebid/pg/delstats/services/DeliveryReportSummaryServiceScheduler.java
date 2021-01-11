package org.prebid.pg.delstats.services;

import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.alerts.AlertName;
import org.prebid.pg.delstats.alerts.AlertPriority;
import org.prebid.pg.delstats.alerts.AlertProxyHttpClient;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.exception.MissingDeliveryReportSummaryException;
import org.prebid.pg.delstats.exception.SystemInitializationException;
import org.prebid.pg.delstats.model.dto.Shutdown;
import org.prebid.pg.delstats.model.dto.Tracer;
import org.prebid.pg.delstats.persistence.SystemState;
import org.prebid.pg.delstats.repository.SystemStateConstants;
import org.prebid.pg.delstats.repository.SystemStateRepository;
import org.prebid.pg.delstats.utils.TimestampUtils;
import org.prebid.pg.delstats.utils.TracerUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class DeliveryReportSummaryServiceScheduler {

    private final DeliveryReportSummaryService deliveryReportSummaryService;

    private final SystemStateRepository systemStateRepository;

    private final ServerConfiguration serverConfiguration;

    private final Tracer tracer;

    private final Shutdown shutdown;

    private final AlertProxyHttpClient alertProxyHttpClient;

    private final Instant startupTime;

    private final long interval;

    private Clock clock;

    public DeliveryReportSummaryServiceScheduler(
            DeliveryReportSummaryService deliveryReportSummaryService,
            SystemStateRepository systemStateRepository,
            ServerConfiguration serverConfiguration,
            SystemService systemService) {
        this.deliveryReportSummaryService = deliveryReportSummaryService;
        this.systemStateRepository = systemStateRepository;
        this.serverConfiguration = serverConfiguration;
        this.interval = serverConfiguration.getDeliverySummaryServiceAggregateInterval();
        this.tracer = systemService.getTracer();
        this.shutdown = systemService.getShutdown();
        this.alertProxyHttpClient = systemService.getAlertProxyHttpClient();
        this.clock = Clock.tick(Clock.systemDefaultZone(), Duration.of(interval, ChronoUnit.MINUTES));
        this.startupTime = clock.instant();
        if (interval > 60) {
            throw new SystemInitializationException("Invalid Delivery Summary Service Aggration Interval: " + interval);
        }
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Scheduled(cron = "${services.delivery-summary.cron}")
    public void runDeliveryReportSummaryJob() {
        if (shutdown.isInitiating()) {
            log.info("TokenSpendDataService::System is shutting down");
            return;
        }
        if (!serverConfiguration.isDeliverySummaryServiceEnabled()) {
            log.debug("Running scheduled delivery report summary is disabled");
            return;
        }

        if (startupTime.plus(interval, ChronoUnit.MINUTES).isAfter(clock.instant())) {
            TracerUtils.traceAsInfoOrlogAsInfo(log, tracer, String.format(
                    "System startup less than %d minutes. Will run delivery summary report in next cycle.", interval));
            return;
        }

        TracerUtils.traceAsInfoOrlogAsInfo(log, tracer, "Running scheduled delivery report summary");

        try {
            final SystemState previousState =
                    systemStateRepository.retrieveByTag(SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY);
            TracerUtils.traceAsInfoOrlogAsInfo(log, tracer, "previousState=" + previousState);

            //not tested for null
            String startTimeString = (previousState == null) ? Instant.EPOCH.toString() : previousState.getVal();
            TracerUtils.traceAsInfoOrlogAsInfo(log, tracer, "initialStartTime=" + startTimeString);
            Instant startTime = startTime(clock.instant().minus(interval, ChronoUnit.MINUTES), startTimeString);
            Instant finalEndTime = endTime(clock.instant(), startTime);
            TracerUtils.traceAsInfoOrlogAsInfo(
                    log, tracer,
                    String.format(
                            "Running scheduled delivery report summary from %s through %s", startTime, finalEndTime));

            while (startTime.isBefore(finalEndTime)) {
                Instant endTime = startTime.plus(interval, ChronoUnit.MINUTES);
                TracerUtils.traceAsInfoOrlogAsInfo(log, tracer, "endTime=" + endTime);
                if (endTime.isAfter(finalEndTime)) {
                    TracerUtils.traceAsInfoOrlogAsInfo(log, tracer, "endTime=break");
                    break;
                }
                deliveryReportSummaryService
                        .runDeliveryReportSummary(Timestamp.from(startTime), Timestamp.from(endTime), true);
                startTime = endTime;
            }
        } catch (Exception e) {
            String msg = "Unexpected problem in delivery report summary";
            alertProxyHttpClient.raiseEventForExceptionAndLog(
                    AlertName.DELIVERY_REPORT_SUMMARY_ERROR, msg, AlertPriority.HIGH, e);
        } finally {
            TracerUtils.traceAsInfoOrlogAsInfo(log, tracer, "Completed scheduled delivery report summary");
        }
    }

    private Instant toInstant(String timeString) {
        return TimestampUtils.convertStringTimeToTimestamp(
                timeString, SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY).toInstant();
    }

    Instant startTime(Instant now, String startTimeString) {
        Instant startTime = toInstant(startTimeString).truncatedTo(ChronoUnit.MINUTES);
        Duration duration = Duration.between(startTime, now);
        if (duration.toMinutes() % interval != 0) {
            long offset = duration.toMinutes() - (duration.toMinutes() / interval) * interval;
            Instant adjustedStartTime = startTime.plus(offset, ChronoUnit.MINUTES).minus(interval, ChronoUnit.MINUTES);
            startTime = adjustedStartTime;
        }
        if (duration.toMinutes() > serverConfiguration.getDeliverySummaryServiceMaxAggregateIntervals() * interval) {
            Instant adjustedStartTime = now.minus(
                    serverConfiguration.getDeliverySummaryServiceMaxAggregateIntervals() * interval,
                    ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
            String msg = String.format("Missing summary reports from %s to %s", startTime, adjustedStartTime);
            alertProxyHttpClient.raiseEventForExceptionAndLog(
                    AlertName.DELIVERY_REPORT_SUMMARY_ERROR,
                    msg, AlertPriority.HIGH, new MissingDeliveryReportSummaryException(msg)
            );
            startTime = adjustedStartTime;
        }
        return startTime;
    }

    Instant endTime(Instant end, Instant start) {
        Instant maxEnd = start.plusSeconds(serverConfiguration.getDeliverySummaryServiceAggregateInterval()
                * interval * 60);
        if (end.isAfter(maxEnd)) {
            return maxEnd;
        }
        return end;
    }

}
