package org.prebid.pg.delstats.metrics;

import lombok.Getter;

enum MetricTag {
    // Errors
    DATA_ACCESS_ERROR("error.db.general"),
    DUPLICATE_KEY_ERROR("error.db.duplicate-key"),
    QUERY_TIMED_OUT("error.db.timed-out"),
    UNKNOWN_ERROR("error.unknown"),
    REQUEST_MISSING_TRANSACTION_ID("error.request.missing-transaction-id"),
    REQUEST_INVALID("error.request.invalid"),
    // TIMERS
    //SERVICE_OPERATION_LATENCY("${service}.${operation}.processing-time"),
    SERVICE_OPERATION_LATENCY("${service}.${operation}"),
    // COUNTERS
    UNIQUE_PBS_INSTANCES("pbs.instances"),
    SERVICE_OPERATION_ITEM("${service}.${operation}.${item}"),
    SERVICE_ENDPOINT_OPERATION("${service}.${endpoint}.${operation}");

    @Getter
    private String tag;

    MetricTag(final String tag) {
        this.tag = tag;
    }

}
