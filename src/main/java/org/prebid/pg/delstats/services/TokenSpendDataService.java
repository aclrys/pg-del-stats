package org.prebid.pg.delstats.services;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.alerts.AlertName;
import org.prebid.pg.delstats.alerts.AlertPriority;
import org.prebid.pg.delstats.alerts.AlertProxyHttpClient;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.exception.DeliveryReportProcessingException;
import org.prebid.pg.delstats.metrics.GraphiteMetricsRecorder;
import org.prebid.pg.delstats.model.dto.Shutdown;
import org.prebid.pg.delstats.model.dto.TokenSpendSummaryDto;
import org.prebid.pg.delstats.model.dto.TokenSpendSummaryLineDto;
import org.prebid.pg.delstats.model.dto.Tracer;
import org.prebid.pg.delstats.persistence.LatestTokenSpendSummary;
import org.prebid.pg.delstats.persistence.SystemState;
import org.prebid.pg.delstats.repository.DeliveryProgressReportsRepository;
import org.prebid.pg.delstats.repository.LatestTokenSpendSummaryConstants;
import org.prebid.pg.delstats.repository.LatestTokenSpendSummaryRepository;
import org.prebid.pg.delstats.repository.SystemStateConstants;
import org.prebid.pg.delstats.repository.SystemStateRepository;
import org.prebid.pg.delstats.utils.StreamHelper;
import org.prebid.pg.delstats.utils.TimestampUtils;
import org.prebid.pg.delstats.utils.TracerUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TokenSpendDataService {

    private final LatestTokenSpendSummaryRepository latestTokenSpendSummaryRepository;

    private final DeliveryProgressReportsRepository deliveryReportSummariesRepository;

    private final SystemStateRepository systemStateRepository;

    private final SystemService systemService;

    private final ServerConfiguration configuration;

    private final GraphiteMetricsRecorder recorder;

    private final ObjectMapper objectMapper;

    private final Tracer tracer;

    private final Shutdown shutdown;

    private final AlertProxyHttpClient alertProxyHttpClient;

    public TokenSpendDataService(LatestTokenSpendSummaryRepository latestTokenSpendSummaryRepository,
                                 DeliveryProgressReportsRepository deliveryReportSummariesRepository,
                                 SystemStateRepository systemStateRepository,
                                 ServerConfiguration configuration,
                                 SystemService systemService) {
        this.latestTokenSpendSummaryRepository = latestTokenSpendSummaryRepository;
        this.deliveryReportSummariesRepository = deliveryReportSummariesRepository;
        this.systemStateRepository = systemStateRepository;
        this.configuration = configuration;
        this.systemService = systemService;
        this.recorder = systemService.getRecorder();
        this.objectMapper = systemService.getObjectMapper();
        this.tracer = systemService.getTracer();
        this.shutdown = systemService.getShutdown();
        this.alertProxyHttpClient = systemService.getAlertProxyHttpClient();
    }

    public static boolean hasRequiredFields(LatestTokenSpendSummary latestTokenSpendSummary) {
        if (latestTokenSpendSummary == null) {
            log.info("Dropping Latest Token Spend Summary: is null");
            return false;
        }
        if (StringUtils.isEmpty(latestTokenSpendSummary.getVendor())) {
            log.info("Dropping Latest Token Spend Summary: no vendor");
            return false;
        }
        if (StringUtils.isEmpty(latestTokenSpendSummary.getRegion())) {
            log.info("Dropping Latest Token Spend Summary: no region");
            return false;
        }
        if (StringUtils.isEmpty(latestTokenSpendSummary.getInstanceId())) {
            log.info("Dropping Latest Token Spend Summary: no instance");
            return false;
        }
        if (StringUtils.isEmpty(latestTokenSpendSummary.getBidderCode())) {
            log.info("Dropping Latest Token Spend Summary: no bidderCode");
            return false;
        }
        if (StringUtils.isEmpty(latestTokenSpendSummary.getLineItemId())) {
            log.info("Dropping Latest Token Spend Summary: no line item id");
            return false;
        }
        return true;
    }

    /**
     * Performs a scheduled aggregation operation on Delivery Progress Reports received since last scheduled run.
     * Note: Operation is transactional at this outer level to ensure 'last scheduled run' does not advance in case
     * of a rollback caused by an error in a lower level write.
     * Futher note: This implementation may need to change to properly perform batch inserts of records.
     */
    @Scheduled(
            fixedDelayString = "${services.token-aggr.refresh-period-sec}000",
            initialDelayString = "${services.token-aggr.initial-delay-sec}000"
    )
    @Transactional
    public void aggregate() {
        if (shutdown.isInitiating()) {
            log.info("TokenSpendDataService::System is shutting down");
            return;
        }
        if (!configuration.isAggregationEnabled()) {
            log.debug("Periodic aggregation is disabled");
            return;
        }
        recorder.markScheduledAggregation();
        Optional<Timer.Context> optionalContext = recorder.scheduledAggregationTimer();
        try {
            final Timestamp startAggregateTimestamp = determineAggregationStartTime();
            final Timestamp endAggregateTimestamp = Timestamp.from(Instant.now());
            TracerUtils.traceAsInfoOrlogAsInfo(
                    log, tracer, String.format("Running scheduled token aggregation from %s to %s",
                    startAggregateTimestamp, endAggregateTimestamp));
            doAggregation(startAggregateTimestamp, endAggregateTimestamp);
            systemStateRepository.store(SystemStateConstants.SYSTEM_STATE_TAG_LAST_SUMMARY_REPORT,
                    endAggregateTimestamp.toString());
        } catch (Exception e) {
            String msg = "Unexpected problem in aggregating token spend summaries";
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.TOKEN_SUMMARY_AGGR_ERROR, msg,
                    AlertPriority.HIGH, e);
        } finally {
            long nanosToComplete = optionalContext.map(Timer.Context::stop).orElse(0L);
            TracerUtils.traceAsInfoOrlogAsInfo(
                    log, tracer, String.format("Completed scheduled token aggregation in %d ms",
                            nanosToComplete / 1_000_000));
        }
    }

    /**
     * Used by Simulation which provides non-clock based start and end times to match 'Simulation Time'.
     *
     * @param startTime
     * @param endTime
     * @param startParamName
     * @param endParamName
     */
    @Transactional
    public void aggregate(String startTime, String endTime, String startParamName, String endParamName) {
        final Timestamp startAggregateTimestamp = TimestampUtils
                .convertStringTimeToTimestamp(startTime, startParamName);
        final Timestamp endAggregateTimestamp = TimestampUtils
                .convertStringTimeToTimestamp(endTime, endParamName);
        doAggregation(startAggregateTimestamp, endAggregateTimestamp);
    }

    public Timestamp determineAggregationStartTime() {
        final SystemState lastSummaryReport =
                systemStateRepository.retrieveByTag(SystemStateConstants.SYSTEM_STATE_TAG_LAST_SUMMARY_REPORT);
        final Timestamp maxLookbackTimestamp = TimestampUtils.secondsFromNow(
                configuration.getMaxLookBack());
        final Timestamp startAggregateTimestamp = (lastSummaryReport == null)
                ? maxLookbackTimestamp
                : TimestampUtils.convertStringTimeToTimestamp(lastSummaryReport.getVal(),
                SystemStateConstants.SYSTEM_STATE_TAG_LAST_SUMMARY_REPORT);
        return startAggregateTimestamp.before(maxLookbackTimestamp) ? maxLookbackTimestamp : startAggregateTimestamp;

    }

    void doAggregation(Timestamp startAggregateTimestamp, Timestamp endAggregateTimestamp) {
        try {
            doAggregationInDB(startAggregateTimestamp, endAggregateTimestamp);
        } catch (DeliveryReportProcessingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Caught exception performing aggregation in db", e);
            throw new DeliveryReportProcessingException(e.getMessage());
        }
    }

    void doAggregationInDB(Timestamp startAggregateTimestamp, Timestamp endAggregateTimestamp) {
        List<Object> distinctVendorRegion = deliveryReportSummariesRepository
                .getDistinctVendorRegion(startAggregateTimestamp, endAggregateTimestamp);
        TracerUtils.traceAsInfoOrlogAsInfo(log, tracer, String.format("Found %d vendor/regions for aggregation.",
                distinctVendorRegion.size()));
        if (distinctVendorRegion.isEmpty()) {
            TracerUtils.logIfActive(log, tracer, "No reports found for aggregation at this time");
            return;
        }

        TracerUtils.logIfActive(log, tracer,
                String.format("Found %d distinct vendor and region combinations to be aggregated.",
                        distinctVendorRegion.size()));
        int totalUpdatedCount = 0;
        for (Object vendorRegionRecord : distinctVendorRegion) {
            VendorRegion vendorRegion = processVendorRegionRecord(vendorRegionRecord);
            if (vendorRegion == null) {
                throw new DeliveryReportProcessingException("Unable to process aggregation");
            }
            TracerUtils.traceAsInfoOrlogAsInfo(log, tracer, String.format("Processing vendor/region: %s/%s",
                    vendorRegion.vendor, vendorRegion.region));
            log.info(LatestTokenSpendSummaryConstants.LATEST_TOKEN_SPEND_UPSERT_TOKEN_MATCH_COUNT_SQL);
            int updatedCount = latestTokenSpendSummaryRepository.upsertTokenSummaries(vendorRegion.vendor,
                    vendorRegion.region, startAggregateTimestamp, endAggregateTimestamp);
            totalUpdatedCount += updatedCount;
            TracerUtils.traceAsInfoOrlogAsInfo(log, tracer, String.format("Updated %d token summaries for "
                    + "vendor/region: %s/%s", updatedCount, vendorRegion.vendor, vendorRegion.region));
        }
        recorder.markLatestTokenSpendSummariesStored(totalUpdatedCount);
    }

    VendorRegion processVendorRegionRecord(Object vendorRegionRecord) {
        if (!(vendorRegionRecord instanceof Object[])) {
            log.error("Invalid results from query to retrieve vendor and region in delivery reports");
            return null;
        }
        Object[] vendorRegionColumns = (Object[]) vendorRegionRecord;
        if (vendorRegionColumns.length != 2) {
            log.error("Invalid number of columns ({}) from query to retrieve vendor and region in delivery reports",
                    vendorRegionColumns.length);
            return null;
        }
        if (!(vendorRegionColumns[0] instanceof String)) {
            log.error("Invalid vendor field from query to retrieve vendor and region in delivery reports");
            return null;
        }
        if (!(vendorRegionColumns[1] instanceof String)) {
            log.error("Invalid region field from query to retrieve vendor and region in delivery reports");
            return null;
        }
        String vendor = (String) vendorRegionColumns[0];
        String region = (String) vendorRegionColumns[1];
        return new VendorRegion(vendor, region);
    }

    public TokenSpendSummaryDto getTokenSpendSummary(String since, String vendor, String region) {
        return getTokenSpendSummary(since, vendor, region, null);
    }

    public TokenSpendSummaryDto getTokenSpendSummary(String since, String vendor, String region, String useAsNow) {
        Instant now = Instant.now();
        if (!StringUtils.isEmpty(useAsNow)) {
            now = TimestampUtils.convertStringTimeToTimestamp(useAsNow, "now").toInstant();
        }
        final Timestamp sinceTimestamp = StringUtils.isEmpty(since)
                ? TimestampUtils.secondsFromThen(configuration.getGetTokenSpendReportSincePeriodSeconds(), now)
                : TimestampUtils.convertStringTimeToTimestamp(since, "since");
        log.info("getTokenSpendSummary since report_timestamp = {}", sinceTimestamp);
        return doGetTokenSpendSummaryDto(sinceTimestamp, vendor, region);
    }

    TokenSpendSummaryDto doGetTokenSpendSummaryDto(Timestamp sinceTimestamp, String vendor, String region) {
        Optional<Timer.Context> optionalContext = recorder.repositoryFetchTokenSpendTimer();
        try {
            final List<LatestTokenSpendSummary> latestTokenSpendSummaries = (vendor == null)
                    ? latestTokenSpendSummaryRepository.retrieveSince(sinceTimestamp)
                    : latestTokenSpendSummaryRepository.retrieveSinceByVednorRegion(sinceTimestamp, vendor, region);

            recorder.markLatestTokenSpendSummariesFetched(latestTokenSpendSummaries.size());

            return TokenSpendSummaryDto.builder()
                    .tokenSpendSummaryLines(StreamHelper.getStream(latestTokenSpendSummaries)
                            .map(this::mapTokenSpendSummaryLineDtoFromLatestTokenSpendSummary)
                            .collect(Collectors.toList())
                    ).build();
        } catch (Exception ex) {
            String msg = "doGetTokenSpendSummaryDto::Exception in getting token spend summary";
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.JSON_ERROR, msg, AlertPriority.HIGH, ex);
            recorder.markExceptionMeter(ex);
            return TokenSpendSummaryDto.builder().build();
        } finally {
            optionalContext.ifPresent(Timer.Context::close);
        }
    }

    TokenSpendSummaryLineDto mapTokenSpendSummaryLineDtoFromLatestTokenSpendSummary(
            LatestTokenSpendSummary tokenSpendSummary
    ) {
        try {
            TracerUtils.logIfActiveMatchingOnVendorRegion(log, tracer, tokenSpendSummary);
            return TokenSpendSummaryLineDto.builder()
                    .reportId(UUID.randomUUID().toString())
                    .vendor(tokenSpendSummary.getVendor())
                    .region(tokenSpendSummary.getRegion())
                    .instanceId(tokenSpendSummary.getInstanceId())
                    .serviceInstanceId(systemService.getServiceInstanceId())
                    .bidderCode(tokenSpendSummary.getBidderCode())
                    .lineItemId(tokenSpendSummary.getLineItemId())
                    .extLineItemId(tokenSpendSummary.getExtLineItemId())
                    .dataWindowStartTimestamp(tokenSpendSummary.getDataWindowStartTimestamp())
                    .dataWindowEndTimestamp(tokenSpendSummary.getDataWindowEndTimestamp())
                    .reportTimestamp(tokenSpendSummary.getReportTimestamp())
                    .summaryData(objectMapper.readTree(tokenSpendSummary.getSummaryData()))
                    .build();

        } catch (Exception e) {
            recorder.markInvalidRequestMeter();
            String msg = "mapTokenSpendSummaryLineDtoFromLatestTokenSpendSummary::Invalid JSON";
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.JSON_ERROR, msg, AlertPriority.HIGH, e);
            return TokenSpendSummaryLineDto.builder().build();
        }
    }

    private class VendorRegion {
        String vendor;
        String region;

        VendorRegion(String vendor, String region) {
            this.vendor = vendor;
            this.region = region;
        }
    }
}
