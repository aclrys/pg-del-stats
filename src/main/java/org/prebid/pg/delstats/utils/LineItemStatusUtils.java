package org.prebid.pg.delstats.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.prebid.pg.delstats.exception.InvalidLineItemIdFormatException;
import org.springframework.util.StringUtils;

public class LineItemStatusUtils {
    private LineItemStatusUtils() {
    }

    public static void fixup(ObjectMapper objectMapper, JsonNode lineItemStatusJson, String separator
    ) throws JsonProcessingException {
        ObjectNode lineItemStatus = (ObjectNode) lineItemStatusJson;
        String bidderCode = JacksonUtil.nullSafeGet(objectMapper, lineItemStatusJson, "bidderCode");
        String extLineItemId = JacksonUtil.nullSafeGet(objectMapper, lineItemStatusJson, "extLineItemId");
        if (!StringUtils.isEmpty(bidderCode) && !StringUtils.isEmpty(extLineItemId)) {
            return;
        }
        String[] lineItemIdParts = JacksonUtil.nullSafeGet(objectMapper, lineItemStatusJson, "lineItemId")
                .split(separator);
        if (lineItemIdParts.length == 2) {
            if (StringUtils.isEmpty(bidderCode)) {
                lineItemStatus.put("bidderCode", lineItemIdParts[0]);
            }
            if (StringUtils.isEmpty(extLineItemId)) {
                lineItemStatus.put("lineItemId", lineItemIdParts[1]);
            }
        } else {
            throw new InvalidLineItemIdFormatException(
                    String.format("LineItemId %s does not contain '%s' as separator. Unable to set bidder code.",
                            lineItemStatus.get("linedItemId"), separator));
        }
    }

    public static String getExtLineItemId(String lineItemId, String separator) {
        String[] lineItemIdParts = lineItemId.split(separator);
        if (lineItemIdParts.length == 2) {
            return lineItemIdParts[1];
        } else {
            throw new InvalidLineItemIdFormatException(
                    String.format("LineItemId %s does not contain '%s' as separator. Unable to set extLineItemId.",
                            lineItemId, separator));
        }
    }
}
