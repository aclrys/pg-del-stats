package org.prebid.pg.delstats.exception;

import java.util.Collections;
import java.util.List;

public class MissingTransactionIdException extends DeliveryReportValidationException {

    private final List<String> errorMessages;

    public MissingTransactionIdException(String errorMessage) {
        super(errorMessage);
        this.errorMessages = Collections.singletonList(errorMessage);
    }

    @Override
    public List<String> getErrorMessages() {
        return errorMessages;
    }

}
