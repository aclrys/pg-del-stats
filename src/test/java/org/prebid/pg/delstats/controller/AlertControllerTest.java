package org.prebid.pg.delstats.controller;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.pg.delstats.alerts.AlertProxyHttpClient;
import org.prebid.pg.delstats.alerts.AlertThrottlingService;
import org.prebid.pg.delstats.config.DeploymentConfiguration;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertControllerTest {
    private SoftAssertions softAssertions;

    @Mock
    private AlertProxyHttpClient alertProxyHttpClient;

    @Mock
    private AlertThrottlingService alertThrottlingService;

    @Mock
    private DeploymentConfiguration deploymentConfiguration;

    @InjectMocks
    private AlertController alertController;

    @BeforeEach
    public void init() {
        softAssertions = new SoftAssertions();
    }

    @Test
    void clear() {
        softAssertions.assertThatCode(() -> alertController.clear()).doesNotThrowAnyException();
        softAssertions.assertAll();
        verify(alertThrottlingService, times(1)).clearEvents();
    }

    @Test
    void throttle() {
        when(deploymentConfiguration.getProfile())
                .thenReturn(DeploymentConfiguration.ProfileType.PROD)
                .thenReturn(DeploymentConfiguration.ProfileType.TEST);
        softAssertions.assertThat(alertController.throttle(null))
                .isEqualByComparingTo(HttpStatus.SERVICE_UNAVAILABLE);
        softAssertions.assertThat(alertController.throttle(null))
                .isEqualByComparingTo(HttpStatus.OK);
        softAssertions.assertAll();
        verify(alertThrottlingService, times(1)).throttleEvent(isNull());
        verify(alertProxyHttpClient, times(1)).sendAlerts(anyList());
    }
}