package org.prebid.pg.delstats.metrics;

enum MetricEndpoint {
    // Delivery Report Endpoints
    GET_LINE_ITEMS,
    GET_DELIVERY_STATS,
    GET_DELIVERY_REPORTS,
    POST_DELIVERY_REPORT,
    POST_DELIVERY_SUMMARY,
    RECREATE_LINE_ITEM_SUMMARY,
    // Token Spend Endpoints
    GET_TOKEN_SPEND_SUMMARY,
    // Timer Scheduled Events
    TIMER_AGGREGATE
}
