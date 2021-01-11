package org.prebid.pg.delstats.model.dto;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.Builder;
import lombok.Getter;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.persistence.DeliveryReportSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class LineItemSummaryReport {

    public static final String DOMAIN_MATCHED = "domainMatched";

    public static final String TARGET_MATCHED = "targetMatched";

    public static final String ACCOUNT_AUCTIONS = "accountAuctions";

    public static final String SENT_TO_BIDDER = "sentToBidder";

    public static final String SENT_TO_BIDDER_AS_TOP_MATCH = "sentToBidderAsTopMatch";

    public static final String SENT_TO_CLIENT = "sentToClient";

    public static final String SENT_TO_CLIENT_AS_TOP_MATCH = "sentToClientAsTopMatch";

    public static final String TARGET_MATCHED_BUT_FCAPPED = "targetMatchedButFcapped";

    public static final String RECEIVED_FROM_BIDDER = "receivedFromBidder";

    public static final String RECEIVED_FROM_BIDDER_INVALIDATED = "receivedFromBidderInvalidated";

    public static final String TARGET_MATCHED_BUT_FCAP_LOOKUP_FAILED = "targetMatchedButFcapLookupFailed";

    public static final String PACING_DEFERRED = "pacingDeferred";

    public static final String WIN_EVENTS = "winEvents";

    public static final String DATA_WINDOW_START_TIMESTAMP = "dataWindowStartTimestamp";

    public static final String DATA_WINDOW_END_TIMESTAMP = "dataWindowEndTimestamp";

    public static final String LINE_ITEM_SOURCE = "lineItemSource";

    public static final String LINE_ITEM_ID = "lineItemId";

    public static final String EXT_LINE_ITEM_ID = "extLineItemId";

    public static final String BIDDER_CODE = "bidderCode";

    public static final String REPORT_WINDOW_START_TIMESTAMP_NATIVE = "report_window_start_timestamp";

    public static final String REPORT_WINDOW_START_TIMESTAMP = "reportWindowStartTimestamp";

    public static final String REPORT_WINDOW_END_TIMESTAMP = "reportWindowEndTimestamp";

    public static final String INTERVAL = "interval";

    private static final Map<String, Function<LineItemStatus, Integer>> METRICS_FUNCTION_MAP =
            buildMetricsFunctionMap();

    private static final Map<String, Function<DeliveryReportSummary, Integer>> SUMMARY_METRICS_FUNCTION_MAP =
            buildSummaryMetricsFunctionMap();

    private static final Set<String> METRICS_FIELDS = buildMetricsSet();

    private LineItemSummaryReport() {
    }

    private static Set<String> buildMetricsSet() {
        Set<String> set = new LinkedHashSet<>(METRICS_FUNCTION_MAP.keySet());
        return Collections.unmodifiableSet(set);
    }

    private static Map<String, Function<DeliveryReportSummary, Integer>> buildSummaryMetricsFunctionMap() {
        Map<String, Function<DeliveryReportSummary, Integer>> map = new LinkedHashMap<>();
        map.put(ACCOUNT_AUCTIONS, DeliveryReportSummary::getAccountAuctions);
        map.put(DOMAIN_MATCHED, DeliveryReportSummary::getDomainMatched);
        map.put(TARGET_MATCHED, DeliveryReportSummary::getTargetMatched);
        map.put(TARGET_MATCHED_BUT_FCAPPED, DeliveryReportSummary::getTargetMatchedButFcapped);
        map.put(TARGET_MATCHED_BUT_FCAP_LOOKUP_FAILED, DeliveryReportSummary::getTargetMatchedButFcapLookupFailed);
        map.put(PACING_DEFERRED, DeliveryReportSummary::getPacingDeferred);
        map.put(SENT_TO_BIDDER, DeliveryReportSummary::getSentToBidder);
        map.put(SENT_TO_BIDDER_AS_TOP_MATCH, DeliveryReportSummary::getSentToBidderAsTopMatch);
        map.put(RECEIVED_FROM_BIDDER_INVALIDATED, DeliveryReportSummary::getReceivedFromBidderInvalidated);
        map.put(RECEIVED_FROM_BIDDER, DeliveryReportSummary::getReceivedFromBidder);
        map.put(SENT_TO_CLIENT, DeliveryReportSummary::getSentToClient);
        map.put(SENT_TO_CLIENT_AS_TOP_MATCH, DeliveryReportSummary::getSentToClientAsTopMatch);
        map.put(WIN_EVENTS, DeliveryReportSummary::getWinEvents);
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, Function<LineItemStatus, Integer>> buildMetricsFunctionMap() {
        Map<String, Function<LineItemStatus, Integer>> map = new LinkedHashMap<>();
        map.put(ACCOUNT_AUCTIONS, LineItemStatus::getAccountAuctions);
        map.put(DOMAIN_MATCHED, LineItemStatus::getDomainMatched);
        map.put(TARGET_MATCHED, LineItemStatus::getTargetMatched);
        map.put(TARGET_MATCHED_BUT_FCAPPED, LineItemStatus::getTargetMatchedButFcapped);
        map.put(TARGET_MATCHED_BUT_FCAP_LOOKUP_FAILED, LineItemStatus::getTargetMatchedButFcapLookupFailed);
        map.put(PACING_DEFERRED, LineItemStatus::getPacingDeferred);
        map.put(SENT_TO_BIDDER, LineItemStatus::getSentToBidder);
        map.put(SENT_TO_BIDDER_AS_TOP_MATCH, LineItemStatus::getSentToBidderAsTopMatch);
        map.put(RECEIVED_FROM_BIDDER_INVALIDATED, LineItemStatus::getReceivedFromBidderInvalidated);
        map.put(RECEIVED_FROM_BIDDER, LineItemStatus::getReceivedFromBidder);
        map.put(SENT_TO_CLIENT, LineItemStatus::getSentToClient);
        map.put(SENT_TO_CLIENT_AS_TOP_MATCH, LineItemStatus::getSentToClientAsTopMatch);
        map.put(WIN_EVENTS, LineItemSummaryReport::aggregateWinEvents);
        return Collections.unmodifiableMap(map);
    }

    public static Set<String> getMetricsFields() {
        return METRICS_FIELDS;
    }

    public static Map<String, Function<LineItemStatus, Integer>> getMetricsFunctionMap() {
        return METRICS_FUNCTION_MAP;
    }

    public static Map<String, Function<DeliveryReportSummary, Integer>> getSummaryMetricsFunctionMap() {
        return SUMMARY_METRICS_FUNCTION_MAP;
    }

    public static <T> void accumulateSimpleField(Map<String, Object> summaryMap, String field,
            Map<String, Function<T, Integer>> functionMap, T item) {
        int val = functionMap.get(field).apply(item);
        if (summaryMap.containsKey(field)) {
            val += (int) summaryMap.get(field);
        }
        summaryMap.put(field, val);
    }

    public static <T> List<T> retrieveLineItemSummaries(String lineItemIds, Instant startTime, Instant endTime,
            Pageable page, RetrievalLineItemSummary<T> function) {
        List<T> reports = new ArrayList<>();
        List<String> lineIds = StringUtils.isEmpty(lineItemIds)
                ? Collections.emptyList()
                : Arrays.stream(lineItemIds.split(","))
                .filter(str -> !StringUtils.isEmpty(str))
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedList::new));

        while (true) {
            Page<T> reportPage = function.retrieve(lineIds, startTime, endTime, page);
            if (reportPage.hasContent()) {
                reports.addAll(reportPage.getContent());
            }
            if (!reportPage.hasNext()) {
                break;
            }
            page = reportPage.nextPageable();
        }
        return reports;
    }

    @FunctionalInterface
    public interface RetrievalLineItemSummary<T> {
        Page<T> retrieve(List<String> lineIds, Instant startTime, Instant endTime, Pageable page);
    }

    private static Set<String> parseMetrics(String metrics) {
        LinkedHashSet<String> allFields = new LinkedHashSet<>(LineItemSummaryReport.METRICS_FIELDS);
        if (StringUtils.isEmpty(metrics)) {
            return allFields;
        }
        // respect user input ordering
        Set<String> metricsSet = Arrays.stream(metrics.trim().split(","))
                .filter(allFields::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return metricsSet.isEmpty() ? allFields : metricsSet;
    }

    public static CsvSchema csvSchema(Set<String> metricsSet) {
        CsvSchema.Builder schemaBuilder = new CsvSchema.Builder().setUseHeader(true);
        schemaBuilder.addColumn(LineItemSummaryReport.INTERVAL, CsvSchema.ColumnType.NUMBER);
        schemaBuilder.addColumn(LineItemSummaryReport.DATA_WINDOW_START_TIMESTAMP, CsvSchema.ColumnType.STRING);
        schemaBuilder.addColumn(LineItemSummaryReport.DATA_WINDOW_END_TIMESTAMP, CsvSchema.ColumnType.STRING);
        schemaBuilder.addColumn(LineItemSummaryReport.LINE_ITEM_ID);
        schemaBuilder.addColumn(LineItemSummaryReport.EXT_LINE_ITEM_ID);
        for (String metrics : metricsSet) {
            schemaBuilder.addColumn(metrics, CsvSchema.ColumnType.NUMBER);
        }
        return schemaBuilder.build();
    }

    private static int aggregateWinEvents(LineItemStatus lineItem) {
        int total = 0;
        if (lineItem.getEvents() != null) {
            for (Event event : lineItem.getEvents()) {
                if ("win".equals(event.getType())) {
                    total += event.getCount();
                }
            }
        }
        return total;
    }

    public static LineItemSummaryRequest validateLineItemSummaryRequest(String metrics, String startTime,
            String endTime, Integer interval, ServerConfiguration config) {
        if (interval == null) {
            interval = config.getLineItemSummaryCsvReportIntervalMinutes();
        }
        int minInterval = config.getLineItemSummaryCsvReportMinIntervalMinutes();
        if (interval < minInterval) {
            interval = minInterval;
        }

        Set<String> metricsSet = parseMetrics(metrics);
        Instant startTimestamp = (startTime != null) ? Instant.parse(startTime).truncatedTo(ChronoUnit.HOURS)
                : Instant.now().minus(config.getLineItemSummaryStartSincePeriodSeconds(), ChronoUnit.SECONDS)
                    .truncatedTo(ChronoUnit.HOURS);
        Instant endTimestamp = (endTime != null) ? Instant.parse(endTime).truncatedTo(ChronoUnit.HOURS)
                : Instant.now().plus(1, ChronoUnit.HOURS)
                    .plus(config.getLineItemSummaryEndSincePeriodSeconds(), ChronoUnit.SECONDS)
                    .truncatedTo(ChronoUnit.HOURS);
        final Instant maxEndTimestamp = startTimestamp.plus(
                config.getLineItemSummaryMaxTimeRangeSeconds(), ChronoUnit.SECONDS);
        if (maxEndTimestamp.isBefore(endTimestamp)) {
            endTimestamp = maxEndTimestamp;
        }
        return LineItemSummaryRequest.builder()
                .startTime(startTimestamp)
                .endTime(endTimestamp)
                .interval(interval)
                .metrics(metricsSet)
                .build();
    }

    @Builder
    @Getter
    public static class LineItemSummaryRequest {
        private Instant startTime;
        private Instant endTime;
        private int interval;
        private Set<String> metrics;
    }
}
