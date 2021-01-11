package org.prebid.pg.delstats.exception;

import java.util.Collections;
import java.util.List;

public class DeliveryReportValidationException extends RuntimeException {
    private final List<String> errorMessages;

    public DeliveryReportValidationException(String errorMessage) {
        super(errorMessage);
        this.errorMessages = Collections.singletonList(errorMessage);
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

}
