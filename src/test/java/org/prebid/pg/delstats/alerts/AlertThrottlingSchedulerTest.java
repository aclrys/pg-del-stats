package org.prebid.pg.delstats.alerts;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.pg.delstats.model.dto.Shutdown;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertThrottlingSchedulerTest {
    @Mock
    AlertProxyHttpClient alertProxyHttpClient;

    @Mock
    AlertThrottlingService alertThrottlingService;

    @Mock
    Shutdown shutdown;

    AlertThrottlingScheduler alertThrottlingScheduler;

    SoftAssertions softAssertions;

    @BeforeEach
    void setUp() {
        alertThrottlingScheduler = new AlertThrottlingScheduler(alertProxyHttpClient, alertThrottlingService, shutdown);
        softAssertions = new SoftAssertions();
    }

    @Test
    public void shouldAlwaysCallAlertServiceRegardlessOfShutdown() {
        when(shutdown.isInitiating()).thenReturn(true).thenReturn(false);

        softAssertions.assertThatCode(() -> alertThrottlingScheduler.deliveryThrottledEvents())
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> alertThrottlingScheduler.deliveryThrottledEvents())
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> alertThrottlingScheduler.deliveryThrottledEvents())
                .doesNotThrowAnyException();
        softAssertions.assertAll();

        verify(alertThrottlingService, times(3)).retrieveThrottledEvents();
    }
}