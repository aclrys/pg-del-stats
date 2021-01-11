package org.prebid.pg.delstats.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "delivery_progress_reports")
@Builder
@IdClass(DeliveryReport.IdClass.class)
@EqualsAndHashCode(of = {"reportId", "lineItemId"})
public class DeliveryReport {

    @Id
    String reportId;

    String vendor;

    String region;

    // Instance name of PBS providing the report (contained in report itself)
    String instanceId;

    String bidderCode;

    @Id
    String lineItemId;

    String extLineItemId;

    Timestamp dataWindowStartTimestamp;

    Timestamp dataWindowEndTimestamp;

    String lineItemStatus;

    Timestamp reportTimestamp;

    Integer clientAuctions;

    @Data
    static class IdClass implements Serializable {
        String reportId;
        String lineItemId;
    }
}
