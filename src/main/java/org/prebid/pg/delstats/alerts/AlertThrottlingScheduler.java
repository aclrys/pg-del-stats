package org.prebid.pg.delstats.alerts;

import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.model.dto.AlertEvent;
import org.prebid.pg.delstats.model.dto.Shutdown;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AlertThrottlingScheduler {
    private final AlertProxyHttpClient alertProxyHttpClient;
    private final AlertThrottlingService alertThrottlingService;
    private final Shutdown shutdown;
    private boolean previousShutdownState;

    public AlertThrottlingScheduler(AlertProxyHttpClient alertProxyHttpClient,
                                    AlertThrottlingService alertThrottlingService,
                                    Shutdown shutdown) {
        this.alertProxyHttpClient = alertProxyHttpClient;
        this.alertThrottlingService = alertThrottlingService;
        this.shutdown = shutdown;
        this.previousShutdownState = shutdown.isInitiating();
    }

    @Scheduled(initialDelayString = "${services.alert-throttling.initial-delay}000",
            fixedDelayString = "${services.alert-throttling.fixed-delay}000")
    public void deliveryThrottledEvents() {
        if (shutdown.isInitiating()) {
            if (shutdown.isInitiating() != previousShutdownState) {
                log.info("Detected shutting down but alerts must go on!");
            }
        }
        previousShutdownState = shutdown.isInitiating();
        List<AlertEvent> alertsToSend = alertThrottlingService.retrieveThrottledEvents();
        alertProxyHttpClient.sendAlerts(alertsToSend);
    }
}
