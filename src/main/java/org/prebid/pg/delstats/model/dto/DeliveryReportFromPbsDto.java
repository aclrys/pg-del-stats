package org.prebid.pg.delstats.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;

import java.sql.Timestamp;
import java.util.List;

@Value
@Getter
@Builder
@ToString
public class DeliveryReportFromPbsDto {

    String reportId;

    Timestamp reportTimeStamp;

    Timestamp dataWindowStartTimeStamp;

    Timestamp dataWindowEndTimeStamp;

    String vendor;

    String region;

    String instanceId;

    Integer clientAuctions;

    List<JsonNode> lineItemStatus;
}
