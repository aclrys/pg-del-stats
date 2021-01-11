package org.prebid.pg.delstats.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.config.GraphiteConfig;
import org.prebid.pg.delstats.exception.MissingTransactionIdException;
import org.prebid.pg.delstats.repository.RepositoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class GraphiteMetricsRecorder {

    private static final String SERVICE_PLACEHOLDER = "\\$\\{service\\}";
    private static final String ENDPOINT_PLACEHOLDER = "\\$\\{endpoint\\}";
    private static final String OPERATION_PLACEHOLDER = "\\$\\{operation\\}";
    private static final String ITEM_PLACEHOLDER = "\\$\\{item\\}";

    private final MetricRegistry registry;
    private final GraphiteConfig graphiteConfig;

    private static Function<GraphiteConfig, Graphite> graphiteSupplier =
            config -> new Graphite(new InetSocketAddress(config.getHost(), config.getPort()));

    @Autowired
    public GraphiteMetricsRecorder(final GraphiteConfig graphiteConfig) {
        this.registry = new MetricRegistry();
        this.graphiteConfig = graphiteConfig;
    }

    @PostConstruct
    public void init() {
        if (!graphiteConfig.isEnabled()) {
            log.warn("Metrics not enabled.");
            return;
        }

        log.info("Starting {} host - [{}:{}].", GraphiteMetricsRecorder.class.getCanonicalName(),
                graphiteConfig.getHost(), graphiteConfig.getPort());
        GraphiteReporter.forRegistry(registry)
                .prefixedWith(graphiteConfig.getPrefix())
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphiteSupplier.apply(graphiteConfig))
            .start(1, TimeUnit.MINUTES);
    }

    static void setGraphiteSupplier(Function<GraphiteConfig, Graphite> gs) {
        graphiteSupplier = gs;
    }

    MetricRegistry getRegistry() {
        return registry;
    }

    public void registerUniquePBSInstanceGauge(Gauge<Integer> gauge) {
        registry.gauge(fixUpEnumName(MetricTag.UNIQUE_PBS_INSTANCES.getTag()), () -> gauge);
    }

    /**
     * Exception Metrics
     */

    public void markDataAccessErrorMeter() {
        registry.counter(fixUpEnumName(MetricTag.DATA_ACCESS_ERROR.getTag()), ResettingCounter::new).inc();
    }

    public void markDuplicateKeyErrorMeter() {
        registry.counter(fixUpEnumName(MetricTag.DUPLICATE_KEY_ERROR.getTag()), ResettingCounter::new).inc();
    }

    public void markInvalidRequestMeter() {
        registry.counter(fixUpEnumName(MetricTag.REQUEST_INVALID.getTag()), ResettingCounter::new).inc();
    }

    public void markMissingTransactionIdMeter() {
        registry.counter(fixUpEnumName(MetricTag.REQUEST_MISSING_TRANSACTION_ID.getTag()), ResettingCounter::new).inc();
    }

    public void markQueryTimedOutMeter() {
        registry.counter(fixUpEnumName(MetricTag.QUERY_TIMED_OUT.getTag()), ResettingCounter::new).inc();
    }

    public void markUnknownErrorMeter() {
        registry.counter(fixUpEnumName(MetricTag.UNKNOWN_ERROR.getTag()), ResettingCounter::new).inc();
    }

    public void markExceptionMeter(Exception ex) {
        if (ex instanceof DataAccessException) {
            if (ex instanceof DuplicateKeyException) {
                markDuplicateKeyErrorMeter();
                return;
            }
            if (ex instanceof QueryTimeoutException) {
                markQueryTimedOutMeter();
                return;
            }
            markDataAccessErrorMeter();
        }
        if (ex instanceof MissingTransactionIdException) {
            markMissingTransactionIdMeter();
            return;
        }
        if (ex instanceof HttpMessageNotReadableException) {
            markInvalidRequestMeter();
            return;
        }
        markUnknownErrorMeter();
    }

    /**
     * Request Endpoint Metrics
     */

    public void markGetRequestForDeliveryLineItems() {
        markMeterForRequests(MetricService.DELIVERY_REPORT, MetricEndpoint.GET_LINE_ITEMS);
    }

    public void markGetRequestForDeliveryStats() {
        markMeterForRequests(MetricService.DELIVERY_REPORT, MetricEndpoint.GET_DELIVERY_STATS);
    }

    public void markGetRequestForDeliveryReports() {
        markMeterForRequests(MetricService.DELIVERY_REPORT, MetricEndpoint.GET_DELIVERY_REPORTS);
    }

    public void markPostRequestForDeliveryReport() {
        markMeterForRequests(MetricService.DELIVERY_REPORT, MetricEndpoint.POST_DELIVERY_REPORT);
    }

    public void markGetRequestForTokenSpend() {
        markMeterForRequests(MetricService.TOKEN_SPEND, MetricEndpoint.GET_TOKEN_SPEND_SUMMARY);
    }

    /**
     * Scheduled Event Metrics
     */

    public void markScheduledAggregation() {
        markMeterForRequests(MetricService.TOKEN_SPEND, MetricEndpoint.TIMER_AGGREGATE);
    }

    public void markScheduledDeliverySummary() {
        markMeterForRequests(MetricService.DELIVERY_SUMMARY, MetricEndpoint.TIMER_AGGREGATE);
    }

    /**
     * Data Access Metrics
     */
    public void markDeliveryReportsSummariesFetched(int inc) {
        markMeterForItems(MetricService.DELIVERY_SUMMARY, MetricOperation.FETCH, RepositoryItem.DELIVERY_REPORTS, inc);
    }

    public void markDeliveryReportRecordsFetched(int inc) {
        markMeterForItems(MetricService.DELIVERY_REPORT, MetricOperation.FETCH, RepositoryItem.DELIVERY_REPORTS, inc);
    }

    public void markDeliveryReportRecordsStored(int inc) {
        markMeterForItems(MetricService.DELIVERY_REPORT, MetricOperation.STORE, RepositoryItem.DELIVERY_REPORTS, inc);
    }

    public void markLatestTokenSpendSummariesFetched(int inc) {
        markMeterForItems(MetricService.TOKEN_SPEND, MetricOperation.FETCH,
                RepositoryItem.LATEST_TOKEN_SPEND_SUMMARIES, inc);
    }

    public void markLatestTokenSpendSummariesStored(int inc) {
        markMeterForItems(MetricService.TOKEN_SPEND, MetricOperation.STORE,
                RepositoryItem.LATEST_TOKEN_SPEND_SUMMARIES, inc);
    }

    public void markDeliverySummariesDeleted(int inc) {
        markMeterForItems(MetricService.DELIVERY_SUMMARY, MetricOperation.DELETE,
                RepositoryItem.DELIVERY_SUMMARY_REPORTS, inc);
    }

    public void markDeliverySummariesStored(int inc) {
        markMeterForItems(MetricService.DELIVERY_SUMMARY, MetricOperation.STORE,
                RepositoryItem.DELIVERY_SUMMARY_REPORTS, inc);
    }

    /**
     * Request Endpoint Timer Metrics
     */

    public Optional<Timer.Context> getDeliveryReportsPerformanceTimer() {
        return getTimerContextForEndpointOperation(
                MetricEndpoint.GET_DELIVERY_REPORTS, MetricOperation.PROCESSING_TIME);
    }

    public Optional<Timer.Context> getDeliveryReportStatsPerformanceTimer() {
        return getTimerContextForEndpointOperation(
                MetricEndpoint.GET_DELIVERY_STATS, MetricOperation.PROCESSING_TIME);
    }

    public Optional<Timer.Context> getDeliveryReportLinesPerformanceTimer() {
        return getTimerContextForEndpointOperation(
                MetricEndpoint.GET_LINE_ITEMS, MetricOperation.PROCESSING_TIME);
    }

    public Optional<Timer.Context> postDeliveryReportPerformanceTimer() {
        return getTimerContextForEndpointOperation(
                MetricEndpoint.POST_DELIVERY_REPORT, MetricOperation.PROCESSING_TIME);
    }

    public Optional<Timer.Context> getTokenSpendPerformanceTimer() {
        return getTimerContextForEndpointOperation(
                MetricEndpoint.GET_TOKEN_SPEND_SUMMARY, MetricOperation.PROCESSING_TIME);
    }

    public Optional<Timer.Context> getRecreateLineItemSummaryTimer() {
        return getTimerContextForEndpointOperation(
                MetricEndpoint.RECREATE_LINE_ITEM_SUMMARY, MetricOperation.PROCESSING_TIME);
    }

    /**
     * Scheduled Event Metrics
     */

    public Optional<Timer.Context> scheduledAggregationTimer() {
        return getTimerContextForEndpointOperation(
                MetricEndpoint.POST_DELIVERY_REPORT, MetricOperation.PROCESSING_TIME);
    }

    public Optional<Timer.Context> scheduledDeliverySummaryTimer() {
        return getTimerContextForEndpointOperation(
                MetricEndpoint.POST_DELIVERY_SUMMARY, MetricOperation.PROCESSING_TIME);
    }

    /**
     * Data Access Timer Metrics
     */
    public Optional<Timer.Context> repositoryFetchDeliveryReportsSummaryTimer() {
        return getTimerContextForEndpointOperation(MetricEndpoint.POST_DELIVERY_SUMMARY, MetricOperation.FETCH);
    }

    public Optional<Timer.Context> repositoryFetchDeliveryReportLinesTimer() {
        return getTimerContextForEndpointOperation(MetricEndpoint.POST_DELIVERY_REPORT, MetricOperation.FETCH);
    }

    public Optional<Timer.Context> repositoryStoreDeliveryReportLinesTimer() {
        return getTimerContextForEndpointOperation(MetricEndpoint.POST_DELIVERY_REPORT, MetricOperation.STORE);
    }

    public Optional<Timer.Context> repositoryFetchTokenSpendTimer() {
        return getTimerContextForEndpointOperation(MetricEndpoint.TIMER_AGGREGATE, MetricOperation.FETCH);
    }

    public Optional<Timer.Context> repositoryStoreTokenSpendTimer() {
        return getTimerContextForEndpointOperation(MetricEndpoint.TIMER_AGGREGATE, MetricOperation.STORE);
    }

    /**
     * Helper functions
     */

    private void markMeterForRequests(final MetricService service, final MetricEndpoint endpoint) {
        markMeterForTag(
                service, endpoint, MetricOperation.REQUEST, null, MetricTag.SERVICE_ENDPOINT_OPERATION, 1
        );
    }

    private void markMeterForItems(final MetricService service,
                                   final MetricOperation operation,
                                   final RepositoryItem item, int inc) {
        markMeterForTag(service, null, operation, item, MetricTag.SERVICE_OPERATION_ITEM, inc);
    }

    private synchronized void markMeterForTag(final MetricService service,
                                 final MetricEndpoint endpoint,
                                 final MetricOperation operation,
                                 final RepositoryItem repositoryItem,
                                 final MetricTag metricTag,
                                 final int increment
    ) {
        getMeterForTag(service, endpoint, operation, repositoryItem, metricTag).inc(increment);
    }

    private Counter getMeterForTag(final MetricService service,
                                   final MetricEndpoint endpoint,
                                   final MetricOperation operation,
                                   final RepositoryItem item,
                                   final MetricTag measurementTag
    ) {
        String meterName = measurementTag.getTag();
        if (service != null) {
            meterName = meterName.replaceAll(SERVICE_PLACEHOLDER, service.name());
        }
        if (endpoint != null) {
            meterName = meterName.replaceAll(ENDPOINT_PLACEHOLDER, endpoint.name());
        }
        if (operation != null) {
            meterName = meterName.replaceAll(OPERATION_PLACEHOLDER, operation.name());
        }
        if (item != null) {
            meterName = meterName.replaceAll(ITEM_PLACEHOLDER, item.name());
        }
        return registry.counter(fixUpEnumName(meterName), ResettingCounter::new);
    }

    private Optional<Timer.Context> getTimerContextForEndpointOperation(
            final MetricEndpoint metricEndpoint,
            final MetricOperation metricOperation
    ) {
        final Timer timer = getTimerForEndpointOperation(metricEndpoint, metricOperation);
        if (timer != null) {
            return Optional.of(timer.time());
        }
        return Optional.empty();
    }

    private Timer getTimerForEndpointOperation(
            final MetricEndpoint metricEndpoint,
            final MetricOperation metricOperation) {
        String service = fixUpEnumName(metricEndpoint.name());
        return registry.timer(MetricTag.SERVICE_OPERATION_LATENCY.getTag()
                .replaceAll(SERVICE_PLACEHOLDER, service)
                .replaceAll(OPERATION_PLACEHOLDER, fixUpEnumName(metricOperation.name())));
    }

    private static String fixUpEnumName(String name) {
        return name.toLowerCase().replace("_", "-");
    }
}
