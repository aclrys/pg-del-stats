package org.prebid.pg.delstats.exception;

public class MissingDeliveryReportSummaryException extends RuntimeException {
    public MissingDeliveryReportSummaryException(String errorMessage) {
        super(errorMessage);
    }
}
