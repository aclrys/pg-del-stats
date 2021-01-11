package org.prebid.pg.delstats.alerts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.config.AlertProxyConfiguration;
import org.prebid.pg.delstats.config.DeploymentConfiguration;
import org.prebid.pg.delstats.model.dto.AlertEvent;
import org.prebid.pg.delstats.model.dto.AlertPayload;
import org.prebid.pg.delstats.model.dto.AlertSource;
import org.prebid.pg.delstats.utils.HttpUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class AlertProxyHttpClient {

    public static final String ALERT_MESSAGE_FORMAT = "%s::%s::%s";

    private ObjectMapper objectMapper;

    private RestTemplate restTemplate;

    private DeploymentConfiguration deploymentConfiguration;

    private AlertProxyConfiguration alertProxyConfiguration;

    private AlertThrottlingService alertThrottlingService;

    private HttpHeaders headers;

    public AlertProxyHttpClient(
            AlertProxyConfiguration alertProxyConfiguration,
            AlertThrottlingService alertThrottlingService,
            DeploymentConfiguration deploymentConfiguration,
            ObjectMapper objectMapper,
            RestTemplate restTemplate
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.alertProxyConfiguration = alertProxyConfiguration;
        this.alertThrottlingService = alertThrottlingService;
        this.deploymentConfiguration = deploymentConfiguration;
        this.headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", HttpUtils.generateBasicAuthHeaderEntry(
                alertProxyConfiguration.getUsername(), alertProxyConfiguration.getPassword())
        );
    }

    private AlertPayload payload(List<AlertEvent> events) {

        return AlertPayload.builder().events(events).build();
    }

    private AlertSource source() {
        return AlertSource.builder()
                .env(deploymentConfiguration.getProfile().toString())
                .hostId("FARGATE_TASK")
                .region(deploymentConfiguration.getRegion())
                .dataCenter(deploymentConfiguration.getDataCenter())
                .subSystem(deploymentConfiguration.getSubSystem())
                .system(deploymentConfiguration.getSystem())
                .build();
    }

    private AlertEvent event(
            String uuid, String action, AlertPriority priority, AlertName name, String details, AlertSource alertSource
    ) {
        return AlertEvent.builder()
                .id(uuid)
                .action(action.toUpperCase())
                .priority(priority.name().toUpperCase())
                .name("del-stats-" + name.name().toLowerCase().replaceAll("_", "-"))
                .details(details)
                .updatedAt(Instant.now())
                .source(alertSource)
                .build();
    }

    public void raiseEventForExceptionAndLog(
            AlertName name, String message, AlertPriority priority, Exception ex, String extraData
    ) {
        String uuid = UUID.randomUUID().toString();
        String alertMessage = String.format(ALERT_MESSAGE_FORMAT, uuid, message, ex.toString());

        log.error(alertMessage);
        if (extraData != null) {
            extraData = String.format(ALERT_MESSAGE_FORMAT, uuid, ex.toString(), extraData);
            log.error(extraData);
        }
        raiseEvent(uuid, name, priority, alertMessage);
    }

    public void raiseEventForExceptionAndLog(
            AlertName name, String message, AlertPriority priority, Exception ex
    ) {
        raiseEventForExceptionAndLog(name, message, priority, ex, null);
    }

    public void sendNotification(
            AlertName name, String message, AlertPriority priority
    ) {
        String uuid = UUID.randomUUID().toString();
        String alertMessage = String.format(ALERT_MESSAGE_FORMAT, uuid, message, "");

        log.info(alertMessage);
        raiseEvent(uuid, name, priority, alertMessage);
    }

    private void raiseEvent(String uuid, AlertName name, AlertPriority priority, String details) {
        if (Boolean.FALSE.equals(alertProxyConfiguration.getEnabled())) {
            log.warn("Alert proxy is not enabled, not sending alerts");
            return;
        }
        sendAlerts(alertThrottlingService.throttleEvent(
                event(uuid, "RAISE", priority, name, details, source())));
    }

    public void sendAlerts(List<AlertEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        try {
            final HttpEntity<String> request =
                    new HttpEntity<>(objectMapper.writeValueAsString(payload(events)), headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    alertProxyConfiguration.getUrl(), HttpMethod.POST, request, String.class
            );
            log.debug("Alert response status code: {}", responseEntity.getStatusCode());
        } catch (RestClientException rce) {
            log.error(
                    "Failure in sending alert to proxy object::{}::{}",
                    alertProxyConfiguration.getUrl(), rce.toString()
            );
        } catch (JsonProcessingException jpe) {
            log.error("Failure in serializing alert object: {}", jpe.toString());
        } catch (Exception e) {
            log.error(
                    "Unexpected exception in sending alert to proxy object::{}::{}",
                    alertProxyConfiguration.getUrl(), e.toString()
            );
        }
    }
}
