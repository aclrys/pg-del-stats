package org.prebid.pg.delstats.exception;

import java.util.Collections;
import java.util.List;

public class DeliveryReportProcessingException extends RuntimeException {
    private final List<String> errorMessages;

    public DeliveryReportProcessingException(String errorMessage) {
        super(errorMessage);
        this.errorMessages = Collections.singletonList(errorMessage);
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

}
