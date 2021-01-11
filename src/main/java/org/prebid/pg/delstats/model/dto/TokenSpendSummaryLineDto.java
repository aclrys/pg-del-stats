package org.prebid.pg.delstats.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

import java.sql.Timestamp;

@Value
@Builder
public class TokenSpendSummaryLineDto {

    String reportId;

    String vendor;

    String region;

    String instanceId;

    String bidderCode;

    String lineItemId;

    String extLineItemId;

    Timestamp dataWindowStartTimestamp;

    Timestamp dataWindowEndTimestamp;

    Timestamp reportTimestamp;

    String serviceInstanceId;

    JsonNode summaryData;
}

