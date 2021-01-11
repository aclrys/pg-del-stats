package org.prebid.pg.delstats.alerts;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.prebid.pg.delstats.config.AlertProxyConfiguration;
import org.prebid.pg.delstats.model.dto.AlertEvent;

import static org.mockito.Mockito.*;

public class AlertThrottlingServiceTest {
    private SoftAssertions softAssertions;

    private AlertProxyHttpClient alertProxyHttpClient;
    private AlertThrottlingService alertThrottlingService;

    @BeforeEach
    public void init() {
        softAssertions = new SoftAssertions();

        alertProxyHttpClient = mock(AlertProxyHttpClient.class);
        AlertProxyConfiguration.AlertPolicy alertPolicy = new AlertProxyConfiguration.AlertPolicy();
        alertPolicy.setAlertName("X");
        alertPolicy.setAlertFrequency(3);
        alertPolicy.setInitialAlerts(2);
        AlertProxyConfiguration.AlertPolicy defaultAlertPolicy = new AlertProxyConfiguration.AlertPolicy();
        defaultAlertPolicy.setAlertName("default");
        defaultAlertPolicy.setAlertFrequency(2);
        defaultAlertPolicy.setInitialAlerts(3);

        AlertProxyConfiguration alertProxyConfiguration = mock(AlertProxyConfiguration.class);
        when(alertProxyConfiguration.getPolicies()).thenReturn(Lists.list(defaultAlertPolicy, alertPolicy));

        alertThrottlingService = new AlertThrottlingService(alertProxyConfiguration);
    }

    @Test
    public void shouldAlertImmediatelyThenThrottleThenEvictOnNamedPolicy() {
        AlertEvent alertEventY = AlertEvent.builder().name("X").priority("1").build();
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(1);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(1);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(0);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(0);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(0);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(3);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(0);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(0);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(3);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(0);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(0);
        softAssertions.assertThat(alertThrottlingService.retrieveThrottledEvents()).hasSize(3);
        softAssertions.assertThat(alertThrottlingService.clearEvents()).hasSize(0);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(0);
        softAssertions.assertThat(alertThrottlingService.clearEvents()).hasSize(1);
        softAssertions.assertAll();
    }

    @Test
    public void shouldAlertImmediatelyThenThrottleOnDefault() {
        AlertEvent alertEventY = AlertEvent.builder().name("Y").priority("1").build();
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(1);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(1);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(1);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(0);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(0);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(2);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(0);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(2);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(0);
        softAssertions.assertThat(alertThrottlingService.retrieveThrottledEvents()).hasSize(2);
        softAssertions.assertThat(alertThrottlingService.clearEvents()).hasSize(0);
        softAssertions.assertThat(alertThrottlingService.throttleEvent(alertEventY)).hasSize(0);
        softAssertions.assertThat(alertThrottlingService.clearEvents()).hasSize(1);
        softAssertions.assertAll();
    }

}
