package org.prebid.pg.delstats.services;

import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.alerts.AlertName;
import org.prebid.pg.delstats.alerts.AlertPriority;
import org.prebid.pg.delstats.alerts.AlertProxyHttpClient;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.persistence.SystemState;
import org.prebid.pg.delstats.repository.SystemStateConstants;
import org.prebid.pg.delstats.repository.SystemStateRepository;
import org.prebid.pg.delstats.utils.TimestampUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * A Job running periodically to check freshness of Delivery Progress Summary Report and
 * sending out an alert if the report is stale.
 */

@Service
@Slf4j
public class DeliverySummaryFreshnessAlertService {

    private static final String ALERT_MESSAGE = "DeliveryReportSummaryService has not run since::";

    private final SystemStateRepository systemStateRepo;

    private final ServerConfiguration serverConfig;

    private final SystemService systemService;

    private final AlertProxyHttpClient alertClient;

    public DeliverySummaryFreshnessAlertService(
            SystemStateRepository systemStateRepo,
            ServerConfiguration serverConfig,
            SystemService systemService,
            AlertProxyHttpClient alertClient
    ) {
        this.systemStateRepo = systemStateRepo;
        this.serverConfig = serverConfig;
        this.systemService = systemService;
        this.alertClient = alertClient;
    }

    @Scheduled(cron = "${services.delivery-summary-freshness-alert.cron}")
    public void checkFreshness() {
        if (systemService.getShutdown().isInitiating()) {
            log.info("DeliverySummaryFreshnessAlertService::System is shutting down");
            return;
        }
        if (!serverConfig.isDeliverySummaryFreshnessAlertEnabled()) {
            log.debug("DeliverySummaryFreshnessAlertService is disabled");
            return;
        }

        log.info("Checking freshness...");
        long maxAggregationTime = serverConfig.getDeliverySummaryServiceMaxAggregateIntervals()
                * serverConfig.getDeliverySummaryServiceAggregateInterval();

        Instant now = Instant.now();
        SystemState state = systemStateRepo.retrieveByTag(SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY);
        Instant expectedLatestRun = now.minus(maxAggregationTime, ChronoUnit.MINUTES);
        StringBuilder alertMessageBuilder = new StringBuilder();
        if (state == null) {
            if (systemService.getStartupTime().isBefore(expectedLatestRun)) {
                alertMessageBuilder.append(ALERT_MESSAGE).append(systemService.getStartupTime()).append(".");
            }
        } else {
            Instant latestRun = getEndTimestamp(state);
            if (latestRun.isBefore(expectedLatestRun)) {
                alertMessageBuilder.append(ALERT_MESSAGE).append(latestRun).append(".");
            }
        }

        List<SystemState> latestSummaryReportStates = systemStateRepo.getLatestDeliverySummaryReportStates(
                Timestamp.from(now.minus(60, ChronoUnit.MINUTES)));
        if (!latestSummaryReportStates.isEmpty()) {
            Instant previousEndTime = getEndTimestamp(latestSummaryReportStates.get(0));
            boolean found = false;
            for (int i = 1; i < latestSummaryReportStates.size(); i++) {
                Instant currentEndTime = getEndTimestamp(latestSummaryReportStates.get(i));
                if (Duration.between(currentEndTime, previousEndTime).toMinutes() > maxAggregationTime) {
                    if (!found) {
                        found = true;
                        alertMessageBuilder.append("DeliveryReportSummaryService failed to run within intervals::");
                    }
                    alertMessageBuilder
                            .append("[")
                            .append(previousEndTime)
                            .append(", ")
                            .append(currentEndTime)
                            .append("]");
                }
                previousEndTime = currentEndTime;
            }
        }

        if (alertMessageBuilder.length() > 0) {
            String alertMessage = alertMessageBuilder.toString();
            log.error(alertMessage);
            alertClient.sendNotification(AlertName.FRESHNESS_ERROR, alertMessage, AlertPriority.MEDIUM);
        }
        log.info("Done checking freshness");
    }

    private Instant getEndTimestamp(SystemState state) {
        return TimestampUtils.convertStringTimeToTimestamp(state.getVal(), state.getTag()).toInstant();
    }

}
