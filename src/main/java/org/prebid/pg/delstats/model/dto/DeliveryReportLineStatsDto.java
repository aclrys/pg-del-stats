package org.prebid.pg.delstats.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.sql.Timestamp;
import java.util.Comparator;

@Value
@Getter
@Builder
public class DeliveryReportLineStatsDto {

    String bidderCode;
    String lineItemId;
    Timestamp dataWindowStartTimestamp;
    Timestamp dataWindowEndTimestamp;
    Integer clientAuctions;
    Integer sentToBidder;
    Integer sentToClient;
    Integer sentToClientAsTopMatch;
    Integer accountAuctions;
    Integer winEvents;
    Timestamp reportTimestamp;

    public static final Comparator<DeliveryReportLineStatsDto> COMPARATOR_BY_BIDDER_CODE =
            Comparator.comparing((DeliveryReportLineStatsDto o) -> o.bidderCode);
    public static final Comparator<DeliveryReportLineStatsDto> COMPARATOR_BY_LINE_ITEM_ID =
            Comparator.comparing((DeliveryReportLineStatsDto o) -> o.lineItemId);
    public static final Comparator<DeliveryReportLineStatsDto> COMPARATOR_BY_BIDDER_CODE_LINE_ITEM_ID =
            Comparator.comparing((DeliveryReportLineStatsDto o) -> o.bidderCode + ":::" + o.lineItemId);
    public static final Comparator<DeliveryReportLineStatsDto> COMPARATOR_BY_LINE_ITEM_ID_BIDDER_CODE =
            Comparator.comparing((DeliveryReportLineStatsDto o) -> o.lineItemId + ":::" + o.bidderCode);

}
