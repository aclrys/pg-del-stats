package org.prebid.pg.delstats.security;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;

import static org.junit.jupiter.api.Assertions.*;

class AppAuditListenerTest {
    private SoftAssertions softAssertions;

    private AppAuditListener appAuditListener;

    @BeforeEach
    void setUp() {
        softAssertions = new SoftAssertions();

        appAuditListener = new AppAuditListener();
    }

    @Test
    void onAuditEvent() {
        AuditApplicationEvent okAuditApplicationEvent = new AuditApplicationEvent("principal", "ok");
        AuditApplicationEvent failAuditApplicationEvent = new AuditApplicationEvent("principal", "failure");
        softAssertions.assertThatCode(() -> appAuditListener.onAuditEvent(okAuditApplicationEvent))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> appAuditListener.onAuditEvent(failAuditApplicationEvent))
                .doesNotThrowAnyException();
        softAssertions.assertAll();
    }
}