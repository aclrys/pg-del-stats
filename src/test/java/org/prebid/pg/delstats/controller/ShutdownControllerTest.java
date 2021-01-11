package org.prebid.pg.delstats.controller;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.pg.delstats.model.dto.Shutdown;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShutdownControllerTest {
    SoftAssertions softAssertions;

    @Mock
    private Shutdown shutdown;

    @InjectMocks
    private ShutdownController shutdownController;

    @BeforeEach
    void setUp() {
        softAssertions = new SoftAssertions();
    }

    @Test
    void set() {
        softAssertions.assertThatCode(() -> shutdownController.set()).doesNotThrowAnyException();
        softAssertions.assertAll();
        verify(shutdown, times(1)).setInitiating(eq(true));
    }

    @Test
    void unset() {
        softAssertions.assertThatCode(() -> shutdownController.unset()).doesNotThrowAnyException();
        softAssertions.assertAll();
        verify(shutdown, times(1)).setInitiating(eq(false));
    }
}