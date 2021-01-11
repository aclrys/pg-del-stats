package org.prebid.pg.delstats.controller;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.config.ApplicationConfiguration.CsvMapperFactory;
import org.prebid.pg.delstats.config.DeploymentConfiguration;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.exception.DeliveryReportProcessingException;
import org.prebid.pg.delstats.exception.SystemInitializationException;
import org.prebid.pg.delstats.model.dto.DeliveryReportFromPbsDto;
import org.prebid.pg.delstats.model.dto.DeliveryReportSummaryToPlannerAdapterDto;
import org.prebid.pg.delstats.model.dto.LineItemSummaryReport;
import org.prebid.pg.delstats.model.dto.TokenSpendSummaryDto;
import org.prebid.pg.delstats.repository.DeliveryProgressReportsRepository;
import org.prebid.pg.delstats.repository.LatestTokenSpendSummaryRepository;
import org.prebid.pg.delstats.services.DeliveryReportSummaryService;
import org.prebid.pg.delstats.services.DeliveryReportsDataService;
import org.prebid.pg.delstats.services.TokenSpendDataService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Endpoints that should be enabled only in a Simulation testing environment where a complete set of PG systems are
 * integrated to test the entire end-to-end algorithm. The intent of these endpoints is to override standard endpoints
 * when simulation time should be used instead of the system clock or to trigger activities normally triggered by the
 * system clock for scheduled events when the scheduler is disabled. For those endpoint, the intent is that, a test
 * orchestrator will hit these end points to perform utility or background operations at the simulated scheduled time.
 */
@RestController
@RequestMapping("${services.base-url}")
@ConditionalOnProperty(prefix = "deployment", name = "profile", havingValue = "algotest")
@Slf4j
@ApiIgnore
public class AlgoTestController {

    private static final String HEADER_ALGOTEST_NOW_TIME = "pg-sim-timestamp";
    private static final String HEADER_ALGOTEST_START_TIME = "pg-algotest-stats-start-time";
    private static final String HEADER_ALGOTEST_END_TIME = "pg-algotest-stats-end-time";
    public static final String STATS_SERVER_NOT_IN_ALGO_TEST_MODE = "Delivery Stats Server not in AlgoTest mode.";

    private TokenSpendDataService tokenSpendDataService;

    private DeliveryReportsDataService deliveryReportsDataService;

    private DeliveryReportSummaryService deliverySummaryService;

    private final LatestTokenSpendSummaryRepository latestTokenSpendSummaryRepository;

    private final DeliveryProgressReportsRepository deliveryReportSummariesRepository;

    private DeploymentConfiguration deploymentConfiguration;

    private ServerConfiguration serverConfiguration;

    private CsvMapper csvMapper;

    private String lastSeenAlgoTestNow;

    public AlgoTestController(TokenSpendDataService tokenSpendDataService,
                              DeliveryReportsDataService deliveryReportsDataService,
                              DeliveryReportSummaryService deliverySummaryService,
                              LatestTokenSpendSummaryRepository latestTokenSpendSummaryRepository,
                              DeliveryProgressReportsRepository deliveryReportSummariesRepository,
                              DeploymentConfiguration deploymentConfiguration,
                              ServerConfiguration serverConfiguration,
                              CsvMapperFactory csvMapperFactory) {
        this.tokenSpendDataService = tokenSpendDataService;
        this.deliveryReportsDataService = deliveryReportsDataService;
        this.deliverySummaryService = deliverySummaryService;
        this.latestTokenSpendSummaryRepository = latestTokenSpendSummaryRepository;
        this.deliveryReportSummariesRepository = deliveryReportSummariesRepository;
        this.deploymentConfiguration = deploymentConfiguration;
        this.serverConfiguration = serverConfiguration;
        this.csvMapper = csvMapperFactory.getCsvMapper();
    }

    @PostConstruct
    public void init() {
        if (deploymentConfiguration.getProfile() != DeploymentConfiguration.ProfileType.ALGOTEST) {
            throw new SystemInitializationException("AlgoTest Controller started in wrong profile: "
                    + deploymentConfiguration.getProfile().toString());
        }
        log.info("AlgoTest Controller contructed.");
    }

    void overrideCsvMapper(CsvMapper csvMapper) {
        this.csvMapper = csvMapper;
    }

