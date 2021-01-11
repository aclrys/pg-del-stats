package org.prebid.pg.delstats.services;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.alerts.AlertName;
import org.prebid.pg.delstats.alerts.AlertPriority;
import org.prebid.pg.delstats.alerts.AlertProxyHttpClient;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.exception.InvalidLineItemStatusException;
import org.prebid.pg.delstats.metrics.GraphiteMetricsRecorder;
import org.prebid.pg.delstats.metrics.SimplisticExpiringSet;
import org.prebid.pg.delstats.model.dto.DeliveryReportFromPbsDto;
import org.prebid.pg.delstats.model.dto.DeliveryReportLineDto;
import org.prebid.pg.delstats.model.dto.DeliveryReportLineStatsDto;
import org.prebid.pg.delstats.model.dto.DeliveryReportToPlannerAdapterDto;
import org.prebid.pg.delstats.model.dto.IntervalDeliveryReport;
import org.prebid.pg.delstats.model.dto.LineItemStatus;
import org.prebid.pg.delstats.model.dto.LineItemSummaryReport;
import org.prebid.pg.delstats.model.dto.Tracer;
import org.prebid.pg.delstats.persistence.DeliveryReport;
import org.prebid.pg.delstats.repository.DeliveryProgressReportsRepository;
import org.prebid.pg.delstats.utils.TimestampUtils;
import org.prebid.pg.delstats.utils.TracerUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Provides services for processing or fetching the Delivery Progress Report Endpoints
 */
@Slf4j
@Service
public class DeliveryReportsDataService {

    private final DeliveryProgressReportsRepository deliveryProgressReportsRepository;

    private final DeliveryReportProcessor deliveryReportProcessor;

    private final ServerConfiguration configuration;

    private final GraphiteMetricsRecorder recorder;

    private final ObjectMapper objectMapper;

    private final Tracer tracer;

    private final AlertProxyHttpClient alertProxyHttpClient;

    private SimplisticExpiringSet<String> recentPbsInstancesSeen;

    private static Map<String, Comparator<DeliveryReportLineStatsDto>> deliveryStatsComparatorMapping;

    private Map<String, String> bidderAliasMaps;

    static {
        deliveryStatsComparatorMapping = new HashMap<>();
        deliveryStatsComparatorMapping.put("biddercode", DeliveryReportLineStatsDto.COMPARATOR_BY_BIDDER_CODE);
        deliveryStatsComparatorMapping.put("lineitemid", DeliveryReportLineStatsDto.COMPARATOR_BY_LINE_ITEM_ID);
        deliveryStatsComparatorMapping.put("biddercode,lineitemid",
                DeliveryReportLineStatsDto.COMPARATOR_BY_BIDDER_CODE_LINE_ITEM_ID);
        deliveryStatsComparatorMapping.put("lineitemid,biddercode",
                DeliveryReportLineStatsDto.COMPARATOR_BY_LINE_ITEM_ID_BIDDER_CODE);
    }

