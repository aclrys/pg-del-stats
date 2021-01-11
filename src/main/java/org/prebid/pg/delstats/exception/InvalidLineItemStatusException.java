package org.prebid.pg.delstats.exception;

public class InvalidLineItemStatusException extends RuntimeException {

    public InvalidLineItemStatusException(String message, Throwable ex) {
        super(message, ex);
    }

}

