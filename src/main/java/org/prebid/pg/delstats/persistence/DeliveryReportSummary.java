package org.prebid.pg.delstats.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "delivery_progress_reports_summary")
@Getter
@Setter
@Builder
@ToString
public class DeliveryReportSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private Instant reportWindowStartTimestamp;

    private Instant reportWindowEndTimestamp;

    private Instant dataWindowStartTimestamp;

    private Instant dataWindowEndTimestamp;

    private String lineItemId;

    private String extLineItemId;

    private String bidderCode;

    private String lineItemSource;

    private int accountAuctions;

    private int domainMatched;

    private int targetMatched;

    private int targetMatchedButFcapped;

    private int targetMatchedButFcapLookupFailed;

    private int pacingDeferred;

    private int sentToBidder;

    private int sentToBidderAsTopMatch;

    private int receivedFromBidderInvalidated;

    private int receivedFromBidder;

    private int sentToClient;

    private int sentToClientAsTopMatch;

    private int winEvents;

    private String planData;

    private Instant createdAt;

}

