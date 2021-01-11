package org.prebid.pg.delstats.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class PlanDataSummary {

    private String lineItemId;

    private String planData;
}
