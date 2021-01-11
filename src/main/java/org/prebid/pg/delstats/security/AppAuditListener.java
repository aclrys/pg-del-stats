package org.prebid.pg.delstats.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppAuditListener {
    private static final Logger logger = LoggerFactory.getLogger(AppAuditListener.class);

    @EventListener
    public void onAuditEvent(AuditApplicationEvent event) {
        final AuditEvent auditEvent = event.getAuditEvent();
        if (auditEvent.getType().toUpperCase().contains("FAILURE")) {
            logger.warn("{} => {}", auditEvent.getType(), auditEvent.getPrincipal());
        } else {
            logger.debug("{} => {}", auditEvent.getType(), auditEvent.getPrincipal());
        }
    }
}
