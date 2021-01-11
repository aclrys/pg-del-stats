package org.prebid.pg.delstats.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.prebid.pg.delstats.model.dto.DeliveryReportFromPbsDto;
import org.prebid.pg.delstats.model.dto.DeliveryReportLineStatsDto;
import org.prebid.pg.delstats.model.dto.Tracer;
import org.prebid.pg.delstats.persistence.LatestTokenSpendSummary;
import org.slf4j.Logger;

import java.util.List;

public class TracerUtils {

    public static final String TRACER = "RADIOACTIVE";

    private static final String TRACE_MSG_FORMAT = "{}::{}";
    private static final String TRACE_MSG_RAW_FORMAT = "{}::{}::{}";

    private TracerUtils() {
    }

    public static void logIfActive(Logger log, Tracer tracer, String message) {
        traceAsInfoOrlogAsDebug(log, tracer, message);
    }

    public static void traceAsInfoOrlogAsDebug(Logger log, Tracer tracer, String message) {
        if (log.isInfoEnabled() && tracer.isActive()) {
            log.info(TRACE_MSG_FORMAT, TRACER, message);
        } else {
            log.debug(message);
        }
    }

    public static void traceAsInfoOrlogAsInfo(Logger log, Tracer tracer, String message) {
        if (log.isInfoEnabled() && tracer.isActive()) {
            log.info(TRACE_MSG_FORMAT, TRACER, message);
        } else {
            log.info(message);
        }
    }

    public static void logIfActiveRaw(Logger log, Tracer tracer, String message, Object rawObject) {
        if (log.isInfoEnabled() && tracer.isActiveAndRaw()) {
            log.info(TRACE_MSG_RAW_FORMAT, tracer, message, rawObject);
        } else if (tracer.isActive()) {
            log.info(TRACE_MSG_FORMAT, TRACER, message);
        } else {
            log.debug(message);
        }
    }

    public static void logIfActiveMatchLineItemIds(Logger log, Tracer tracer, String lineItemIds, String message) {
        if (log.isInfoEnabled() && tracer.isActive()) {
            if (tracer.matchLineItemId(lineItemIds)) {
                log.info(TRACE_MSG_FORMAT, TRACER, message);
            } else {
                log.info(message);
            }
        } else {
            log.info(message);
        }

    }

    public static void logIfActiveMatchBidderCode(Logger log, Tracer tracer, String bidderCode, String message) {
        if (log.isInfoEnabled() && tracer.isActive()) {
            if (tracer.matchBidderCode(bidderCode)) {
                log.info(TRACE_MSG_FORMAT, TRACER, message);
            } else {
                log.info(message);
            }
        } else {
            log.debug(message);
        }
    }

    public static void logStats(Logger log, Tracer tracer, List<DeliveryReportLineStatsDto> stats) {
        for (DeliveryReportLineStatsDto stat : stats) {
            if (tracer.isActive()
                    && tracer.matchBidderCode(stat.getBidderCode())
                    && tracer.matchLineItemId(stat.getLineItemId())) {
                log.info("{}::Delivery Progress Stat::{}", TRACER, stat);
            } else {
                log.debug("Delivery Progress Stat::{}", stat);
            }
        }

    }

    public static void logIfActiveTokenSummaries(Logger log, Tracer tracer,
                                                 List<LatestTokenSpendSummary> spendSummaries) {
        if (tracer.isActive()) {
            for (LatestTokenSpendSummary latestTokenSpendSummary : spendSummaries) {
                if (tracer.isMatchingOn(latestTokenSpendSummary.getVendor(), latestTokenSpendSummary.getRegion(),
                        latestTokenSpendSummary.getBidderCode(), latestTokenSpendSummary.getLineItemId())) {
                    log.info("{}::latestTokenSpendSummary={}", TRACER, latestTokenSpendSummary);
                }
            }
            log.info("{}::Latest Token Spend Records ({}) stored.", TRACER, spendSummaries.size());
        } else {
            log.info("Latest Token Spend Records ({}) stored.", spendSummaries.size());
        }
    }

    public static void logIfActiveMatchingOnVendorRegion(Logger log, Tracer tracer, LatestTokenSpendSummary summary) {
        if (tracer.isActive() && tracer.isMatchingOn(summary.getVendor(), summary.getRegion(),
                summary.getBidderCode(), summary.getLineItemId())) {
            log.info("{}::Token spend summary line={}", TRACER, summary);
        }
    }

    public static void logIfActiveMatchingOnLineItemStatus(Logger log, Tracer tracer,
                                                           ObjectMapper objectMapper, String message,
                                                           DeliveryReportFromPbsDto deliveryReport,
                                                           JsonNode lineItemStatusJson) {
        try {
            if (tracer.isActive() && tracer.isMatchingOn(
                    deliveryReport.getVendor(),
                    deliveryReport.getRegion(),
                    JacksonUtil.nullSafeGet(objectMapper, lineItemStatusJson, "lineItemSource"),
                    JacksonUtil.nullSafeGet(objectMapper, lineItemStatusJson, "lineItemId"))
            ) {
                if (log.isInfoEnabled()) {
                    log.info(TRACE_MSG_RAW_FORMAT, TRACER, message, deliveryReport);
                }
            }
        } catch (JsonProcessingException ex) {
            // Ignoring exceptions by tracing only code
        }

    }
}
