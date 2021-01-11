package org.prebid.pg.delstats.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.alerts.AlertProxyHttpClient;
import org.prebid.pg.delstats.alerts.AlertThrottlingService;
import org.prebid.pg.delstats.config.DeploymentConfiguration;
import org.prebid.pg.delstats.model.dto.AlertEvent;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints to control the Alert feature to force throttling and clear out any throttled alerts.
 */
@Slf4j
@RestController
@RequestMapping("${services.admin-base-url}")
@Api(tags = "admin")
public class AlertController {

    private AlertProxyHttpClient alertProxyHttpClient;

    private AlertThrottlingService alertThrottlingService;

    private DeploymentConfiguration deploymentConfiguration;

    public AlertController(AlertProxyHttpClient alertProxyHttpClient,
                           AlertThrottlingService alertThrottlingService,
                           DeploymentConfiguration deploymentConfiguration) {
        this.alertProxyHttpClient = alertProxyHttpClient;
        this.alertThrottlingService = alertThrottlingService;
        this.deploymentConfiguration = deploymentConfiguration;
    }

    /**
     * Endpoint to clear any events that are currently being throttled.
     */
    @PostMapping(value = "/v1/clear")
    @ResponseStatus(HttpStatus.OK)
    public void clear() {
        alertThrottlingService.clearEvents();
    }

    /**
     * Endpoint to force an alert into the throttled state.
     *
     * @param alertEvent
     * @return
     */
    @PostMapping(value = "/v1/throttle-event")
    @ResponseStatus(HttpStatus.OK)
    public HttpStatus throttle(AlertEvent alertEvent) {
        if (DeploymentConfiguration.ProfileType.PROD.equals(deploymentConfiguration.getProfile())) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        List<AlertEvent> returnedAlerts = alertThrottlingService.throttleEvent(alertEvent);
        alertProxyHttpClient.sendAlerts(returnedAlerts);
        return HttpStatus.OK;
    }
}
