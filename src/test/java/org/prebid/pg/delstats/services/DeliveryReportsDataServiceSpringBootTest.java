package org.prebid.pg.delstats.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.prebid.pg.delstats.config.DeploymentConfiguration;
import org.prebid.pg.delstats.config.ServerConfiguration;
import org.prebid.pg.delstats.metrics.GraphiteMetricsRecorder;
import org.prebid.pg.delstats.model.dto.DeliveryReportFromPbsDto;
import org.prebid.pg.delstats.repository.DeliveryProgressReportsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;

@SpringBootTest
class DeliveryReportsDataServiceSpringBootTest {
    private SoftAssertions softAssertions;

    @Autowired
    DeliveryProgressReportsRepository deliveryProgressReportsRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    private DeliveryReportProcessor deliveryReportProcessor;

    @Autowired
    private DeliveryReportsDataService deliveryReportsDataService;

    @Autowired
    private ServerConfiguration configuration;

    @Autowired
    private DeploymentConfiguration deploymentConfiguration;

    @Autowired
    private GraphiteMetricsRecorder graphiteMetricsRecorder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUpBeforeEach() throws Exception {
        softAssertions = new SoftAssertions();
    }

    @AfterEach
    public void clean() {
        entityManager.clear();
    }

    @Transactional
    @Test
    void shouldFindRecentPostedReports() throws Exception {
        Instant now = Instant.now();
        Timestamp nowTS = Timestamp.from(now);
        Timestamp then1Min1SecAgoTS = Timestamp.from(now.minusSeconds(61));
        Timestamp then1MinAgoTS = Timestamp.from(now.minusSeconds(60));
        Timestamp then2MinAgoTS = Timestamp.from(now.minusSeconds(120));
        Timestamp then2SecAgoTS = Timestamp.from(now.minusSeconds(2));
        Timestamp then1SecAgoTS = Timestamp.from(now.minusSeconds(1));
        String jsonTemplate = "[{\"%s\":\"%s\",\"%s\":\"%s\"}]";
        String lineItemStatusJson1 = String.format(jsonTemplate, "lineItemSource", "bidder", "lineItemId", "bidderPG-1");
        String lineItemStatusJson2 = String.format(jsonTemplate, "lineItemSource", "bidder", "lineItemId", "bidderPG-2");
        JsonNode lineItemStatus1 = objectMapper.readTree(lineItemStatusJson1);
        JsonNode lineItemStatus2 = objectMapper.readTree(lineItemStatusJson2);


        DeliveryReportFromPbsDto deliveryReportFromPbsDto1 = DeliveryReportFromPbsDto.builder()
                .reportId("R1")
                .instanceId("I1")
                .vendor("V1")
                .region("R1")
                .clientAuctions(5)
                .reportTimeStamp(then1SecAgoTS)
                .dataWindowStartTimeStamp(then1Min1SecAgoTS)
                .dataWindowEndTimeStamp(then2SecAgoTS)
                .lineItemStatus(Lists.newArrayList(lineItemStatus1))
                .build();
        DeliveryReportFromPbsDto deliveryReportFromPbsDto2 = DeliveryReportFromPbsDto.builder()
                .reportId("R2")
                .instanceId("I2")
                .vendor("V1")
                .region("R1")
                .clientAuctions(15)
                .reportTimeStamp(then1MinAgoTS)
                .dataWindowStartTimeStamp(then2MinAgoTS)
                .dataWindowEndTimeStamp(then1Min1SecAgoTS)
                .lineItemStatus(Lists.newArrayList(lineItemStatus2))
                .build();
        softAssertions.assertThatCode(() -> deliveryReportsDataService.storeLines(deliveryReportFromPbsDto1, then1SecAgoTS))
                .doesNotThrowAnyException();
        softAssertions.assertThatCode(() -> deliveryReportsDataService.storeLines(deliveryReportFromPbsDto2, then1MinAgoTS))
                .doesNotThrowAnyException();
        softAssertions.assertThat(deliveryReportsDataService
                .retrieveByBidderCode("bidder", then1Min1SecAgoTS.toString(), null).getDeliveryReports())
                .hasSize(2);
        softAssertions.assertThat(deliveryReportsDataService
                .retrieveByBidderCode("bidder", then1SecAgoTS.toString(), null).getDeliveryReports())
                .hasSize(1);
        softAssertions.assertThat(deliveryReportsDataService
                .retrieveByBidderCode("bidder", nowTS.toString(), null).getDeliveryReports())
                .hasSize(0);
        softAssertions.assertThat(deliveryReportsDataService
                .retrieveByBidderCode("bidder", then1Min1SecAgoTS.toString(), nowTS.toString()).getDeliveryReports())
                .hasSize(2);
        softAssertions.assertAll();
    }
}
