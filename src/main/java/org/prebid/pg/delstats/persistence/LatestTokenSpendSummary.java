package org.prebid.pg.delstats.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.io.Serializable;
import java.sql.Timestamp;

@Data
@Builder(toBuilder = true)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@IdClass(LatestTokenSpendSummary.IdClass.class)
public class LatestTokenSpendSummary {

    @Id
    String vendor;

    @Id
    String region;

    @Id
    String instanceId;

    @Id
    String bidderCode;

    @Id
    String lineItemId;

    String extLineItemId;

    Timestamp dataWindowStartTimestamp;

    Timestamp dataWindowEndTimestamp;

    Timestamp reportTimestamp;

    String serviceInstanceId;

    String summaryData;

    @Data
    static class IdClass implements Serializable {
        String vendor;
        String region;
        String instanceId;
        String bidderCode;
        String lineItemId;
    }
}