    private String fixReportTimestamp(String reportTimestamp) {
        if (reportTimestamp != null) {
            lastSeenAlgoTestNow = reportTimestamp;
            return reportTimestamp;
        }
        return lastSeenAlgoTestNow;
    }

    /**
     * Overrides the usual endpoint for posting Deliver Progress Reports using the simulation time.
     *
     * @param deliveryReportFromPbsDto
     * @param reportTimestamp
     */
    @PostMapping(value = "/v1/report/delivery", headers = {HEADER_ALGOTEST_NOW_TIME})
    @ResponseStatus(HttpStatus.OK)
    public void storeReport(
            @RequestBody DeliveryReportFromPbsDto deliveryReportFromPbsDto,
            @RequestHeader(value = HEADER_ALGOTEST_NOW_TIME) String reportTimestamp
    ) {
        reportTimestamp = fixReportTimestamp(reportTimestamp);
        log.debug("Delivery Progress Report posted with time '{}'.", reportTimestamp);
        if (!DeploymentConfiguration.ProfileType.ALGOTEST.equals(deploymentConfiguration.getProfile())) {
            throw new IllegalStateException(STATS_SERVER_NOT_IN_ALGO_TEST_MODE);
        }
        deliveryReportsDataService.storeReport(deliveryReportFromPbsDto, reportTimestamp, HEADER_ALGOTEST_NOW_TIME);
    }

    /**
     * Overrides the usual endpoint for triggering the scheduled event that aggregated Delivery Progress Reports.
     *
     * @param startTime
     * @param endTime
     */
    @PostMapping(value = "/e2eAdmin/aggregate", headers = {HEADER_ALGOTEST_START_TIME, HEADER_ALGOTEST_END_TIME})
    @ResponseStatus(HttpStatus.OK)
    public void aggregate(@RequestHeader(value = HEADER_ALGOTEST_START_TIME) String startTime,
                          @RequestHeader(value = HEADER_ALGOTEST_END_TIME) String endTime) {
        if (!DeploymentConfiguration.ProfileType.ALGOTEST.equals(deploymentConfiguration.getProfile())) {
            throw new IllegalStateException(STATS_SERVER_NOT_IN_ALGO_TEST_MODE);
        }
        tokenSpendDataService.aggregate(startTime, endTime, HEADER_ALGOTEST_START_TIME, HEADER_ALGOTEST_END_TIME);
    }

    /**
     * Overrides the usual endpoint for retrieving Token Spend Summaries using the simulation time.
     * @param since
     * @param reportTimestamp
     * @return
     */
    @GetMapping(value = "/v1/report/token-spend", headers = {HEADER_ALGOTEST_NOW_TIME})
    @ResponseStatus(HttpStatus.OK)
    public TokenSpendSummaryDto getReport(@RequestParam(required = false) String since,
                                          @RequestParam(required = false) String vendor,
                                          @RequestParam(required = false) String region,
                                          @RequestHeader(value = HEADER_ALGOTEST_NOW_TIME) String reportTimestamp) {
        reportTimestamp = fixReportTimestamp(reportTimestamp);
        log.info("AlgoTest token-spend accessed: Header: '{}', Since: '{}'", reportTimestamp, since);
        if (!DeploymentConfiguration.ProfileType.ALGOTEST.equals(deploymentConfiguration.getProfile())) {
            throw new IllegalStateException(STATS_SERVER_NOT_IN_ALGO_TEST_MODE);
        }
        if (StringUtils.isEmpty(reportTimestamp)) {
            reportTimestamp = since;
        }
        if (StringUtils.isEmpty(reportTimestamp)) {
            throw new IllegalStateException("Token Spend called in Simulation mode with no simulation time provided.");
        }
        return tokenSpendDataService.getTokenSpendSummary("", null, null, reportTimestamp);
    }

