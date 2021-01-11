package org.prebid.pg.delstats.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.prebid.pg.delstats.exception.InvalidLineItemIdFormatException;

import java.io.InputStream;
import java.util.List;

public class LineItemStatusUtilsTest {
    private SoftAssertions softAssertions;

    private ObjectMapper om;

    private List<JsonNode> lineItemStatusDtos;

    @BeforeEach
    public void setup() throws Exception {
        softAssertions = new SoftAssertions();

        om = new ObjectMapper();

        InputStream inputStream = LineItemStatusUtilsTest.class.getClassLoader().getResourceAsStream("LineItemStatus.json");

        lineItemStatusDtos = om.readValue(inputStream, new TypeReference<List<JsonNode>>(){});
    }

    @Test
    public void shouldSplitBidderCodeFromLineItem() throws Exception {
        JsonNode lineItemStatusJson = lineItemStatusDtos.get(0);
        String bidderCode = JacksonUtil.nullSafeGet(om, lineItemStatusJson, "bidderCode");
        String lineItemId = JacksonUtil.nullSafeGet(om, lineItemStatusJson, "lineItemId");
        String extLineItemId = JacksonUtil.nullSafeGet(om, lineItemStatusJson, "extLineItemId");
        if (!StringUtils.isBlank(bidderCode)) {
            String newLineItemId = bidderCode + "PG-" + lineItemId;
            ((ObjectNode) lineItemStatusJson).put("lineItemId", newLineItemId);
        }

        softAssertions.assertThatCode(() -> LineItemStatusUtils.fixup(om, lineItemStatusJson, "PG-"))
                .doesNotThrowAnyException();
        softAssertions.assertThat(lineItemStatusJson.get("lineItemId").asText()).isEqualTo(lineItemId);
        softAssertions.assertThat(lineItemStatusJson.get("extLineItemId").asText()).isEqualTo(extLineItemId);

        softAssertions.assertAll();
    }

    @Test
    public void shouldThrowExceptionForLineItemsWithNoSeparator() {

        softAssertions.assertThatCode(() -> LineItemStatusUtils.fixup(om, lineItemStatusDtos.get(0), "XXX"))
                .isInstanceOf(InvalidLineItemIdFormatException.class);
        softAssertions.assertAll();
    }
}
