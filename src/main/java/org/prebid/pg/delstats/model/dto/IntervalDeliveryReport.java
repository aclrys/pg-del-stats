package org.prebid.pg.delstats.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.prebid.pg.delstats.persistence.DeliveryReport;

import java.time.Instant;

@Getter
@Setter
@Builder
public class IntervalDeliveryReport {

    private DeliveryReport deliveryReport;

    private Instant reportWindowStartTimestamp;

    private Instant reportWindowEndTimestamp;
}

