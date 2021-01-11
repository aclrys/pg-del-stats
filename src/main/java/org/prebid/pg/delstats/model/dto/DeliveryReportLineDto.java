package org.prebid.pg.delstats.model.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.sql.Timestamp;

@Value
@Getter
@Builder
public class DeliveryReportLineDto {

    String vendor;
    String region;
    String instanceId;
    String bidderCode;
    String lineItemId;
    Timestamp dataWindowStartTimestamp;
    Timestamp dataWindowEndTimestamp;
    String reportId;
    @JsonRawValue
    String lineItemStatus;
    Timestamp reportTimestamp;

}
