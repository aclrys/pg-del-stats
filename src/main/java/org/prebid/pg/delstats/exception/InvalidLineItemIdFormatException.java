package org.prebid.pg.delstats.exception;

import java.util.Collections;
import java.util.List;

public class InvalidLineItemIdFormatException extends DeliveryReportValidationException {
    private final List<String> errorMessages;

    public InvalidLineItemIdFormatException(String errorMessage) {
        super(errorMessage);
        this.errorMessages = Collections.singletonList(errorMessage);
    }

    @Override
    public List<String> getErrorMessages() {
        return errorMessages;
    }

}
