package org.prebid.pg.delstats.controller.handler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.hibernate.exception.ConstraintViolationException;
import org.prebid.pg.delstats.exception.ApiNotActiveException;
import org.prebid.pg.delstats.exception.DeliveryReportProcessingException;
import org.prebid.pg.delstats.exception.DeliveryReportValidationException;
import org.prebid.pg.delstats.exception.InvalidLineItemIdFormatException;
import org.prebid.pg.delstats.exception.InvalidRequestException;
import org.prebid.pg.delstats.exception.InvalidTimestampFormatException;
import org.prebid.pg.delstats.exception.MissingTransactionIdException;
import org.prebid.pg.delstats.metrics.GraphiteMetricsRecorder;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLIntegrityConstraintViolationException;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {
    static final String DELIVERY_REPORT_VALIDATION_DEFAULT_MESSAGE = "Delivery Report Failed Validation";
    static final String DELIVERY_REPORT_PROCESSING_DEFAULT_MESSAGE = "Delivery Report Failed Processing";
    static final String MISSING_TRANSACTION_ID_DEFAULT_MESSAGE = "Missing or Invalid Transaction Id";
    static final String INVALID_TIMESTAMP_FORMAT_DEFAULT_MESSAGE = "Invalid Timestamp Format";
    static final String INVALID_LINEITEM_FORMAT_DEFAULT_MESSAGE = "Invalid LineItem Format";
    static final String MESSAGE_NOT_READABLE_MESSAGE_PREFIX = "Message Not Readable: ";
    static final String INVALID_JSON_MAPPING_MESSAGE_PREFIX = "Invalid Json Mapping: ";
    static final String INVALID_JSON_PARSING_MESSAGE_PREFIX = "Invalid Json Parsing: ";

    GraphiteMetricsRecorder graphiteMetricsRecorder;

    public RestResponseEntityExceptionHandler(GraphiteMetricsRecorder graphiteMetricsRecorder) {
        this.graphiteMetricsRecorder = graphiteMetricsRecorder;
    }

    @ExceptionHandler(value = { ApiNotActiveException.class })
    protected ResponseEntity<Object> handleApiNotActiveException(RuntimeException ex, WebRequest request) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(value = { DataAccessException.class })
    protected ResponseEntity<Object> handleDataAccessException(DataAccessException ex, WebRequest request) {
        // For timeouts, return REQUEST_TIMEOUT (408)
        if (ex instanceof QueryTimeoutException) {
            graphiteMetricsRecorder.markQueryTimedOutMeter();
            return handleExceptionInternal(ex, null, new HttpHeaders(), HttpStatus.REQUEST_TIMEOUT, request);
        }
        // For duplicate keys, return CONFLICT (409)
        if (ex.getCause() instanceof ConstraintViolationException) {
            ConstraintViolationException ce = (ConstraintViolationException) ex.getCause();
            if (ce.getSQLException() instanceof SQLIntegrityConstraintViolationException) {
                SQLIntegrityConstraintViolationException se =
                        (SQLIntegrityConstraintViolationException) ce.getSQLException();
                graphiteMetricsRecorder.markDuplicateKeyErrorMeter();
                return handleExceptionInternal(ex, se.getMessage(), new HttpHeaders(),
                        HttpStatus.CONFLICT, request);
            }
        }
        if (ex instanceof DuplicateKeyException) {
            graphiteMetricsRecorder.markDuplicateKeyErrorMeter();
            return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.CONFLICT, request);
        }
        graphiteMetricsRecorder.markDataAccessErrorMeter();
        if (ex instanceof DataIntegrityViolationException) {
            DataIntegrityViolationException dataIntegrityViolationException = (DataIntegrityViolationException) ex;
            return handleExceptionInternal(ex, dataIntegrityViolationException.getMostSpecificCause().getMessage(),
                    new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
        }
        // Otherwise, don't know what caused this exception, so use generic BAD_REQUEST (400)
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = { DeliveryReportValidationException.class })
    protected ResponseEntity<Object> handleDeliveryReportValidationException(RuntimeException ex, WebRequest request) {
        String bodyOfResponse = DELIVERY_REPORT_VALIDATION_DEFAULT_MESSAGE;
        if (ex instanceof DeliveryReportValidationException) {
            DeliveryReportValidationException reportValidationException = (DeliveryReportValidationException) ex;
            bodyOfResponse = String.join("", reportValidationException.getErrorMessages());
        }
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = { DeliveryReportProcessingException.class })
    protected ResponseEntity<Object> handleDeliveryReportProcessingException(RuntimeException ex, WebRequest request) {
        String bodyOfResponse = DELIVERY_REPORT_PROCESSING_DEFAULT_MESSAGE;
        if (ex instanceof DeliveryReportProcessingException) {
            DeliveryReportProcessingException deliveryReportProcessingException =
                    (DeliveryReportProcessingException) ex;
            bodyOfResponse = String.join("", deliveryReportProcessingException.getErrorMessages());
        }
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = { MissingTransactionIdException.class })
    protected ResponseEntity<Object> handleMissingTransactionIds(RuntimeException ex, WebRequest request) {
        graphiteMetricsRecorder.markMissingTransactionIdMeter();
        String bodyOfResponse = MISSING_TRANSACTION_ID_DEFAULT_MESSAGE;
        if (ex instanceof MissingTransactionIdException) {
            MissingTransactionIdException missingTransactionIdException = (MissingTransactionIdException) ex;
            bodyOfResponse = String.join("", missingTransactionIdException.getErrorMessages());
        }
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = { InvalidLineItemIdFormatException.class })
    protected ResponseEntity<Object> handleFailedLineItemConversions(RuntimeException ex, WebRequest request) {
        graphiteMetricsRecorder.markInvalidRequestMeter();
        String bodyOfResponse = INVALID_LINEITEM_FORMAT_DEFAULT_MESSAGE;
        if (ex instanceof InvalidLineItemIdFormatException) {
            InvalidLineItemIdFormatException invalidLineItemIdFormatException = (InvalidLineItemIdFormatException) ex;
            bodyOfResponse = String.join("", invalidLineItemIdFormatException.getErrorMessages());
        }
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = { InvalidTimestampFormatException.class })
    protected ResponseEntity<Object> handleFailedTimestampConversions(RuntimeException ex, WebRequest request) {
        graphiteMetricsRecorder.markInvalidRequestMeter();
        String bodyOfResponse = INVALID_TIMESTAMP_FORMAT_DEFAULT_MESSAGE;
        if (ex instanceof InvalidTimestampFormatException) {
            InvalidTimestampFormatException invalidTimestampFormatException = (InvalidTimestampFormatException) ex;
            bodyOfResponse = String.join("", invalidTimestampFormatException.getErrorMessages());
        }
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = { InvalidRequestException.class })
    protected ResponseEntity<Object> handleInvalidRequest(RuntimeException ex, WebRequest request) {
        graphiteMetricsRecorder.markInvalidRequestMeter();
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        graphiteMetricsRecorder.markInvalidRequestMeter();
        String bodyOfResponse = MESSAGE_NOT_READABLE_MESSAGE_PREFIX + ex.getMessage();
        if (ex.getCause() instanceof JsonMappingException) {
            JsonMappingException jsonMappingException = (JsonMappingException) ex.getCause();
            bodyOfResponse = INVALID_JSON_MAPPING_MESSAGE_PREFIX + jsonMappingException.getMessage();
        }
        if (ex.getCause() instanceof JsonParseException) {
            JsonParseException jsonParseException = (JsonParseException) ex.getCause();
            bodyOfResponse = INVALID_JSON_PARSING_MESSAGE_PREFIX + jsonParseException.getMessage();
        }
        return handleExceptionInternal(ex, bodyOfResponse, headers, HttpStatus.BAD_REQUEST, request);
    }
}
