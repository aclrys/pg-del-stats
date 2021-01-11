package org.prebid.pg.delstats.exception;

public class ApiNotActiveException extends RuntimeException {
    public ApiNotActiveException(String errorMessage) {
        super(errorMessage);
    }
}
