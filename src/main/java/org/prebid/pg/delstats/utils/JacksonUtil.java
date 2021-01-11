package org.prebid.pg.delstats.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

public class JacksonUtil {

    private JacksonUtil() {
    }

    public static String nullSafeGet(ObjectMapper objectMapper, JsonNode jsonNode, String fieldName)
            throws JsonProcessingException {
        return nullSafeGet(objectMapper, jsonNode, fieldName, "", "");
    }

    public static String nullSafeGet(ObjectMapper objectMapper, JsonNode jsonNode, String fieldName,
                                 String defaultValueIfNull, String defaultValue) throws JsonProcessingException {
        JsonNode fieldNode = jsonNode.get(fieldName);
        if (fieldNode == null) {
            return defaultValueIfNull;
        } else {
            if (fieldNode instanceof TextNode) {
                return fieldNode.asText(defaultValue);
            }
            return objectMapper.writeValueAsString(fieldNode);
        }
    }
}
