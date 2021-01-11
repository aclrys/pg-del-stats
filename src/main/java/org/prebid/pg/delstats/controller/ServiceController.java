package org.prebid.pg.delstats.controller;

import com.codahale.metrics.Timer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.alerts.AlertName;
import org.prebid.pg.delstats.alerts.AlertPriority;
import org.prebid.pg.delstats.alerts.AlertProxyHttpClient;
import org.prebid.pg.delstats.config.ApplicationConfiguration.CsvMapperFactory;
import org.prebid.pg.delstats.config.DeploymentConfiguration;
import org.prebid.pg.delstats.config.SecurityConfiguration;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.exception.ApiNotActiveException;
import org.prebid.pg.delstats.exception.DeliveryReportProcessingException;
import org.prebid.pg.delstats.exception.DeliveryReportValidationException;
import org.prebid.pg.delstats.metrics.GraphiteMetricsRecorder;
import org.prebid.pg.delstats.model.dto.DeliveryReportFromPbsDto;
import org.prebid.pg.delstats.model.dto.DeliveryReportSummaryToPlannerAdapterDto;
import org.prebid.pg.delstats.model.dto.DeliveryReportToPlannerAdapterDto;
import org.prebid.pg.delstats.model.dto.LineItemSummaryReport;
import org.prebid.pg.delstats.model.dto.Shutdown;
import org.prebid.pg.delstats.model.dto.TokenSpendSummaryDto;
import org.prebid.pg.delstats.model.dto.Tracer;
import org.prebid.pg.delstats.services.DeliveryReportSummaryService;
import org.prebid.pg.delstats.services.DeliveryReportsDataService;
import org.prebid.pg.delstats.services.SystemService;
import org.prebid.pg.delstats.services.TokenSpendDataService;
import org.prebid.pg.delstats.utils.TracerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides the primary API endpoints for the PG Delivery Stats service including posting and fetching
 * Delivery Progress Reports, and fetching the Token Spend and Line Item summaries.
 */
@Slf4j
@RestController
@RequestMapping("${services.base-url}")
@Api(tags = {"pbs", "pa", "gp"})
public class ServiceController {

    private TokenSpendDataService tokenSpendDataService;

    private DeliveryReportsDataService deliveryReportsDataService;

    private DeliveryReportSummaryService deliverySummaryService;

    private CsvMapperFactory csvMapperFactory;

    private ServerConfiguration serverConfiguration;

    private DeploymentConfiguration deploymentConfiguration;

    private Tracer tracer;

    private Shutdown shutdown;

    private GraphiteMetricsRecorder recorder;

    private AlertProxyHttpClient alertProxyHttpClient;

    public ServiceController(
            @Autowired TokenSpendDataService tokenSpendDataService,
            DeliveryReportsDataService deliveryReportsDataService,
            DeliveryReportSummaryService deliverySummaryService,
            CsvMapperFactory csvMapperFactory,
            ServerConfiguration serverConfiguration,
            DeploymentConfiguration deploymentConfiguration,
            SystemService systemService
    ) {
        this.tokenSpendDataService = tokenSpendDataService;
        this.deliveryReportsDataService = deliveryReportsDataService;
        this.deliverySummaryService = deliverySummaryService;
        this.csvMapperFactory = csvMapperFactory;
        this.serverConfiguration = serverConfiguration;
        this.deploymentConfiguration = deploymentConfiguration;
        this.tracer = systemService.getTracer();
        this.shutdown = systemService.getShutdown();
        this.recorder = systemService.getRecorder();
        this.alertProxyHttpClient = systemService.getAlertProxyHttpClient();
    }

