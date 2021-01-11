package org.prebid.pg.delstats.alerts;

import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.config.AlertProxyConfiguration;
import org.prebid.pg.delstats.model.dto.AlertEvent;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AlertThrottlingService {

    private Map<String, AlertProxyConfiguration.AlertPolicy> alertPolicies;
    private Map<String, List<AlertEvent>> throttledEventsMap;
    private Map<String, AtomicInteger> initialEventsMaps;

    public AlertThrottlingService(AlertProxyConfiguration alertProxyConfiguration) {
        this.alertPolicies = alertProxyConfiguration.getPolicies().stream().collect(
                Collectors.toMap(org.prebid.pg.delstats.config.AlertProxyConfiguration.AlertPolicy::getAlertName,
                        Function.identity()));
        this.throttledEventsMap = new ConcurrentHashMap<>();
        this.initialEventsMaps = new ConcurrentHashMap<>();
    }

    public List<AlertEvent> retrieveThrottledEvents() {
        Map<String, List<AlertEvent>> returnAlertList;
        synchronized (this) {
            returnAlertList = throttledEventsMap;
            throttledEventsMap = new ConcurrentHashMap<>();
        }
        return returnAlertList.values().stream()
                .flatMap(Collection::stream).collect(Collectors.toList());
    }

    public List<AlertEvent> throttleEvent(AlertEvent alertEvent) {
        AlertProxyConfiguration.AlertPolicy alertPolicy = alertPolicies.get(alertEvent.getName());
        if (alertPolicy == null) {
            alertPolicy = alertPolicies.get("default");
            if (alertPolicy == null) {
                log.debug("Sending Alert now: no default policy");
                return Collections.singletonList(alertEvent);
            }
        }
        String alertNamePriority = String.join("%", alertEvent.getName(), alertEvent.getPriority());
        AtomicInteger seenEvents = initialEventsMaps.computeIfAbsent(alertNamePriority, k -> new AtomicInteger());
        if (seenEvents.getAndIncrement() < alertPolicy.getInitialAlerts()) {
            log.debug("Sending Alert {} now: initial throttle threshold not yet met.", alertNamePriority);
            return Collections.singletonList(alertEvent);
        }
        List<AlertEvent> excessiveEvents = Collections.emptyList();
        synchronized (this) {
            List<AlertEvent> throttledEvents = throttledEventsMap.computeIfAbsent(alertNamePriority,
                    k -> new LinkedList<>());
            if (throttledEvents.size() >= alertPolicy.getAlertFrequency()) {
                excessiveEvents = throttledEvents;
                log.info("Raising {} Alerts", excessiveEvents.size());
                throttledEvents = new LinkedList<>();
                throttledEventsMap.put(alertNamePriority, throttledEvents);
            } else {
                log.debug("Throttling {} event at counter {}", alertNamePriority, throttledEvents.size());
            }
            throttledEvents.add(alertEvent);
        }
        return excessiveEvents;
    }

    public List<AlertEvent> clearEvents() {
        List<AlertEvent> remainingEvents;
        synchronized (this) {
            remainingEvents = throttledEventsMap.values().stream()
                    .flatMap(Collection::stream).collect(Collectors.toList());
            throttledEventsMap = new ConcurrentHashMap<>();
        }
        return remainingEvents;
    }
}
