package org.prebid.pg.delstats.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.exception.DeliveryReportValidationException;
import org.prebid.pg.delstats.model.dto.DeliveryReportFromPbsDto;
import org.prebid.pg.delstats.utils.MockSystemService;
import org.prebid.pg.delstats.utils.ResourceUtil;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeliveryReportProcessorTest {
    private SoftAssertions softAssertions;

    private DeliveryReportProcessor deliveryReportProcessor;

    private ServerConfiguration configuration;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUpBeforeEach() throws Exception {
        softAssertions = new SoftAssertions();

        configuration = mock(ServerConfiguration.class);
        lenient().when(configuration.getLineItemBidderCodeSeparator()).thenReturn("-");
        lenient().when(configuration.getDeliveryReportInstanceNameCacheExpirationUnit()).thenReturn(ChronoUnit.MINUTES);
        lenient().when(configuration.isValidationEnabled()).thenReturn(true);
        lenient().when(configuration.getLineItemSummaryMaxTimeRangeSeconds()).thenReturn(86400);
        lenient().when(configuration.getBidderAliasMappingString()).thenReturn("A:B");

        deliveryReportProcessor = new DeliveryReportProcessor(configuration, new MockSystemService());

        objectMapper = new ObjectMapper();
    }

    @Test
    public void shouldProcessValidReport() throws Exception {
        when(configuration.getLineItemBidderCodeSeparator()).thenReturn("-");

        List<Exception> caughtExceptions = new LinkedList<>();
        DeliveryReportFromPbsDto validDeliveryProgressReport =
                objectMapper.readValue(ResourceUtil.readFromClasspath("DeliveryProgressReportValidExample.json"),
                        DeliveryReportFromPbsDto.class);
        softAssertions.assertThatCode(() ->deliveryReportProcessor.processLineItemStatus(validDeliveryProgressReport,
                validDeliveryProgressReport.getLineItemStatus().get(0),
                Timestamp.from(Instant.now()), caughtExceptions)).doesNotThrowAnyException();
        softAssertions.assertThat(caughtExceptions).isEmpty();
        softAssertions.assertAll();
    }

    @Test
    public void shouldThrowExceptionWhenProcessingInvalidLineItemStatus() throws Exception {
        when(configuration.getLineItemBidderCodeSeparator()).thenReturn("-");

        List<Exception> caughtExceptions1 = new LinkedList<>();
        List<Exception> caughtExceptions2 = new LinkedList<>();
        List<Exception> caughtExceptions3 = new LinkedList<>();

        DeliveryReportFromPbsDto validDeliveryProgressReport =
                objectMapper.readValue(ResourceUtil.readFromClasspath("DeliveryProgressReportValidExample.json"),
                        DeliveryReportFromPbsDto.class);
        JsonNode emptyLineItemStatus = objectMapper.readTree("{}");
        JsonNode onlyBidderCodeLineItemStatus = objectMapper.readTree("{ \"bidderCode\": \"bidder\" }");
        JsonNode badJsonElement = objectMapper.readTree("{\"lineItemId\": \"bidderPG-1\", \"x\":[{\"field\":\"{\"}]}");

        softAssertions.assertThatCode(() ->deliveryReportProcessor.processLineItemStatus(validDeliveryProgressReport,
                emptyLineItemStatus, Timestamp.from(Instant.now()), caughtExceptions1)).doesNotThrowAnyException();
        softAssertions.assertThat(caughtExceptions1).isNotEmpty();
        softAssertions.assertThatCode(() ->deliveryReportProcessor.processLineItemStatus(validDeliveryProgressReport,
                onlyBidderCodeLineItemStatus, Timestamp.from(Instant.now()), caughtExceptions2)).doesNotThrowAnyException();
        softAssertions.assertThat(caughtExceptions2).isNotEmpty();
        softAssertions.assertThatCode(() ->deliveryReportProcessor.processLineItemStatus(validDeliveryProgressReport,
                badJsonElement, Timestamp.from(Instant.now()), caughtExceptions3)).doesNotThrowAnyException();
        softAssertions.assertThat(caughtExceptions3).isNotEmpty();
        softAssertions.assertAll();
    }

    @Test
    public void shouldParseValidDeliveryProgressReportToDto() throws Exception {
        when(configuration.getLineItemBidderCodeSeparator()).thenReturn("-");
        String validDeliveryProgressReport = ResourceUtil.readFromClasspath("DeliveryProgressReportValidExample.json");
        String testingDeliveryProgressReport = ResourceUtil.readFromClasspath("DeliveryProgressReportTestExample.json");
        softAssertions.assertThatCode(() -> objectMapper.readValue(validDeliveryProgressReport, DeliveryReportFromPbsDto.class))
                .doesNotThrowAnyException();
        DeliveryReportFromPbsDto deliveryReportFromPbsDto = objectMapper.readValue(testingDeliveryProgressReport, DeliveryReportFromPbsDto.class);
        softAssertions.assertThatCode(() -> deliveryReportProcessor.validateDeliveryReport(deliveryReportFromPbsDto))
                .doesNotThrowAnyException();
        softAssertions.assertAll();
    }

    @Test
    public void validateDeliveryReport() {
        when(configuration.getLineItemBidderCodeSeparator()).thenReturn("-");

        Instant baseTime = Instant.now();
        Timestamp start = Timestamp.from(baseTime.minusSeconds(360));
        Timestamp end = Timestamp.from(baseTime.plusSeconds(360));

        softAssertions.assertThatCode(() -> deliveryReportProcessor
                .validateDeliveryReport(makeDeliveryReportFromPbsDto("R", "V", "I", start, end)))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> deliveryReportProcessor
                .validateDeliveryReport(makeDeliveryReportFromPbsDto("R", "V", "I", end, start)))
                .isInstanceOf(DeliveryReportValidationException.class);
        softAssertions.assertThatCode(() -> deliveryReportProcessor
                .validateDeliveryReport(makeDeliveryReportFromPbsDto("", "V", "I", start, end)))
                .doesNotThrowAnyException();

        softAssertions.assertAll();
    }

    @Test
    void checkNodesRecursivelyForValidTimestampsShouldCatchBadTimestamps() throws Exception {
        when(configuration.getLineItemBidderCodeSeparator()).thenReturn("-");

        String validLineItemStatusArray = ResourceUtil.readFromClasspath("LineItemStatus.json");
        JsonNode validLineItemStatusArrayJson = objectMapper.readTree(validLineItemStatusArray);
        softAssertions.assertThatCode(() -> validLineItemStatusArrayJson.elements().forEachRemaining(
                element -> deliveryReportProcessor.checkNodesRecursivelyForValidTimestamps("root", element)))
                .doesNotThrowAnyException();

        softAssertions.assertAll();
    }

    public static DeliveryReportFromPbsDto makeDeliveryReportFromPbsDto(String reportId, String vendor, String instanceId, Timestamp start, Timestamp end) {
        return makeDeliveryReportFromPbsDtoWithLineItemStatus(reportId, vendor, instanceId, start, end, Lists.emptyList());
    }

    public static DeliveryReportFromPbsDto makeDeliveryReportFromPbsDtoWithLineItemStatus(String reportId, String vendor, String instanceId, Timestamp start, Timestamp end, List<JsonNode> lineItemsStatusList) {
        return DeliveryReportFromPbsDto.builder()
                .dataWindowStartTimeStamp(start)
                .dataWindowEndTimeStamp(end)
                .reportTimeStamp(Timestamp.from(Instant.now()))
                .reportId(reportId)
                .vendor(vendor)
                .region("fake-region")
                .instanceId(instanceId)
                .lineItemStatus(lineItemsStatusList)
                .build();
    }

}
