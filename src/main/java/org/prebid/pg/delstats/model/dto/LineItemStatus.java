package org.prebid.pg.delstats.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LineItemStatus {

    private String lineItemId;

    private String extLineItemId;

    private String lineItemSource;

    private String bidderCode;

    private int domainMatched;

    private int targetMatched;

    private int accountAuctions;

    private int sentToBidder;

    private int sentToBidderAsTopMatch;

    private int sentToClient;

    private int sentToClientAsTopMatch;

    private int targetMatchedButFcapped;

    private int receivedFromBidder;

    private int receivedFromBidderInvalidated;

    private int targetMatchedButFcapLookupFailed;

    private int pacingDeferred;

    private List<Event> events;

}

