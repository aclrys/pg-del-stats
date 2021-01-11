package org.prebid.pg.delstats.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import org.prebid.pg.delstats.persistence.DeliveryReportSummary;

import java.util.Map;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

@Getter
@Setter
@Builder
public class DeliveryReportSummaryToPlannerAdapterDto {

    String reportId;

    Timestamp reportTimeStamp;

    Instant reportStartTimeStamp;

    Instant reportEndTimeStamp;

    @JsonProperty("deliveryReports")
    ArrayList<LineDeliverySummary> lineDeliverySummaries;

    public static DeliveryReportSummaryToPlannerAdapterDto buildDto(
            Instant reportStartTimeStamp, Instant reportEndTimeStamp,
            List<DeliveryReportSummary> reports, Map<String, Map<String, Integer>> lineToPlanTokensSpent
    ) {
        ArrayList<LineDeliverySummary> lineDeliverySummaries = new ArrayList<>(reports.size());
        for (DeliveryReportSummary report : reports) {
            LineDeliverySummary lineDeliverySummary
                    = LineDeliverySummary.builder()
                    .lineItemId(report.getLineItemId())
                    .extLineItemId(report.getExtLineItemId())
                    .wins(report.getWinEvents())
                    .targetMatched(report.getTargetMatched())
                    .accountAuctions(report.getAccountAuctions())
                    .deliverySchedules(buildDeliverySchedules(lineToPlanTokensSpent.get(report.getLineItemId())))
                    .build();
            lineDeliverySummaries.add(lineDeliverySummary);
        }
        return DeliveryReportSummaryToPlannerAdapterDto.builder()
                .reportId(UUID.randomUUID().toString())
                .reportTimeStamp(Timestamp.from(Instant.now()))
                .reportStartTimeStamp(reportStartTimeStamp)
                .reportEndTimeStamp(reportEndTimeStamp)
                .lineDeliverySummaries(lineDeliverySummaries)
                .build();
    }

    static ArrayList<DeliverySchedule> buildDeliverySchedules(Map<String, Integer> planToTokensSpent) {
        ArrayList<DeliverySchedule> deliverySchedules = new ArrayList<>(planToTokensSpent.size());

        for (String planId : planToTokensSpent.keySet()) {
            deliverySchedules.add(
                    DeliverySchedule.builder().planId(planId).tokensSpent(planToTokensSpent.get(planId)).build()
            );
        }

        return deliverySchedules;
    }
}

@Value
@Getter
@Builder
class LineDeliverySummary {

    String lineItemId;

    String extLineItemId;

    Integer wins;

    Integer targetMatched;

    Integer accountAuctions;

    @JsonProperty("deliverySchedule")
    ArrayList<DeliverySchedule> deliverySchedules;
}

@Value
@Getter
@Builder
class DeliverySchedule {

    String planId;

    Integer tokensSpent;
}