    @GetMapping(value = "/v2/report/delivery", headers = {HEADER_ALGOTEST_NOW_TIME},
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public DeliveryReportSummaryToPlannerAdapterDto getDeliveryReportsV2(
            @ApiIgnore Authentication authentication,
            @RequestParam String bidderCode,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestHeader(value = HEADER_ALGOTEST_NOW_TIME) String reportTimestamp) {
        reportTimestamp = fixReportTimestamp(reportTimestamp);
        log.info("AlgoTest v2 delivery report accessed: Header: '{}', Start: '{}', End: '{}'",
                reportTimestamp, startTime, endTime);
        if (!DeploymentConfiguration.ProfileType.ALGOTEST.equals(deploymentConfiguration.getProfile())) {
            throw new IllegalStateException(STATS_SERVER_NOT_IN_ALGO_TEST_MODE);
        }
        return deliverySummaryService.getDeliverySummaryReport(startTime, endTime,
                reportTimestamp, HEADER_ALGOTEST_NOW_TIME);
    }

    /**
     * A new endpoint to clean the applications database. It should be called prior to running the simulation.
     */
    @PostMapping(value = "/e2eAdmin/cleanup", headers = {HEADER_ALGOTEST_START_TIME, HEADER_ALGOTEST_END_TIME})
    @ResponseStatus(HttpStatus.OK)
    public void cleanup() {
        if (!DeploymentConfiguration.ProfileType.ALGOTEST.equals(deploymentConfiguration.getProfile())) {
            throw new IllegalStateException(STATS_SERVER_NOT_IN_ALGO_TEST_MODE);
        }
        doCleanup();
    }

    @Transactional
    public void doCleanup() {
        deliveryReportSummariesRepository.deleteAll();
        latestTokenSpendSummaryRepository.deleteAll();
    }

    /**
     * An endpoint to be used after the simulation has completed to calculate and return the line item summaries since
     * the scheduled line item summary service has not yet been coded to work in the simulation environment.
     *
     * @param lineItemIds
     * @param startTime
     * @param endTime
     * @param metrics
     * @param interval
     * @return
     */
    @GetMapping("/e2eAdmin/line-item-summary")
    @ResponseStatus(HttpStatus.OK)
    public String getLineItemSummary(
            @RequestParam(required = false) String lineItemIds,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String metrics,
            @RequestParam(required = false) Integer interval
    ) {
        try {
            LineItemSummaryReport.LineItemSummaryRequest req = LineItemSummaryReport.validateLineItemSummaryRequest(
                    metrics, startTime, endTime, interval, serverConfiguration);
            List<Map<String, Object>> lineItemSummaries = deliveryReportsDataService.getLineItemSummaryReport(
                    lineItemIds, req.getStartTime(), req.getEndTime(), req.getMetrics(), req.getInterval(), 0);
            return csvMapper.writerFor(List.class)
                    .with(LineItemSummaryReport.csvSchema(req.getMetrics()))
                    .writeValueAsString(lineItemSummaries);
        } catch (Exception e) {
            log.error("getLineItemSummaries::Unexpected exception", e);
            throw new DeliveryReportProcessingException(e.getMessage());
        }
    }

    /**
     * An endpoint to be used during the simulation run to perform the line item aggregation operation.
     *
     * @param startTime
     * @param endTime
     * @param reportTimestamp
     *
     * @deprecated Use the {@link #getLineItemSummary(String, String, String, String, Integer)} instead.
     */
    @PostMapping(value = "/e2eAdmin/aggregate-line-item-summary", headers = { HEADER_ALGOTEST_NOW_TIME })
    public void aggregateLineItemSummary(@RequestParam String startTime, @RequestParam String endTime,
                                         @RequestHeader(value = HEADER_ALGOTEST_NOW_TIME) String reportTimestamp) {
        reportTimestamp = fixReportTimestamp(reportTimestamp);
        log.info("Aggregate Delivery Reports Summary requested with Sim Time {} from {} through {}",
                reportTimestamp, startTime, endTime);

        Instant start = Instant.parse(reportTimestamp);
        Instant finalEndTime = Instant.parse(reportTimestamp).plusSeconds(3600);
        final int defaultInterval = 60;
        while (start.isBefore(finalEndTime)) {
            Instant end = start.plus(defaultInterval, ChronoUnit.MINUTES);
            if (end.isAfter(finalEndTime)) {
                break;
            }
            deliverySummaryService.runDeliveryReportSummary(Timestamp.from(start), Timestamp.from(end), true);
            start = end;
        }
        log.info("Done Aggregate Delivery Reports Summary requested from {} through {}", startTime, endTime);
    }

}

