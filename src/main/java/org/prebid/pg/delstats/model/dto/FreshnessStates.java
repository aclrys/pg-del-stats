package org.prebid.pg.delstats.model.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FreshnessStates {

    private List<SummaryReportFreshness> latestSummaryReportWindowEndTimestamps;

    @Data
    @Builder
    @EqualsAndHashCode
    public static class SummaryReportFreshness {

        private String serviceInstanceId;

        private String source;

        private String extLineItemId;

        private String ts;
    }

}

