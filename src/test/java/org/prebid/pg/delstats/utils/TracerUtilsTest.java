package org.prebid.pg.delstats.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.POJONode;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.prebid.pg.delstats.model.dto.DeliveryReportFromPbsDto;
import org.prebid.pg.delstats.model.dto.DeliveryReportLineStatsDto;
import org.prebid.pg.delstats.model.dto.Tracer;
import org.prebid.pg.delstats.persistence.LatestTokenSpendSummary;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TracerUtilsTest {
    SoftAssertions softAssertions;

    @Mock
    Logger logger;

    @Mock
    Tracer tracer;

    @Mock
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        softAssertions = new SoftAssertions();
    }

    @Test
    void logIfActive() {
        String msg = "message";

        when(logger.isInfoEnabled()).thenReturn(true);
        when(tracer.isActive()).thenReturn(true).thenReturn(false);

        softAssertions.assertThatCode(() -> TracerUtils.logIfActive(logger, tracer, msg))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> TracerUtils.logIfActive(logger, tracer, msg))
                .doesNotThrowAnyException();
        softAssertions.assertAll();

        verify(logger, times(1)).info(anyString(), eq(TracerUtils.TRACER), any());
        verify(logger, times(1)).debug(anyString());
    }

    @Test
    void logIfActiveRaw() {
        String msg = "message";
        Boolean raw = true;

        when(logger.isInfoEnabled()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(tracer.isActiveAndRaw()).thenReturn(true).thenReturn(false);
        when(tracer.isActive()).thenReturn(true).thenReturn(false);

        softAssertions.assertThatCode(() -> TracerUtils.logIfActiveRaw(logger, tracer, msg, raw))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> TracerUtils.logIfActiveRaw(logger, tracer, msg, raw))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> TracerUtils.logIfActiveRaw(logger, tracer, msg, raw))
                .doesNotThrowAnyException();
        softAssertions.assertAll();

        verify(logger, times(1)).info(anyString(), eq(tracer), eq(msg), eq(raw));
    }

    @Test
    void logIfActiveMatchLineItemIds() {
        String lineIds = "L1, L2";
        String msg = "message";

        when(logger.isInfoEnabled()).thenReturn(true).thenReturn(true).thenReturn(true);
        when(tracer.isActive()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(tracer.matchLineItemId(eq(lineIds))).thenReturn(true).thenReturn(false);

        softAssertions.assertThatCode(() -> TracerUtils.logIfActiveMatchLineItemIds(logger, tracer, lineIds, msg))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> TracerUtils.logIfActiveMatchLineItemIds(logger, tracer, lineIds, msg))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> TracerUtils.logIfActiveMatchLineItemIds(logger, tracer, lineIds, msg))
                .doesNotThrowAnyException();
        softAssertions.assertAll();

        verify(logger, times(1)).info(anyString(), eq(TracerUtils.TRACER), eq(msg));
        verify(logger, times(2)).info(eq(msg));
    }

    @Test
    void logIfActiveMatchBidderCode() {
        String bidderCode = "bc";
        String msg = "message";

        when(logger.isInfoEnabled()).thenReturn(true).thenReturn(true).thenReturn(true);
        when(tracer.isActive()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(tracer.matchBidderCode(eq(bidderCode))).thenReturn(true).thenReturn(false);

        softAssertions.assertThatCode(() -> TracerUtils.logIfActiveMatchBidderCode(logger, tracer, bidderCode, msg))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> TracerUtils.logIfActiveMatchBidderCode(logger, tracer, bidderCode, msg))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> TracerUtils.logIfActiveMatchBidderCode(logger, tracer, bidderCode, msg))
                .doesNotThrowAnyException();
        softAssertions.assertAll();

        verify(logger, times(1)).info(anyString(), eq(TracerUtils.TRACER), eq(msg));
        verify(logger, times(1)).info(eq(msg));
        verify(logger, times(1)).debug(eq(msg));
    }

    @Test
    void logStats() {
        List<DeliveryReportLineStatsDto> stats = Lists.newArrayList();
        DeliveryReportLineStatsDto deliveryReportLineStatsDto1 = DeliveryReportLineStatsDto.builder()
                .lineItemId("L1")
                .bidderCode("bc")
                .build();
        DeliveryReportLineStatsDto deliveryReportLineStatsDto2 = DeliveryReportLineStatsDto.builder()
                .lineItemId("L2")
                .bidderCode("bc")
                .build();
        stats.add(deliveryReportLineStatsDto1);
        stats.add(deliveryReportLineStatsDto2);

        when(tracer.isActive()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(tracer.matchBidderCode(eq("bc"))).thenReturn(true);
        when(tracer.matchLineItemId(eq("L1"))).thenReturn(true);
        when(tracer.matchLineItemId(eq("L2"))).thenReturn(false);

        softAssertions.assertThatCode(() -> TracerUtils.logStats(logger, tracer, stats))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> TracerUtils.logStats(logger, tracer, stats))
                .doesNotThrowAnyException();
        softAssertions.assertAll();

        verify(logger, times(1)).info(anyString(), eq(TracerUtils.TRACER), eq(deliveryReportLineStatsDto1));
        verify(logger, times(1)).debug(anyString(), eq(deliveryReportLineStatsDto1));
        verify(logger, times(2)).debug(anyString(), eq(deliveryReportLineStatsDto2));
    }

    @Test
    void logIfActiveTokenSummaries() {
        LatestTokenSpendSummary latestTokenSpendSummary = LatestTokenSpendSummary.builder()
                .vendor("vendor").region("region").bidderCode("bc").lineItemId("L1").build();
        List<LatestTokenSpendSummary> summaries = Collections.singletonList(latestTokenSpendSummary);

        when(tracer.isActive()).thenReturn(true);
        when(tracer.isMatchingOn(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        softAssertions.assertThatCode(() -> TracerUtils.logIfActiveTokenSummaries(logger, tracer, summaries))
                .doesNotThrowAnyException();

        softAssertions.assertAll();
    }

    @Test
    void logIfActiveMatchingOnVendorRegion() {
        LatestTokenSpendSummary latestTokenSpendSummary = LatestTokenSpendSummary.builder()
                .vendor("vendor").region("region").bidderCode("bc").lineItemId("L1").build();

        when(tracer.isActive()).thenReturn(true);
        when(tracer.isMatchingOn(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        softAssertions.assertThatCode(() -> TracerUtils.logIfActiveMatchingOnVendorRegion(logger, tracer, latestTokenSpendSummary))
                .doesNotThrowAnyException();

        softAssertions.assertAll();
    }

    @Test
    void logIfActiveMatchingOnLineItemStatus() {
        DeliveryReportFromPbsDto deliveryReportFromPbsDto = DeliveryReportFromPbsDto.builder()
                .vendor("vendor").region("region").build();
        JsonNode jsonNode = new POJONode("pojo");

        when(logger.isInfoEnabled()).thenReturn(true);
        when(tracer.isActive()).thenReturn(true);
        when(tracer.isMatchingOn(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        softAssertions.assertThatCode(() -> TracerUtils.logIfActiveMatchingOnLineItemStatus(logger, tracer, objectMapper, "", deliveryReportFromPbsDto, jsonNode))
                .doesNotThrowAnyException();

        softAssertions.assertAll();
    }

}