    /**
     * Endpoint to accept Delivery Progress Reports from PBS instances and store them in a central repository
     * to be summarized in the background at a later time.
     *
     * @param deliveryReportFromPbsDto
     */
    @PostMapping(value = "/v1/report/delivery")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "postDeliveryReport", tags = {"pbs"}, consumes = "application/json", notes =
            "On a scheduled basis, each PBS instance will send a Delivery Report for each line active"
                    + " since the last report was sent.")
    @ApiResponses(
            @ApiResponse(code = 400, message = "Delivery Report validation failed.")
    )
    public void storeReport(
            @RequestBody DeliveryReportFromPbsDto deliveryReportFromPbsDto
    ) {
        if (deliveryReportFromPbsDto == null) {
            throw new DeliveryReportValidationException("No Delivery Report contents");
        }

        if (DeploymentConfiguration.ProfileType.ALGOTEST.equals(deploymentConfiguration.getProfile())) {
            log.error("Received Delivery Report in Sim Mode without Sim Time header.");
            throw new DeliveryReportValidationException("Missing Simulation Time header.");
        }
        String source = String.format("%s|%s|%s",
                deliveryReportFromPbsDto.getVendor(),
                deliveryReportFromPbsDto.getRegion(),
                deliveryReportFromPbsDto.getInstanceId()
        );
        log.info("Received Delivery Report {} with {} lines from {}", deliveryReportFromPbsDto.getReportId(),
                deliveryReportFromPbsDto.getLineItemStatus().size(), source);
        checkForShutdown();
        validateDeliveryReportMetadata(deliveryReportFromPbsDto);
        log.info("Validated meta data in Delivery Report {}", deliveryReportFromPbsDto.getReportId());
        recorder.markPostRequestForDeliveryReport();
        TracerUtils.logIfActiveRaw(log, tracer, String.format("Delivery Report received (%s)", source),
                deliveryReportFromPbsDto);
        Optional<Timer.Context> optionalContext = recorder.postDeliveryReportPerformanceTimer();
        try {
            deliveryReportsDataService.storeReport(deliveryReportFromPbsDto);
        } catch (Exception e) {
            String msg = "storeReport::Unexpected exception";
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.ERROR, msg, AlertPriority.HIGH, e);
            throw new DeliveryReportProcessingException(e.getMessage());
        } finally {
            optionalContext.ifPresent(Timer.Context::close);
        }
    }

    /**
     * Endpoint to fetch raw Delivery Progress Reports for Planning Adapters. The results can be filtered on
     * bidderCode and start and end times. If not provided, configurable settings are used to determine the time frame
     * based on the system clock. Because of the volume of data that can be returned, the results will be compressed.
     *
     * @param authentication
     * @param bidderCode
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping(value = "/v1/report/delivery", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "getDeliveryReports", tags = {"pa"}, produces = "application/json", notes =
            "Set of all delivery reports sent by PBS instances with matching bidder code from the startTime"
                    + " (inclusive) to the end time (exclusive).")
    public DeliveryReportToPlannerAdapterDto getDeliveryReports(@ApiIgnore Authentication authentication,
                                                                @RequestParam String bidderCode,
                                                                @RequestParam(required = false) String startTime,
                                                                @RequestParam(required = false) String endTime) {
        if (!serverConfiguration.isPaApiEnabled()) {
            throw new ApiNotActiveException("/v1/report/delivery is not active");
        }
        checkForShutdown();
        validateRoleForBidderCode(authentication, bidderCode);
        recorder.markGetRequestForDeliveryReports();
        log.info(
                "Delivery Progress Reports requested from {} through {} for bidderCode = {}",
                startTime, endTime, bidderCode
        );

        DeliveryReportToPlannerAdapterDto deliveryReportToPlannerAdapterDto;
        Optional<Timer.Context> optionalContext = recorder.getDeliveryReportsPerformanceTimer();
        int recordsReturned = 0;
        try {
            deliveryReportToPlannerAdapterDto =
                    deliveryReportsDataService.retrieveByBidderCode(bidderCode, startTime, endTime);
            recordsReturned = deliveryReportToPlannerAdapterDto.getDeliveryReports().size();
        } catch (Exception e) {
            String msg = "getReport::Unexpected exception";
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.ERROR, msg, AlertPriority.HIGH, e);
            throw new DeliveryReportProcessingException(e.getMessage());
        } finally {
            String millisSpent = String.format("%.03f",
                    optionalContext.map(Timer.Context::stop).orElse(0L) / 1_000_000.0);
            log.info("Returned {} Delivery Reports after {} milliseconds", recordsReturned, millisSpent);
        }
        return deliveryReportToPlannerAdapterDto;
    }

    boolean validateRoleForBidderCode(Authentication authentication, @RequestParam String bidderCode) {
        if (authentication == null) {
            return true;
        }
        User userDetails = (User) authentication.getPrincipal();
        if (userDetails.getAuthorities().stream()
                .anyMatch(
                        grantedAuthority ->
                                grantedAuthority.getAuthority().endsWith(bidderCode)
                                        ||
                                grantedAuthority.getAuthority().endsWith(SecurityConfiguration.ADMIN_ROLE))) {
            return true;
        }
        throw new DeliveryReportValidationException(String.format("Access denied for user %s to bidder %s reports",
                userDetails.getUsername(), bidderCode));
    }

    /**
     * Endpoint to provide token spend summaries used by General Planners since a given time. If not provided,
     * configurable settings are used to determine the time frame based on the system clock.
     *
     * @param since
     * @param vendor
     * @param region
     * @return
     */
    @GetMapping("/v1/report/token-spend")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "getTokenSpend", tags = {"gp"}, produces = "application/json", notes =
            "Set of all token spend reports aggregated since last request or since a requested time.")
    public TokenSpendSummaryDto getTokenSpendReports(@RequestParam(required = false) String since,
                                                     @RequestParam(required = false) String vendor,
                                                     @RequestParam(required = false) String region) {
        checkForShutdown();
        if (!serverConfiguration.isTokenSpendApiEnabled()) {
            throw new ApiNotActiveException("/v1/report/token-spend is not active");
        }
        if (DeploymentConfiguration.ProfileType.ALGOTEST.equals(deploymentConfiguration.getProfile())) {
            throw new ApiNotActiveException("Regular Token Spend Endpoint hit while in algorithm testing mode.");
        }
        if ((vendor == null && region != null) || (region == null && vendor != null)) {
            throw new DeliveryReportValidationException("If vendor or region is provided then both must be provided.");
        }

        long start = System.currentTimeMillis();
        recorder.markGetRequestForTokenSpend();
        TracerUtils.traceAsInfoOrlogAsInfo(log, tracer, String.format("Token Spend Summary requested since %s", since));

        Optional<Timer.Context> optionalContext = recorder.getTokenSpendPerformanceTimer();
        try {
            TokenSpendSummaryDto dto = tokenSpendDataService.getTokenSpendSummary(since, vendor, region);
            int size = 0;
            if (dto != null && dto.getTokenSpendSummaryLines() != null) {
                size = dto.getTokenSpendSummaryLines().size();
            }
            log.info("Token Spend Summary returned {} rows in {}ms", size, System.currentTimeMillis() - start);
            return dto;
        } catch (Exception e) {
            String msg = "getReport::Called by GP::Unexpected exception::%s";
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.ERROR, msg, AlertPriority.HIGH, e);
            throw new DeliveryReportProcessingException(e.getMessage());
        } finally {
            optionalContext.ifPresent(Timer.Context::close);
        }
    }

    /**
     * Endpoint to fetch aggregated Delivery Progress Reports for Planning Adapters. The results can be filtered on
     * bidderCode and start and end times. If not provided, configurable settings are used to determine the time frame
     * based on the system clock. Because of the volume of data that can be returned, the results will be compressed.
     *
     * @param authentication
     * @param bidderCode
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping(value = "/v2/report/delivery", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "getSummarizedDeliveryReports", tags = {"pa"}, produces = "application/json", notes =
            "Summarized set of all delivery reports sent by PBS instances with matching bidder code from the startTime"
                    + " (inclusive) to the end time (exclusive).")
    public DeliveryReportSummaryToPlannerAdapterDto getDeliveryReportsV2(
            @ApiIgnore Authentication authentication,
            @RequestParam String bidderCode,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        if (!serverConfiguration.isPaApiEnabled()) {
            throw new ApiNotActiveException("/v2/report/delivery is not active");
        }
        checkForShutdown();
        validateRoleForBidderCode(authentication, bidderCode);
        recorder.markGetRequestForDeliveryReports();
        log.info(
                "V2 Delivery Progress Reports requested from start {} through end {} for bidderCode = {}",
                startTime, endTime, bidderCode
        );

        Optional<Timer.Context> optionalContext = recorder.getDeliveryReportsPerformanceTimer();
        try {
            return deliverySummaryService.getDeliverySummaryReport(startTime, endTime);
        } catch (Exception e) {
            String msg = "getReportV2::Unexpected exception";
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.ERROR, msg, AlertPriority.HIGH, e);
            throw new DeliveryReportProcessingException(e.getMessage());
        } finally {
            String millisSpent = String.format("%.03f",
                    optionalContext.map(Timer.Context::stop).orElse(0L) / 1_000_000.0);
            log.info("Returned V2 Delivery Reports after {} milliseconds", millisSpent);
        }
    }

    /**
     * Endpoint for returning line item summary reports. Can be filtered by a list of requested line item ids and/or
     * specific metrics. If not provided, configurable settings are used to determine the start time used based on
     * system clock. The end time is always calculated based on a configured setting.
     *
     * @param lineItemIds
     * @param startTime
     * @param metrics
     *
     * @deprecated A separate service is now responsible for generating line item summaries and reporting on them.
     */
    @GetMapping("/v1/report/line-item-summary")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "getLineItemSummaries", tags = {"read-only"}, produces = "application/text", notes = "Comma "
            + "separated list of metrics per line per summarized interval. First line is header of metric names.")
    @ApiResponses(
            @ApiResponse(code = 200, message = "Multiple lines of csv metrics",
                examples = @Example(value = {@ExampleProperty(mediaType = "*/*", value = "interval,"
                    + "dataWindowStartTimestamp,dataWindowEndTimestamp,lineItemId,extLineItemId,accountAuctions,"
                    + "domainMatched,targetMatched,targetMatchedButFcapped,targetMatchedButFcapLookupFailed,"
                    + "pacingDeferred,sentToBidder,sentToBidderAsTopMatch,receivedFromBidderInvalidated,"
                    + "receivedFromBidder,sentToClient,sentToClientAsTopMatch,winEvents\n"
                + "0,2020-09-17T17:59:00.000Z,2020-09-17T18:59:00.000Z,bidderPG-2232,2232,0,0,0,0,0,0,0,0,0,0,0,0,0\n"
                + "1,2020-09-17T18:59:00.000Z,2020-09-17T19:59:00.000Z,bidderPG-2232,2232,0,0,0,0,0,0,0,0,0,0,0,0,0\n"
                + "2,2020-09-17T19:59:00.000Z,2020-09-17T20:59:00.000Z,bidderPG-2232,2232,0,0,0,0,0,0,0,0,0,0,0,0,0\n"
                + "3,2020-09-17T20:59:00.000Z,2020-09-17T21:14:00.000Z,bidderPG-2232,2232,0,0,0,0,0,0,0,0,0,0,0,0,0"
            )})))
    public String getLineItemSummary(
            @RequestParam(required = false) String lineItemIds,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String metrics
    ) {
        if (!serverConfiguration.isSumApiEnabled()) {
            throw new ApiNotActiveException("/v1/report/line-item-summary is not active");
        }
        checkForShutdown();
        recorder.markGetRequestForDeliveryLineItems();
        TracerUtils.logIfActiveMatchLineItemIds(log, tracer, lineItemIds,
                String.format("Delivery Progress Reports Stats requested from %s for lineItemIds = %s",
                        startTime, lineItemIds));
        Optional<Timer.Context> optionalContext = recorder.getDeliveryReportLinesPerformanceTimer();

        try {
            LineItemSummaryReport.LineItemSummaryRequest req = LineItemSummaryReport.validateLineItemSummaryRequest(
                    metrics, startTime, null, 60, serverConfiguration);
            log.info("Line Item Summary Requested for times {} to {} over interval {} for lines: {}",
                    req.getStartTime(), req.getEndTime(), req.getInterval(), lineItemIds);
            List<Map<String, Object>> lineItemSummaries = deliverySummaryService.getLineItemSummaryReport(
                    lineItemIds, req.getStartTime(), req.getEndTime(), req.getMetrics(), req.getInterval());
            return csvMapperFactory.getCsvMapper().writerFor(List.class)
                    .with(LineItemSummaryReport.csvSchema(req.getMetrics()))
                    .writeValueAsString(lineItemSummaries);
        } catch (Exception e) {
            log.error("Caught exception getting LineItemSummary", e);
            String msg = "getLineItemSummaries::Unexpected exception";
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.ERROR, msg, AlertPriority.HIGH, e);
            throw new DeliveryReportProcessingException(e.getMessage());
        } finally {
            optionalContext.ifPresent(Timer.Context::close);
        }
    }

    /**
     * Provides basic validation of Delivery Progress Reports received from PBS instances. A configurable setting can
     * be used to disabled this validation if needed.
     *
     * @param deliveryReportFromPbsDto
     */
    void validateDeliveryReportMetadata(DeliveryReportFromPbsDto deliveryReportFromPbsDto) {
        if (!serverConfiguration.isValidationEnabled()) {
            return;
        }
        if (deliveryReportFromPbsDto.getReportId() == null) {
            log.info("Delivery Report with missing Report Id");
            throw new DeliveryReportValidationException("Missing Report Id");
        }
        if (deliveryReportFromPbsDto.getInstanceId() == null) {
            log.info("Delivery Report with missing Instance Id");
            throw new DeliveryReportValidationException("Missing Instance Id");
        }
        if (deliveryReportFromPbsDto.getVendor() == null) {
            log.info("Delivery Report with missing Vendor");
            throw new DeliveryReportValidationException("Missing Vendor Id");
        }
        if (deliveryReportFromPbsDto.getRegion() == null) {
            log.info("Delivery Report with missing Region");
            throw new DeliveryReportValidationException("Missing Region Id");
        }
        if (deliveryReportFromPbsDto.getReportTimeStamp() == null) {
            log.info("Delivery Report ({}) with missing Report Timestamp : {}",
                    deliveryReportFromPbsDto.getReportId(), deliveryReportFromPbsDto.getReportTimeStamp());
            throw new DeliveryReportValidationException("Invalid report timestamp");
        }
        if (deliveryReportFromPbsDto.getDataWindowStartTimeStamp() == null) {
            log.info("Delivery Report ({}) with missing Data Window Start : {}",
                    deliveryReportFromPbsDto.getReportId(), deliveryReportFromPbsDto.getDataWindowStartTimeStamp());
            throw new DeliveryReportValidationException("Invalid data window start timestamp");
        }
        if (deliveryReportFromPbsDto.getDataWindowEndTimeStamp() == null) {
            log.info("Delivery Report ({}) with missing Data Window End : {}",
                    deliveryReportFromPbsDto.getReportId(), deliveryReportFromPbsDto.getDataWindowEndTimeStamp());
            throw new DeliveryReportValidationException("Invalid data window end timestamp");
        }
        if (!deliveryReportFromPbsDto.getDataWindowEndTimeStamp()
                .after(deliveryReportFromPbsDto.getDataWindowStartTimeStamp())) {
            log.info("Delivery Report ({}) with Data Window Start and End invalid: {} / {}",
                    deliveryReportFromPbsDto.getReportId(), deliveryReportFromPbsDto.getDataWindowStartTimeStamp(),
                    deliveryReportFromPbsDto.getDataWindowEndTimeStamp());
            throw new DeliveryReportValidationException("Data window end timestamp before start timestamp");
        }
    }

    private void checkForShutdown() {
        if (shutdown.isInitiating()) {
            throw new IllegalStateException("Service shutdown has been initiated");
        }
    }
}
