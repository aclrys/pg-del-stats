package org.prebid.pg.delstats;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.pg.delstats.config.CorsConfiguration;
import org.prebid.pg.delstats.config.ServerAuthDataConfiguration;
import org.prebid.pg.delstats.controller.ServiceController;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeliveryStatsServerTest {
    @Mock
    private ServerAuthDataConfiguration serverAuthDataConfiguration;

    @Mock
    private CorsConfiguration corsConfiguration;

    @InjectMocks
    DeliveryStatsServer deliveryStatsServer;

    @Test
    public void shouldCorsConfigurerNotThrowExceptionWhenUsed() {
        SoftAssertions softAssertions = new SoftAssertions();

        softAssertions.assertThatCode(() ->deliveryStatsServer.corsConfigurer()).doesNotThrowAnyException();
        softAssertions.assertAll();
    }
}
