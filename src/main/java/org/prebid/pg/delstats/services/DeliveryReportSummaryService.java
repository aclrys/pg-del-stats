package org.prebid.pg.delstats.services;

import com.codahale.metrics.Timer;
import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.alerts.AlertName;
import org.prebid.pg.delstats.alerts.AlertPriority;
import org.prebid.pg.delstats.alerts.AlertProxyHttpClient;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.exception.DeliveryReportProcessingException;
import org.prebid.pg.delstats.exception.InvalidRequestException;
import org.prebid.pg.delstats.metrics.GraphiteMetricsRecorder;
import org.prebid.pg.delstats.model.dto.DeliveryReportSummaryToPlannerAdapterDto;
import org.prebid.pg.delstats.model.dto.LineItemSummaryReport;
import org.prebid.pg.delstats.model.dto.PlanDataSummary;
import org.prebid.pg.delstats.model.dto.Tracer;
import org.prebid.pg.delstats.persistence.DeliveryReportSummary;
import org.prebid.pg.delstats.persistence.SystemState;
import org.prebid.pg.delstats.repository.DeliveryReportSummaryRepository;
import org.prebid.pg.delstats.repository.SystemStateConstants;
import org.prebid.pg.delstats.repository.SystemStateRepository;
import org.prebid.pg.delstats.utils.TimestampUtils;
import org.prebid.pg.delstats.utils.TracerUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DeliveryReportSummaryService {

    private final DeliveryReportSummaryRepository deliveryReportSummaryRepository;

    private final DeliveryReportsDataService deliveryReportsDataService;

    private final SystemStateRepository systemStateRepository;

    private final ServerConfiguration serverConfiguration;

    private final Tracer tracer;

    private final GraphiteMetricsRecorder recorder;

    private final AlertProxyHttpClient alertProxyHttpClient;

    private final long interval;

    private final String serviceInstanceId;

    public DeliveryReportSummaryService(
            DeliveryReportSummaryRepository deliveryReportSummaryRepository,
            DeliveryReportsDataService deliveryReportsDataService,
            SystemStateRepository systemStateRepository,
            ServerConfiguration serverConfiguration,
            SystemService systemService) {
        this.deliveryReportSummaryRepository = deliveryReportSummaryRepository;
        this.deliveryReportsDataService = deliveryReportsDataService;
        this.serverConfiguration = serverConfiguration;
        this.systemStateRepository = systemStateRepository;
        this.recorder = systemService.getRecorder();
        this.tracer = systemService.getTracer();
        this.alertProxyHttpClient = systemService.getAlertProxyHttpClient();
        this.interval = serverConfiguration.getDeliverySummaryServiceAggregateInterval();
        this.serviceInstanceId = systemService.getServiceInstanceId();
    }

    @Transactional
    public int runDeliveryReportSummary(Timestamp startTime, Timestamp endTime, boolean freshReports) {
        TracerUtils.traceAsInfoOrlogAsInfo(log, tracer, String.format(
                "Creating Delivery Report Summary from %s to %s.", startTime, endTime));
        recorder.markScheduledDeliverySummary();

        Optional<Timer.Context> optionalContext = recorder.scheduledDeliverySummaryTimer();
        int summariesSaved = 0;
        String endTimeStr = endTime.toString();
        int endTimeMinutes = endTime.toInstant().atZone(ZoneOffset.UTC).getMinute();
        try {
            summariesSaved += deliveryReportSummaryRepository.insertLineSummariesDirectly(startTime, endTime);
            String summaryReportTagSuffix = freshReports
                    ? SystemStateConstants.SYSTEM_STATE_TAG_SUMMARY_REPORT_SUFFIX
                    : SystemStateConstants.SYSTEM_STATE_TAG_SUMMARY_REPORT_RECREATE_SUFFIX;
            systemStateRepository.saveDeliverySummaryReportStates(
                    startTime, endTime, serviceInstanceId, endTimeStr, summaryReportTagSuffix);

            String intervalEndFormat = freshReports
                    ? SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY_INTERVAL_END_FORMAT
                    : SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY_RECREATE_INTERVAL_END_FORMAT;
            String tag = String.format(intervalEndFormat, endTimeMinutes);
            systemStateRepository.store(tag, endTimeStr);

            if (freshReports) {
                systemStateRepository.store(SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY, endTimeStr);
            }
            return summariesSaved;
        } catch (Exception e) {
            log.error("Caught exception processing delivery report summaries", e);
            throw new DeliveryReportProcessingException(e.getMessage());
        } finally {
            long nanosToComplete = optionalContext.map(Timer.Context::stop).orElse(0L);
            TracerUtils.traceAsInfoOrlogAsInfo(log, tracer,
                    String.format("Completed scheduled delivery report summary: %d summaries created."
                                    + " for interval: %s - %s in %d ms",
                            summariesSaved, startTime, endTime, nanosToComplete / 1_000_000));
        }
    }

    @Transactional
    public int recreateLineItemSummaryReport(Timestamp startTime, Timestamp endTime, boolean overwrite) {
        if (overwrite) {
            int deleted = deliveryReportSummaryRepository.deleteLineSummaries(startTime, endTime);
            log.info("delete {} rows of Line Item Summary Report.", deleted);
            recorder.markDeliverySummariesDeleted(deleted);
        } else {
            int count = deliveryReportSummaryRepository.countByReportWindow(startTime, endTime);
            if (count > 0) {
                throw new InvalidRequestException(
                        String.format("There are existing summaries, use overwrite=true to overwrite", count));
            }
        }
        return runDeliveryReportSummary(startTime, endTime, false);
    }

    public List<Map<String, Object>> getLineItemSummaryReport(String lineItemIds, Instant startTime, Instant endTime,
            Set<String> metrics, int interval) {
        List<DeliveryReportSummary> reports = tryRetrieveLineItemSummaries(lineItemIds, startTime, endTime);
        recorder.markDeliveryReportsSummariesFetched(reports.size());
        if (reports.isEmpty()) {
            return Collections.emptyList();
        }

        return buildSummaries(lineItemIds, startTime, endTime, metrics, interval, reports);
    }

    public DeliveryReportSummaryToPlannerAdapterDto getDeliverySummaryReport(
            String startTimeString, String endTimeString) {
        final SystemState previousState = systemStateRepository.retrieveByTag(
                SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY);
        return getDeliverySummaryReport(startTimeString, endTimeString,
                previousState != null ? previousState.getVal() : "",
                SystemStateConstants.SYSTEM_STATE_TAG_DELIVERY_SUMMARY);
    }

    public DeliveryReportSummaryToPlannerAdapterDto getDeliverySummaryReport(
            String startTimeString, String endTimeString, String previousStateVal, String source) {

        Instant startTime = (startTimeString != null) ? Instant.parse(startTimeString) : null;
        Instant endTime = (endTimeString != null) ? Instant.parse(endTimeString) : null;

        if (startTime == null || endTime == null) {
            if (StringUtils.isEmpty(previousStateVal)) {
                log.info("No previous time found and no start/end time supplied. Returning no results.");
                return DeliveryReportSummaryToPlannerAdapterDto.builder().build();
            }
            Instant maxEndTime = TimestampUtils.convertStringTimeToTimestamp(previousStateVal, source).toInstant();

            Instant maxStartTime = maxEndTime.minus(interval, ChronoUnit.MINUTES);

            if (startTime == null && endTime == null) {
                startTime = maxStartTime;
                endTime = maxEndTime;
            } else if (endTime == null) {
                endTime = maxEndTime;
            } else {
                startTime = maxEndTime.minus(serverConfiguration.getDeliverySummaryServiceMaxSummaryIntervals()
                        * interval, ChronoUnit.MINUTES);
            }
        }
        startTime = startTime.truncatedTo(ChronoUnit.MINUTES);
        endTime = endTime.truncatedTo(ChronoUnit.MINUTES);

        log.info(
                "Intermediate - Looking for V2 reports from report window start {} through end {}",
                startTime, endTime
        );

        int endTimeMinute = endTime.atZone(ZoneOffset.UTC).getMinute();

        //this is the part to discuss with PA
        if (endTime.minus(interval + (endTimeMinute % interval), ChronoUnit.MINUTES).isBefore(startTime)) {
            startTime = endTime.minus(interval + (endTimeMinute % interval), ChronoUnit.MINUTES);
        }
        log.info(
                "Final - Looking for V2 reports from report window start {} through end {}",
                startTime, endTime
        );

        List<DeliveryReportSummary> reports = tryRetrieveLineItemSummaries("", startTime, endTime);

        Map<String, Map<String, Integer>> lineToPlanTokensSpent = retrieveLineToPlanTokensSpent(startTime, endTime);

        log.info("reports returned=" + reports.size());
        recorder.markDeliveryReportsSummariesFetched(reports.size());
        return DeliveryReportSummaryToPlannerAdapterDto.buildDto(startTime, endTime, reports, lineToPlanTokensSpent);
    }

    Map<String, Map<String, Integer>> retrieveLineToPlanTokensSpent(Instant startTime, Instant endTime) {
        List<PlanDataSummary> planDataSummaryList =
                deliveryReportSummaryRepository.findPlanDataByReportWindowHour(startTime, endTime);
        log.info("planDataSummaryList.size=" + planDataSummaryList.size());
        Map<String, Map<String, Integer>> lineToPlanTokensSpent = new TreeMap<>();
        for (PlanDataSummary planDataSummary : planDataSummaryList) {
            if (planDataSummary.getLineItemId().equalsIgnoreCase("bidderPG-1010")) {
                log.info(planDataSummary.toString());
            }
            String[] planDataParts = planDataSummary.getPlanData().split(",");
            lineToPlanTokensSpent.putIfAbsent(planDataSummary.getLineItemId(), new TreeMap<>());
            Map<String, Integer> planMap = lineToPlanTokensSpent.get(planDataSummary.getLineItemId());
            for (int i = 0; i < planDataParts.length; i = i + 2) {
                if (!planDataParts[i].isEmpty()) {
                    planMap.putIfAbsent(planDataParts[i], 0);
                    planMap.put(
                            planDataParts[i],
                            planMap.get(planDataParts[i]) + Integer.parseInt(planDataParts[i + 1])
                    );
                }
            }
        }
        return lineToPlanTokensSpent;
    }

    List<DeliveryReportSummary> tryRetrieveLineItemSummaries(String lineItemIds, Instant startTime, Instant endTime) {
        Optional<Timer.Context> optionalContext = recorder.repositoryFetchDeliveryReportsSummaryTimer();
        try {
            return retrieveLineItemSummaries(lineItemIds, startTime, endTime);
        } catch (Exception ex) {
            String msg = "getDeliveryReportsSummary::Unexpected exception";
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.ERROR, msg, AlertPriority.HIGH, ex);
            return Collections.emptyList();
        } finally {
            optionalContext.ifPresent(Timer.Context::stop);
        }
    }

    List<Map<String, Object>> buildSummaries(String lineItemIds, Instant startTime, Instant endTime,
                                             Set<String> metrics, int interval, List<DeliveryReportSummary> reports) {
        List<Map<String, Object>> summaries = new LinkedList<>();
        List<List<DeliveryReportSummary>> intervalReports = groupByIntervals(reports, interval, startTime, endTime);
        for (int index = 0; index < intervalReports.size(); index++) {
            if (intervalReports.get(index).isEmpty()) {
                Map<String, Object> emptySummary = new TreeMap<>();
                emptySummary.put(LineItemSummaryReport.INTERVAL, index);
                summaries.add(emptySummary);
            } else {
                summaries.addAll(buildLineItemSummaryIntervalReport(intervalReports.get(index), metrics, index));
            }
        }

        if (true) {
            // Purposely skipping any adhoc generation for now due to performance concerns
            return summaries;
        }
        Instant adhocReportStartTime = intervalReports.isEmpty()
                ? startTime
                : intervalReports.get(intervalReports.size() - 1).get(0).getReportWindowEndTimestamp();
        List<Map<String, Object>> adhocReports = deliveryReportsDataService.getLineItemSummaryReport(lineItemIds,
                adhocReportStartTime, endTime, metrics, interval, intervalReports.size());

        summaries.addAll(adhocReports);
        return summaries;
    }

    List<Map<String, Object>> buildLineItemSummaryIntervalReport(
            Collection<DeliveryReportSummary> intervalReports, Set<String> metrics, int index) {
        List<Map<String, Object>> summaries = new LinkedList<>();
        Map<String, List<DeliveryReportSummary>> lineIdToReportMap = intervalReports.stream()
                .collect(Collectors.groupingBy(DeliveryReportSummary::getLineItemId, TreeMap::new,
                        Collectors.toCollection(LinkedList::new)));

        for (List<DeliveryReportSummary> deliveryReports : lineIdToReportMap.values()) {
            Map<String, Object> summaryMap = new TreeMap<>();
            DeliveryReportSummary report0 = deliveryReports.get(0);
            Instant dataWindowStart = report0.getDataWindowStartTimestamp();
            Instant dataWindowEnd = report0.getDataWindowEndTimestamp();
            for (int i = 0; i < deliveryReports.size(); i++) {
                DeliveryReportSummary report = deliveryReports.get(i);
                if (report.getDataWindowStartTimestamp().isBefore(dataWindowStart)) {
                    dataWindowStart = report.getDataWindowStartTimestamp();
                }
                if (report.getDataWindowEndTimestamp().isAfter(dataWindowEnd)) {
                    dataWindowEnd = report.getDataWindowEndTimestamp();
                }
                for (String field : metrics) {
                    LineItemSummaryReport.accumulateSimpleField(summaryMap, field,
                            LineItemSummaryReport.getSummaryMetricsFunctionMap(), report);
                }
            }
            summaryMap.put(LineItemSummaryReport.INTERVAL, index);
            summaryMap.put(LineItemSummaryReport.LINE_ITEM_ID, report0.getLineItemId());
            summaryMap.put(LineItemSummaryReport.EXT_LINE_ITEM_ID, report0.getExtLineItemId());
            summaryMap.put(LineItemSummaryReport.DATA_WINDOW_START_TIMESTAMP, Timestamp.from(dataWindowStart));
            summaryMap.put(LineItemSummaryReport.DATA_WINDOW_END_TIMESTAMP, Timestamp.from(dataWindowEnd));
            summaries.add(summaryMap);
        }
        return summaries;
    }

    public List<List<DeliveryReportSummary>> groupByIntervals(
            List<DeliveryReportSummary> reports, int interval, Instant startTime, Instant endTime) {
        List<List<DeliveryReportSummary>> intervalReports = new LinkedList<>();
        if (reports.isEmpty()) {
            return intervalReports;
        }

        Instant intervalStart = startTime;
        Instant intervalEnd = startTime.plus(interval, ChronoUnit.MINUTES);
        int index = 0;
        while (!intervalEnd.isAfter(endTime) && index < reports.size()) {
            List<DeliveryReportSummary> intervalReport = new LinkedList<>();
            intervalReports.add(intervalReport);
            while (index < reports.size()) {
                DeliveryReportSummary summary = reports.get(index);
                if (withinDuration(summary, intervalStart, intervalEnd)) {
                    intervalReport.add(summary);
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

    private boolean withinDuration(DeliveryReportSummary summary, Instant startTime, Instant endTime) {
        return !summary.getReportWindowStartTimestamp().isBefore(startTime)
                && summary.getDataWindowEndTimestamp().isBefore(endTime);
    }

    private List<DeliveryReportSummary> retrieveLineItemSummaries(String ids, Instant start, Instant end) {
        Pageable page = PageRequest.of(0,
                serverConfiguration.getLineItemSummaryPagerSize(),
                Sort.by(LineItemSummaryReport.REPORT_WINDOW_START_TIMESTAMP_NATIVE).ascending());
        return LineItemSummaryReport.retrieveLineItemSummaries(ids, start, end, page, this::findLineItemSummaries);
    }

    private Page<DeliveryReportSummary> findLineItemSummaries(List<String> ids, Instant startTime, Instant endTime,
                                                              Pageable page) {
        log.info("start=" + startTime + ", end=" + endTime);
        return ids.isEmpty()
                ? deliveryReportSummaryRepository.findByReportWindowHour(startTime, endTime, page)
                : deliveryReportSummaryRepository.findByLineIdAndReportWindowHour(ids, startTime, endTime, page);

    }

}


