package org.prebid.pg.delstats.exception;

public class SystemInitializationException extends RuntimeException {
    public SystemInitializationException(String errorMessage) {
        super(errorMessage);
    }
}