    public DeliveryReportsDataService(
            DeliveryProgressReportsRepository deliveryProgressReportsRepository,
            DeliveryReportProcessor deliveryReportProcessor,
            ServerConfiguration configuration, SystemService systemService) {
        this.deliveryProgressReportsRepository = deliveryProgressReportsRepository;
        this.deliveryReportProcessor = deliveryReportProcessor;
        this.configuration = configuration;
        this.recorder = systemService.getRecorder();
        this.objectMapper = systemService.getObjectMapper();
        this.tracer = systemService.getTracer();
        this.alertProxyHttpClient = systemService.getAlertProxyHttpClient();
        this.recorder.registerUniquePBSInstanceGauge(() -> recentPbsInstancesSeen.size());
        this.bidderAliasMaps = new HashMap<>();
        if (!StringUtils.isEmpty(configuration.getBidderAliasMappingString())) {
            String[] bidderAliasMappings = configuration.getBidderAliasMappingString().split(",");
            Arrays.stream(bidderAliasMappings).forEach(bidderAliasMapEntry -> {
                String[] bidderAliasMap = bidderAliasMapEntry.split(":");
                if (bidderAliasMap.length == 2) {
                    this.bidderAliasMaps.put(bidderAliasMap[0], bidderAliasMap[1]);
                }
            });
        }
        this.recentPbsInstancesSeen = new SimplisticExpiringSet<>();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @PostConstruct
    public void init() {
        this.recentPbsInstancesSeen = new SimplisticExpiringSet<>(
                configuration.getDeliveryReportInstanceNameCacheExpirationAmount(),
                configuration.getDeliveryReportInstanceNameCacheExpirationUnit());
    }

    /**
     * Store a Delivery Progress Report using the current system clock time for the report timestamp field.
     *
     * @param deliveryReportFromPbsDto
     */
    public void storeReport(DeliveryReportFromPbsDto deliveryReportFromPbsDto) {
        doStoreReport(deliveryReportFromPbsDto, Timestamp.from(Instant.now()));
    }

    /**
     * Store a Delivery Progress Report using a timestamp provided by an external source (used in Simulation mode).
     *
     * @param deliveryReportFromPbsDto
     * @param nowString
     * @param nowParamName
     */
    public void storeReport(DeliveryReportFromPbsDto deliveryReportFromPbsDto, String nowString, String nowParamName) {
        Timestamp now = TimestampUtils.convertStringTimeToTimestamp(nowString, nowParamName);
        doStoreReport(deliveryReportFromPbsDto, now);
    }

    private void doStoreReport(DeliveryReportFromPbsDto deliveryReportFromPbsDto, Timestamp now) {
        if (deliveryReportFromPbsDto.getLineItemStatus() == null
                || deliveryReportFromPbsDto.getLineItemStatus().isEmpty()) {
            log.warn("DeliveryReport {} contained no line item status entries", deliveryReportFromPbsDto.getReportId());
            return;
        }

        Optional<Timer.Context> optionalContext = recorder.repositoryStoreDeliveryReportLinesTimer();
        Instant start = Instant.now();
        boolean rc = false;
        try {
            storeLines(deliveryReportFromPbsDto, now);
            recorder.markDeliveryReportRecordsStored(deliveryReportFromPbsDto.getLineItemStatus().size());
            rc = true;
        } finally {
            optionalContext.ifPresent(Timer.Context::stop);
            long timeSpent = Duration.between(start, Instant.now()).toMillis();
            String results = rc ? "Stored" : "Rejected";
            int records = deliveryReportFromPbsDto.getLineItemStatus().size();
            TracerUtils.logIfActive(log, tracer, String.format("%s %d reports in %d ms", results, records, timeSpent));
        }
    }

    void storeLines(DeliveryReportFromPbsDto deliveryReportFromPbsDto, Timestamp now) {
        if (deliveryReportFromPbsDto.getLineItemStatus() == null) {
            log.info("No delivery report summaries in request");
            return;
        }
        deliveryReportProcessor.validateDeliveryReport(deliveryReportFromPbsDto);

        List<DeliveryReport> deliveryReports = new LinkedList<>();
        List<Exception> capturedExceptions = new LinkedList<>();

        for (final JsonNode lineItemStatusJson : deliveryReportFromPbsDto.getLineItemStatus()) {
            deliveryReports.addAll(deliveryReportProcessor.processLineItemStatus(deliveryReportFromPbsDto,
                    lineItemStatusJson, now, capturedExceptions));
            recentPbsInstancesSeen.add(deliveryReportFromPbsDto.getInstanceId());
        }

        if (!capturedExceptions.isEmpty()) {
            recorder.markInvalidRequestMeter();
        }
        if (deliveryReports.isEmpty()) {
            log.info("No {} line items summaries in delivery report", capturedExceptions.isEmpty() ? "" : "valid");
            return;
        }

        storeLinesInDB(deliveryReportFromPbsDto, deliveryReports, capturedExceptions);
    }

    @Transactional
    void storeLinesInDB(DeliveryReportFromPbsDto deliveryReportFromPbsDto,
                        List<DeliveryReport> deliveryReports,
                        List<Exception> capturedExceptions) {
        try {
            String source = String.format("%s|%s|%s", deliveryReportFromPbsDto.getVendor(),
                    deliveryReportFromPbsDto.getRegion(), deliveryReportFromPbsDto.getInstanceId());
            if (capturedExceptions.isEmpty()) {
                log.info("Saving all {} delivery report line item summaries in Delivery Report {} from {}",
                        deliveryReports.size(), deliveryReportFromPbsDto.getReportId(), source);
            } else {
                log.info("Delivery Report {} saving only {} delivery report line item summaries of {} from {}",
                        deliveryReports.size(), deliveryReportFromPbsDto.getReportId(),
                        deliveryReportFromPbsDto.getLineItemStatus().size(), source);
            }
            List<DeliveryReport> results = deliveryProgressReportsRepository.saveAll(deliveryReports);
            log.info("Saved {} Delivery Report(s) {} from {}.",
                    results.size(), deliveryReportFromPbsDto.getReportId(), source);
        } catch (DataIntegrityViolationException ex) {
            // Ignoring these errors as they indicate the same report was previously delivered.
            log.warn("DB Problem storing delivery report::{}", ex.getMessage());
        } catch (DataAccessException ex) {
            String msg = String.format("DB Problem storing delivery report::%s", ex.getMessage());
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.STORE_ERROR, msg,
                    AlertPriority.HIGH, ex);
            // If call stack threw these types of exceptions, we don't want to swallow it with the catchall below
            recorder.markInvalidRequestMeter();
            throw ex;
        } catch (Exception ex) {
            String msg = "Unexpected problem in storing delivery report";
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.STORE_ERROR, msg,
                    AlertPriority.HIGH, ex);
            recorder.markExceptionMeter(ex);
            throw ex;
        }
    }

    /**
     * Fetch Delivery Progress Reports from the DB using the parameters provided as filters.
     *
     * @param bidderCode
     * @param startTime
     * @param endTime
     * @return
     */
    public DeliveryReportToPlannerAdapterDto retrieveByBidderCode(String bidderCode,
                                                                  String startTime,
                                                                  String endTime) {
        if (this.bidderAliasMaps.containsKey(bidderCode)) {
            String prevBidderCode = bidderCode;
            bidderCode = this.bidderAliasMaps.get(bidderCode);
            log.info("Switching requested bidderCode {} to alias of {}", prevBidderCode, bidderCode);
        }
        final Timestamp startTimestamp = TimestampUtils.convertStringTimeToTimestamp(startTime,
                configuration.getGetDeliveryReportStartSincePeriodSeconds(), "startTime");
        final Timestamp endTimestamp = TimestampUtils.convertStringTimeToTimestamp(endTime,
                configuration.getGetDeliveryReportEndSincePeriodSeconds(), "endTime");
        final Instant startInstant = startTimestamp.toInstant();
        final Instant endInstant = endTimestamp.toInstant();

        log.info(
                "Looking for Delivery Progress Reports from {} through {} for bidderCode={}",
                startTimestamp, endTimestamp, bidderCode
        );

        final List<DeliveryReportLineDto> deliveryReportList = new ArrayList<>();
        Optional<Timer.Context> optionalContext = recorder.repositoryFetchDeliveryReportLinesTimer();
        // Break down queries into minute-level queries for performance with a lot of lines from many PBS
        final long requestRangeMinutes = Duration.between(startInstant, endInstant).toMinutes();
        for (int fetchMinute = 0; fetchMinute <= requestRangeMinutes; fetchMinute++) {
            Instant fetchStart = startInstant.plusSeconds(fetchMinute * 60L);
            Instant fetchEnd = (fetchMinute < requestRangeMinutes) ? fetchStart.plusSeconds(60) : endInstant;
            if (!fetchStart.isBefore(fetchEnd)) {
                continue;
            }
            deliveryReportList.addAll(fetchDeliveryReports(bidderCode,
                    Timestamp.from(fetchStart), Timestamp.from(fetchEnd), configuration.getDeliveryReportsPageSize()));
        }

        optionalContext.ifPresent(Timer.Context::stop);
        recorder.markDeliveryReportRecordsFetched(deliveryReportList.size());

        return DeliveryReportToPlannerAdapterDto.builder()
                    .reportId(UUID.randomUUID().toString())
                    .reportTimeStamp(Timestamp.from(Instant.now()))
                    .dataWindowStartTimeStamp(startTimestamp)
                    .dataWindowEndTimeStamp(endTimestamp)
                    .deliveryReports(deliveryReportList)
                    .build();
    }

    private List<DeliveryReportLineDto> fetchDeliveryReports(String bidderCode, Timestamp startTimestamp,
                                                             Timestamp endTimestamp, int pageSize) {
        Pageable page = PageRequest.of(0, pageSize);
        ArrayList<DeliveryReportLineDto> deliveryReportList = new ArrayList<>();
        while (true) {
            Page<DeliveryReport> deliveryReportPage = deliveryProgressReportsRepository
                    .findByBidderCodeAndReportTimestampRange(
                            bidderCode, startTimestamp, endTimestamp, page);
            if (deliveryReportPage.hasContent()) {
                for (DeliveryReport deliveryReport : deliveryReportPage.getContent()) {
                    deliveryReportList.add(mapDeliveryReportToDeliveryReportLine(deliveryReport));
                }
            }
            if (deliveryReportPage.hasNext()) {
                page = deliveryReportPage.nextPageable();
            } else {
                break;
            }
        }
        return deliveryReportList;
    }

    DeliveryReportLineDto mapDeliveryReportToDeliveryReportLine(DeliveryReport deliveryReport) {
        try {
            return DeliveryReportLineDto.builder()
                    .vendor(deliveryReport.getVendor())
                    .region(deliveryReport.getRegion())
                    .instanceId(deliveryReport.getInstanceId())
                    .bidderCode(deliveryReport.getBidderCode())
                    .lineItemId(deliveryReport.getLineItemId())
                    .dataWindowStartTimestamp(deliveryReport.getDataWindowStartTimestamp())
                    .dataWindowEndTimestamp(deliveryReport.getDataWindowEndTimestamp())
                    .reportId(deliveryReport.getReportId())
                    .reportTimestamp(deliveryReport.getReportTimestamp())
                    .lineItemStatus(deliveryReport.getLineItemStatus())
                    .build();
        } catch (Exception ex) {
            String msg =
                    "mapDeliveryReportToDeliveryReportLine::Invalid JSON in lineItemStatus in deliveryReport object";
            alertProxyHttpClient.raiseEventForExceptionAndLog(
                    AlertName.ERROR, msg, AlertPriority.HIGH, ex, deliveryReport.getLineItemStatus()
            );
            recorder.markExceptionMeter(ex);
            return DeliveryReportLineDto.builder().build();
        }
    }

    public List<DeliveryReport> retrieveLineItemSummaries(String lineItemIds, Instant startTime, Instant endTime) {
        Optional<Timer.Context> optionalContext = recorder.repositoryFetchDeliveryReportLinesTimer();
        List<DeliveryReport> reports = new ArrayList<>();
        Pageable page = PageRequest.of(0, configuration.getLineItemSummaryPagerSize(),
                Sort.by("data_window_end_timestamp").ascending());
        try {
            reports = LineItemSummaryReport.retrieveLineItemSummaries(lineItemIds, startTime, endTime, page,
                    this::findDeliveryReports);
        } catch (Exception e) {
            String msg = "getLineItemSummaries::Unexpected exception";
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.ERROR, msg, AlertPriority.HIGH, e);
        } finally {
            optionalContext.ifPresent(Timer.Context::stop);
        }
        recorder.markDeliveryReportRecordsFetched(reports.size());
        return reports;
    }

    LineItemStatus mapToLineItemStatus(String lineStatus) {
        try {
            return objectMapper.readValue(lineStatus, LineItemStatus.class);
        } catch (Exception ex) {
            String msg = "getLineItemSummaries::Invalid JSON in lineItemStatus in deliveryReport object";
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.ERROR, msg,
                    AlertPriority.HIGH, ex, lineStatus);
            recorder.markExceptionMeter(ex);
            throw new InvalidLineItemStatusException(msg, ex);
        }
    }

    public List<List<IntervalDeliveryReport>> groupByIntervals(
            List<DeliveryReport> reports, int interval, Instant startTime, Instant endTime) {
        List<List<IntervalDeliveryReport>> intervalReports = new ArrayList<>();
        if (reports.isEmpty()) {
            return intervalReports;
        }

        Instant intervalStart = startTime;
        Instant intervalEnd = startTime.plus(interval, ChronoUnit.MINUTES);
        int index = 0;
        while (intervalStart.isBefore(endTime) && index < reports.size()) {
            List<IntervalDeliveryReport> intervalReport = new ArrayList<>();
            intervalReports.add(intervalReport);
            while (index < reports.size()) {
                DeliveryReport report = reports.get(index);
                if (withinDuration(report, intervalStart, intervalEnd)) {
                    intervalReport.add(IntervalDeliveryReport.builder()
                            .deliveryReport(report)
                            .reportWindowStartTimestamp(intervalStart)
                            .reportWindowEndTimestamp(intervalEnd)
                            .build());
                    index++;
                } else {
                    intervalStart = intervalEnd;
                    intervalEnd = intervalStart.plus(interval, ChronoUnit.MINUTES);
                    break;
                }
            }
        }
        return intervalReports;
    }

    private boolean withinDuration(DeliveryReport report, Instant startTime, Instant endTime) {
        Instant end = report.getDataWindowEndTimestamp().toInstant();
        return !end.isBefore(startTime) && end.isBefore(endTime);
    }

    public List<Map<String, Object>> getLineItemSummaryReport(String lineItemIds, Instant startTime,
            Instant endTime, Set<String> metrics, Integer interval, Integer csvStartInterval) {
        List<DeliveryReport> reports = retrieveLineItemSummaries(lineItemIds, startTime, endTime);
        List<List<IntervalDeliveryReport>> intervalReports = groupByIntervals(reports, interval, startTime, endTime);

        List<Map<String, Object>> summaries = new ArrayList<>();
        for (int index = 0; index < intervalReports.size(); index++) {
            if (intervalReports.get(index).isEmpty()) {
                Map<String, Object> emptySummary = new LinkedHashMap<>();
                emptySummary.put(LineItemSummaryReport.INTERVAL, index + csvStartInterval);
                summaries.add(emptySummary);
            } else {
                summaries.addAll(buildLineItemSummaryIntervalReport(
                        intervalReports.get(index), metrics, index + csvStartInterval));
            }
        }
        return summaries;
    }

    List<Map<String, Object>> buildLineItemSummaryIntervalReport(
            Collection<IntervalDeliveryReport> intervalReports, Set<String> metrics, int index) {
        List<Map<String, Object>> summaries = new ArrayList<>();
        Map<String, List<IntervalDeliveryReport>> lineIdToReportMap = intervalReports.stream()
                .collect(Collectors.groupingBy(report -> report.getDeliveryReport().getLineItemId(),
                        TreeMap::new, Collectors.toCollection(LinkedList::new)));
        for (List<IntervalDeliveryReport> deliveryReports : lineIdToReportMap.values()) {
            summarizeReports(metrics, index, summaries, deliveryReports);
        }
        return summaries;
    }

    private void summarizeReports(Set<String> metrics, int index,
                                  List<Map<String, Object>> summaries, List<IntervalDeliveryReport> deliveryReports) {
        Map<String, Object> summaryMap = new HashMap<>();
        IntervalDeliveryReport intervalReport0 = deliveryReports.get(0);
        DeliveryReport report0 = intervalReport0.getDeliveryReport();
        Timestamp dataWindowStart = report0.getDataWindowStartTimestamp();
        Timestamp dataWindowEnd = report0.getDataWindowEndTimestamp();
        for (int i = 0; i < deliveryReports.size(); i++) {
            IntervalDeliveryReport intervalReport = deliveryReports.get(i);
            DeliveryReport report = intervalReport.getDeliveryReport();
            LineItemStatus item = mapToLineItemStatus(report.getLineItemStatus());
            if (report.getDataWindowStartTimestamp().before(dataWindowStart)) {
                dataWindowStart = report.getDataWindowStartTimestamp();
            }
            if (report.getDataWindowEndTimestamp().after(dataWindowEnd)) {
                dataWindowEnd = report.getDataWindowEndTimestamp();
            }
            for (String field : metrics) {
                LineItemSummaryReport.accumulateSimpleField(summaryMap, field,
                        LineItemSummaryReport.getMetricsFunctionMap(), item);
            }
            if (i == 0) {
                summaryMap.put(LineItemSummaryReport.LINE_ITEM_ID, item.getLineItemId());
                summaryMap.put(LineItemSummaryReport.EXT_LINE_ITEM_ID, item.getExtLineItemId());
            }
        }
        summaryMap.put(LineItemSummaryReport.INTERVAL, index);
        summaryMap.put(LineItemSummaryReport.DATA_WINDOW_START_TIMESTAMP, dataWindowStart);
        summaryMap.put(LineItemSummaryReport.DATA_WINDOW_END_TIMESTAMP, dataWindowEnd);
        summaries.add(summaryMap);
    }

    private Page<DeliveryReport> findDeliveryReports(List<String> lineIds, Instant startTime, Instant endTime,
            Pageable page) {
        return lineIds.isEmpty()
                ? deliveryProgressReportsRepository.getLineItemReportsWithTimeRange(startTime, endTime, page)
                : deliveryProgressReportsRepository.getLineItemReportsByLineIdsWithTimeRange(
                lineIds, startTime, endTime, page);
    }
}
