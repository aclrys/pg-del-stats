package org.prebid.pg.delstats.alerts;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.pg.delstats.config.AlertProxyConfiguration;
import org.prebid.pg.delstats.config.DeploymentConfiguration;
import org.prebid.pg.delstats.model.dto.AlertEvent;
import org.prebid.pg.delstats.model.dto.AlertSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertProxyHttpClientTest {
    SoftAssertions softAssertions;

    @Mock
    private HttpHeaders httpHeaders;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DeploymentConfiguration deploymentConfiguration;

    @Mock
    private AlertProxyConfiguration alertProxyConfiguration;

    @Mock
    private AlertThrottlingService alertThrottlingService;

    @InjectMocks
    private AlertProxyHttpClient alertProxyHttpClient;

    @BeforeEach
    public void init() {
        softAssertions = new SoftAssertions();

    }

    @Test
    void raiseEventForExceptionAndLogShouldNotThrowException() {
        when(alertThrottlingService.throttleEvent(any(AlertEvent.class))).thenReturn(Collections.emptyList());
        when(deploymentConfiguration.getProfile()).thenReturn(DeploymentConfiguration.ProfileType.TEST);
        when(deploymentConfiguration.getRegion()).thenReturn("region");
        when(alertProxyConfiguration.getEnabled()).thenReturn(true).thenReturn(true).thenReturn(false);

        softAssertions.assertThatCode(() -> alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.ERROR, "",AlertPriority.HIGH, new NullPointerException(), ""))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.ERROR, "",AlertPriority.MEDIUM, new NullPointerException()))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.ERROR, "",AlertPriority.NOTIFICATION, new NullPointerException()))
                .doesNotThrowAnyException();
        softAssertions.assertAll();
    }

    @Test
    void sendAlerts() throws Exception {
        ResponseEntity<String> responseEntity = mock(ResponseEntity.class);

        when(alertProxyConfiguration.getUrl()).thenReturn("url");
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{}")
                .thenReturn("{}")
                .thenReturn("{}")
                .thenThrow(new JsonParseException(null, "oops"));
        ;
        when(restTemplate.exchange(eq("url"), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class)))
                .thenReturn(responseEntity)
                .thenThrow(new RestClientException("oops"))
                .thenThrow(new RuntimeException("oops"));

        AlertEvent alertEvent = AlertEvent.builder()
                .name("test")
                .priority("p1")
                .action("a1")
                .details("dets")
                .id("id1")
                .source(AlertSource.builder().env("test").build())
                .updatedAt(Instant.now())
                .build();
        List<AlertEvent> events = Collections.singletonList(alertEvent);

        softAssertions.assertThatCode(() ->alertProxyHttpClient.sendAlerts(Collections.emptyList())).doesNotThrowAnyException();
        softAssertions.assertThatCode(() ->alertProxyHttpClient.sendAlerts(events)).doesNotThrowAnyException();
        softAssertions.assertThatCode(() ->alertProxyHttpClient.sendAlerts(events)).doesNotThrowAnyException();
        softAssertions.assertThatCode(() ->alertProxyHttpClient.sendAlerts(events)).doesNotThrowAnyException();
        softAssertions.assertThatCode(() ->alertProxyHttpClient.sendAlerts(events)).doesNotThrowAnyException();

        softAssertions.assertAll();
    }
}