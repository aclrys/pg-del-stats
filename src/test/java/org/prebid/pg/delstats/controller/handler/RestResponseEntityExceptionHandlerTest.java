package org.prebid.pg.delstats.controller.handler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.prebid.pg.delstats.exception.*;
import org.prebid.pg.delstats.config.GraphiteConfig;
import org.prebid.pg.delstats.metrics.GraphiteMetricsRecorder;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.WebRequest;

import static org.mockito.Mockito.mock;

public class RestResponseEntityExceptionHandlerTest {
    SoftAssertions softAssertions;

    @BeforeEach
    public void setup() {
        softAssertions = new SoftAssertions();
    }

    @Test
    public void shouldReturnExpectedMessagesBasedOnException() {
        GraphiteConfig graphiteConfig = new GraphiteConfig();
        GraphiteMetricsRecorder graphiteMetricsRecorder = new GraphiteMetricsRecorder(graphiteConfig);
        graphiteMetricsRecorder.init();
        RestResponseEntityExceptionHandler handler = new RestResponseEntityExceptionHandler(graphiteMetricsRecorder);

        WebRequest webRequest = mock(WebRequest.class);
        JsonParser jsonParser = mock(JsonParser.class);
        HttpInputMessage httpInputMessage = mock(HttpInputMessage.class);

        DataAccessException dataAccessException = new DataAccessResourceFailureException("test");
        DeliveryReportValidationException deliveryReportValidationException = new DeliveryReportValidationException("test");
        DeliveryReportProcessingException deliveryReportProcessingException = new DeliveryReportProcessingException("test");
        DuplicateKeyException duplicateKeyException = new DuplicateKeyException("test");
        QueryTimeoutException queryTimeoutException = new QueryTimeoutException("test");
        MissingTransactionIdException missingTransactionIdException = new MissingTransactionIdException("test");
        InvalidLineItemIdFormatException invalidLineItemIdFormatException = new InvalidLineItemIdFormatException("test");
        InvalidTimestampFormatException invalidTimestampFormatException = new InvalidTimestampFormatException("test");
        HttpMessageNotReadableException basicHttpMessageNotReadableException =
                new HttpMessageNotReadableException("test", httpInputMessage);
        JsonMappingException jsonMappingException = new JsonMappingException(jsonParser, "jsonMap");
        JsonParseException jsonParseException = new JsonParseException(jsonParser, "jsonParse");
        HttpMessageNotReadableException jsonMappingHttpMessageNotReadableException =
                new HttpMessageNotReadableException("test", jsonMappingException, httpInputMessage);
        HttpMessageNotReadableException jsonParsingHttpMessageNotReadableException =
                new HttpMessageNotReadableException("test", jsonParseException, httpInputMessage);

        softAssertions.assertThat(handler.handleDataAccessException(dataAccessException, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("body", "test");
        softAssertions.assertThat(handler.handleDataAccessException(duplicateKeyException, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.CONFLICT)
                .hasFieldOrPropertyWithValue("body", "test");
        softAssertions.assertThat(handler.handleDataAccessException(queryTimeoutException, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.REQUEST_TIMEOUT)
                .hasFieldOrPropertyWithValue("body", null);

        softAssertions.assertThat(handler.handleMissingTransactionIds(missingTransactionIdException, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("body", "test");
        softAssertions.assertThat(handler.handleMissingTransactionIds(invalidTimestampFormatException, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("body", RestResponseEntityExceptionHandler.MISSING_TRANSACTION_ID_DEFAULT_MESSAGE);

        softAssertions.assertThat(handler.handleFailedLineItemConversions(invalidLineItemIdFormatException, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("body", "test");
        softAssertions.assertThat(handler.handleFailedLineItemConversions(missingTransactionIdException, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("body", RestResponseEntityExceptionHandler.INVALID_LINEITEM_FORMAT_DEFAULT_MESSAGE);

        softAssertions.assertThat(handler.handleFailedTimestampConversions(invalidTimestampFormatException, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("body", "test");
        softAssertions.assertThat(handler.handleFailedTimestampConversions(missingTransactionIdException, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("body", RestResponseEntityExceptionHandler.INVALID_TIMESTAMP_FORMAT_DEFAULT_MESSAGE);

        softAssertions.assertThat(handler.handleHttpMessageNotReadable(basicHttpMessageNotReadableException, new HttpHeaders(), HttpStatus.OK, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("body", RestResponseEntityExceptionHandler.MESSAGE_NOT_READABLE_MESSAGE_PREFIX + "test");
        softAssertions.assertThat(handler.handleHttpMessageNotReadable(jsonMappingHttpMessageNotReadableException, new HttpHeaders(), HttpStatus.OK, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("body", RestResponseEntityExceptionHandler.INVALID_JSON_MAPPING_MESSAGE_PREFIX + "jsonMap");
        softAssertions.assertThat(handler.handleHttpMessageNotReadable(jsonParsingHttpMessageNotReadableException, new HttpHeaders(), HttpStatus.OK, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("body", RestResponseEntityExceptionHandler.INVALID_JSON_PARSING_MESSAGE_PREFIX + "jsonParse");

        softAssertions.assertThat(handler.handleFailedTimestampConversions(new NullPointerException(), webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("body", RestResponseEntityExceptionHandler.INVALID_TIMESTAMP_FORMAT_DEFAULT_MESSAGE);

        softAssertions.assertThat(handler.handleDeliveryReportValidationException(deliveryReportValidationException, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("body", "test");
        softAssertions.assertThat(handler.handleDeliveryReportValidationException(deliveryReportProcessingException, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("body", RestResponseEntityExceptionHandler.DELIVERY_REPORT_VALIDATION_DEFAULT_MESSAGE);

        softAssertions.assertThat(handler.handleDeliveryReportProcessingException(deliveryReportProcessingException, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("body", "test");
        softAssertions.assertThat(handler.handleDeliveryReportProcessingException(deliveryReportValidationException, webRequest))
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST)
                .hasFieldOrPropertyWithValue("body", RestResponseEntityExceptionHandler.DELIVERY_REPORT_PROCESSING_DEFAULT_MESSAGE);

        softAssertions.assertAll();
    }
}
