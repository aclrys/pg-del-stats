package org.prebid.pg.delstats.model.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DeliveryReportStatsDto {

    List<DeliveryReportLineStatsDto> deliveryReportStats;
}
