package org.prebid.pg.delstats.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.alerts.AlertName;
import org.prebid.pg.delstats.alerts.AlertPriority;
import org.prebid.pg.delstats.alerts.AlertProxyHttpClient;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.exception.DeliveryReportValidationException;
import org.prebid.pg.delstats.metrics.GraphiteMetricsRecorder;
import org.prebid.pg.delstats.model.dto.DeliveryReportFromPbsDto;
import org.prebid.pg.delstats.model.dto.Tracer;
import org.prebid.pg.delstats.persistence.DeliveryReport;
import org.prebid.pg.delstats.utils.JacksonUtil;
import org.prebid.pg.delstats.utils.LineItemStatusUtils;
import org.prebid.pg.delstats.utils.TimestampUtils;
import org.prebid.pg.delstats.utils.TracerUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

/**
 * Performs detailed work on Line Item Status entries in  Delivery Progress Reports, converting them from the transfer
 * format to the persistence format.
 */
@Component
@Slf4j
public class DeliveryReportProcessor {

    private final ServerConfiguration configuration;

    private final GraphiteMetricsRecorder recorder;

    private final ObjectMapper objectMapper;

    private final Tracer tracer;

    private final AlertProxyHttpClient alertProxyHttpClient;

    public DeliveryReportProcessor(ServerConfiguration configuration, SystemService systemService) {
        this.configuration = configuration;
        this.recorder = systemService.getRecorder();
        this.objectMapper = systemService.getObjectMapper();
        this.tracer = systemService.getTracer();
        this.alertProxyHttpClient = systemService.getAlertProxyHttpClient();
    }

    /**
     * Convert each line item status element from a Delivery Progress Report into a List of Delivery Report records.
     *
     * @param deliveryReportFromPbsDto
     * @param lineItemStatusJson
     * @param now
     * @param capturedExceptions
     * @return
     */
    List<DeliveryReport> processLineItemStatus(DeliveryReportFromPbsDto deliveryReportFromPbsDto,
                               JsonNode lineItemStatusJson,
                               Timestamp now,
                               List<Exception> capturedExceptions) {
        List<DeliveryReport> deliveryReports = new LinkedList<>();
        try {
            TracerUtils.logIfActiveMatchingOnLineItemStatus(log, tracer, objectMapper,
                    "Attempting to store deliveryReportFromPbsDto={}",
                    deliveryReportFromPbsDto, lineItemStatusJson);
            String lineItemId = JacksonUtil.nullSafeGet(objectMapper, lineItemStatusJson, "lineItemId");
            String extLineItemId = LineItemStatusUtils.getExtLineItemId(
                    lineItemId, configuration.getLineItemBidderCodeSeparator());
            String lineItemSource = JacksonUtil.nullSafeGet(
                    objectMapper, lineItemStatusJson, "lineItemSource");

            if (StringUtils.isEmpty(lineItemSource)) {
                log.warn("No lineItemSource present for line item id {} in Line Item Status::{}", lineItemId,
                        objectMapper.writeValueAsString(lineItemStatusJson));
                throw new DeliveryReportValidationException(
                        String.format("No LineItemSource present in Line Item Status for line %s", lineItemId));

            }
            DeliveryReport deliveryReport = DeliveryReport.builder()
                    .reportId(deliveryReportFromPbsDto.getReportId())
                    .vendor(deliveryReportFromPbsDto.getVendor())
                    .region(deliveryReportFromPbsDto.getRegion())
                    .instanceId(deliveryReportFromPbsDto.getInstanceId())
                    .bidderCode(lineItemSource)
                    .lineItemId(lineItemId)
                    .extLineItemId(extLineItemId)
                    .dataWindowStartTimestamp(deliveryReportFromPbsDto.getDataWindowStartTimeStamp())
                    .dataWindowEndTimestamp(deliveryReportFromPbsDto.getDataWindowEndTimeStamp())
                    .reportTimestamp(now)
                    .clientAuctions(deliveryReportFromPbsDto.getClientAuctions())
                    .lineItemStatus(objectMapper.writeValueAsString(lineItemStatusJson))
                    .build();
            log.info(deliveryReport.toString());
            deliveryReports.add(deliveryReport);
        } catch (DeliveryReportValidationException ex) {
            // Since deliveryReportFromPbsDto was parsed from valid json, this can only happen in test cases
            String msg = "Line Item Status not valid";
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.REPORT_PARSE_ERROR, msg,
                    AlertPriority.MEDIUM, ex, deliveryReportFromPbsDto.toString()
            );
            recorder.markExceptionMeter(ex);
            capturedExceptions.add(ex);
        } catch (JsonProcessingException ex) {
            // Since deliveryReportFromPbsDto was parsed from valid json, this can only happen in test cases
            String msg = "Line Item Status not valid json";
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.REPORT_PARSE_ERROR, msg,
                    AlertPriority.MEDIUM, ex, deliveryReportFromPbsDto.toString()
            );
            recorder.markExceptionMeter(ex);
            capturedExceptions.add(
                    new DeliveryReportValidationException("JsonProcessingException: " + ex.getOriginalMessage()));
        } catch (Exception ex) {
            String msg = "Unexpected problem in parsing delivery report";
            alertProxyHttpClient.raiseEventForExceptionAndLog(AlertName.ERROR, msg, AlertPriority.MEDIUM, ex);
            recorder.markExceptionMeter(ex);
            capturedExceptions.add(ex);
        }
        return deliveryReports;
    }

    /**
     * Validates the timestamp fields in the Delivery Progress Report received. This is a recursive operation that can
     * impact performance for a large number of lines.
     *
     * @param deliveryReportFromPbsDto
     */
    public void validateDeliveryReport(DeliveryReportFromPbsDto deliveryReportFromPbsDto) {
        if (!configuration.isValidationEnabled()) {
            return;
        }
        if (deliveryReportFromPbsDto.getDataWindowEndTimeStamp()
                .before(deliveryReportFromPbsDto.getDataWindowStartTimeStamp())) {
            log.error(
                    "DataWindowEndTimeStamp {} is earlier than DataWindowStartTimeStamp {} in report {}",
                    deliveryReportFromPbsDto.getDataWindowEndTimeStamp(),
                    deliveryReportFromPbsDto.getDataWindowStartTimeStamp(),
                    deliveryReportFromPbsDto.getReportId()
            );
            throw new DeliveryReportValidationException("DataWindowEndTimeStamp before DataWindowStartTimeStamp");
        }
        deliveryReportFromPbsDto.getLineItemStatus().stream().forEach(lineItemStatus ->
                checkNodesRecursivelyForValidTimestamps("root", lineItemStatus)
        );
    }

    void checkNodesRecursivelyForValidTimestamps(String name, JsonNode node) {
        if (node.isObject()) {
            node.fields().forEachRemaining(field ->
                    checkNodesRecursivelyForValidTimestamps(field.getKey(), field.getValue()));
        } else {
            if (node.isArray()) {
                node.elements().forEachRemaining(element ->
                        checkNodesRecursivelyForValidTimestamps("[]", element));
            } else {
                if (node.isValueNode() && name.toLowerCase().endsWith("timestamp")) {
                    TimestampUtils.convertStringTimeToTimestamp(node.asText(), name);
                }
            }
        }
    }

}
