package org.prebid.pg.delstats.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.temporal.ChronoUnit;

@Configuration
@Data
public class ServerConfiguration {
    @Value("${api.get-delivery-report.start-since-period-sec}")
    private long getDeliveryReportStartSincePeriodSeconds;

    @Value("${api.get-delivery-report.end-since-period-sec}")
    private long getDeliveryReportEndSincePeriodSeconds;

    @Value("${api.get-token-spend-report.enabled}")
    private boolean tokenSpendApiEnabled;

    @Value("${api.get-token-spend-report.start-since-period-sec}")
    private long getTokenSpendReportSincePeriodSeconds;

    @Value("${api.get-delivery-report.enabled}")
    private boolean paApiEnabled;

    @Value("${api.get-delivery-report.page-size}")
    private int deliveryReportsPageSize;

    @Value("${api.delivery-summary-freshness.enabled}")
    private boolean deliverySummaryFreshnessApiEnabled;

    @Value("${api.get-line-item-summary.enabled}")
    private boolean sumApiEnabled;

    @Value("${api.get-line-item-summary.start-since-period-sec}")
    private long lineItemSummaryStartSincePeriodSeconds;

    @Value("${api.get-line-item-summary.end-since-period-sec}")
    private long lineItemSummaryEndSincePeriodSeconds;

    @Value("${api.get-line-item-summary.pager-size}")
    private int lineItemSummaryPagerSize;

    @Value("${api.get-line-item-summary.max-time-range-sec}")
    private int lineItemSummaryMaxTimeRangeSeconds;

    @Value("${api.get-line-item-summary.csv-report-min-interval-minute}")
    private int lineItemSummaryCsvReportMinIntervalMinutes;

    @Value("${api.get-line-item-summary.csv-report-interval-minute}")
    private int lineItemSummaryCsvReportIntervalMinutes;

    @Value("${services.delivery-summary.aggregate-interval-minute}")
    private int deliverySummaryServiceAggregateInterval;

    @Value("${services.delivery-summary.max-aggregate-intervals}")
    private long deliverySummaryServiceMaxAggregateIntervals;

    @Value("${services.delivery-summary.max-summary-intervals}")
    private long deliverySummaryServiceMaxSummaryIntervals;

    @Value("${services.delivery-summary.enabled}")
    private boolean deliverySummaryServiceEnabled;

    @Value("${services.token-aggr.enabled}")
    private boolean aggregationEnabled;

    @Value("${services.token-aggr.max-look-back-sec}")
    private int maxLookBack;

    @Value("${services.token-aggr.delivery-schedule-field-name}")
    private String deliveryScheduleFieldName;

    @Value("${services.token-aggr.plan-start-timestamp-field-name}")
    private String planStartTimestampFieldName;

    @Value("${services.token-aggr.plan-end-timestamp-field-name}")
    private String planEndTimestampFieldName;

    @Value("${services.token-aggr.target-matched-field-name}")
    private String targetMatched;

    @Value("${services.line-item-biddder-code-separator}")
    private String lineItemBidderCodeSeparator;

    @Value("${services.delivery-report.bidder-alias-mappings}")
    private String bidderAliasMappingString;

    @Value("${services.delivery-report.instance-name-cache.expiration-amount}")
    private int deliveryReportInstanceNameCacheExpirationAmount;

    @Value("${services.delivery-report.instance-name-cache.expiration-unit}")
    private ChronoUnit deliveryReportInstanceNameCacheExpirationUnit;

    @Value("${services.validation.enabled}")
    private boolean validationEnabled;

    @Value("${services.delivery-summary-freshness-alert.enabled}")
    private boolean deliverySummaryFreshnessAlertEnabled;

    @Value("${api.recreate-line-item-summary.enabled}")
    private boolean recreateLineItemSummaryApiEnabled;

    @Value("${api.recreate-line-item-summary.max-look-back-in-days}")
    private int recreateLineItemSummaryApiMaxLookBackInDays;

    @Value("${api.jetty-gzip-handler.enabled}")
    private boolean jettyGzipHandlerEnabled;
}
