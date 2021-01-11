package org.prebid.pg.delstats.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.sql.Timestamp;
import java.util.List;

@Value
@Getter
@Builder
public class DeliveryReportToPlannerAdapterDto {

    String reportId;

    Timestamp reportTimeStamp;

    Timestamp dataWindowStartTimeStamp;

    Timestamp dataWindowEndTimeStamp;

    List<DeliveryReportLineDto> deliveryReports;

}
