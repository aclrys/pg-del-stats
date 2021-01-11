package org.prebid.pg.delstats.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.prebid.pg.delstats.model.dto.DeliveryReportFromPbsDto;
import org.prebid.pg.delstats.repository.DeliveryProgressReportsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@SpringBootTest
public class ServiceControllerSpringBootTest {
    SoftAssertions softAssertions;

    @Autowired
    ServiceController serviceController;

    @Autowired
    DeliveryProgressReportsRepository deliveryProgressReportsRepository;

    ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        softAssertions = new SoftAssertions();
        objectMapper = new ObjectMapper();
    }

    @Test
    @Transactional
    public void shouldGetReportsRespectingReportingTimestamp() throws Exception {
        Instant iNow = Instant.now();
        Timestamp now = Timestamp.from(iNow);
        Timestamp then15minsAgo = Timestamp.from(iNow.minus(15, ChronoUnit.MINUTES));
        Timestamp then16minsAgo = Timestamp.from(iNow.minus(16, ChronoUnit.MINUTES));
        String jsonTemplate = "{\"%s\":\"%s\",\"%s\":\"%s\"}";
        String lineItemStatusJson1 = String.format(jsonTemplate, "lineItemSource", "bidder", "lineItemId", "bidderPG-1");
        String lineItemStatusJson2 = String.format(jsonTemplate, "lineItemSource", "bidder", "lineItemId", "bidderPG-2");
        String lineItemStatusJson3 = String.format(jsonTemplate, "lineItemSource", "bidder", "lineItemId", "bidderPG-3");
        JsonNode lineItemStatus1 = objectMapper.readTree(lineItemStatusJson1);
        JsonNode lineItemStatus2 = objectMapper.readTree(lineItemStatusJson2);
        JsonNode lineItemStatus3 = objectMapper.readTree(lineItemStatusJson3);
        DeliveryReportFromPbsDto deliveryReportFromPbsDto = DeliveryReportFromPbsDto.builder()
                .reportId("reportId")
                .vendor("vendor")
                .region("region")
                .instanceId(UUID.randomUUID().toString())
                .reportTimeStamp(then15minsAgo)
                .lineItemStatus(Lists.list(lineItemStatus1, lineItemStatus2, lineItemStatus3))
                .dataWindowStartTimeStamp(then16minsAgo)
                .dataWindowEndTimeStamp(then15minsAgo)
                .reportTimeStamp(then15minsAgo)
                .clientAuctions(0)
                .build();
        softAssertions.assertThatCode(() ->serviceController.storeReport(deliveryReportFromPbsDto))
                .doesNotThrowAnyException();

        softAssertions.assertThat(
                serviceController.getDeliveryReports(null , "bidder", then16minsAgo.toString(),
                        Timestamp.from(iNow.plusSeconds(60)).toString())
                        .getDeliveryReports()).hasSize(3);
        softAssertions.assertAll();
    }
}
