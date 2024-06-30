package com.harmoni.menu.dashboard.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.menu.dashboard.component.BroadcastMessage;

public final class ObjectUtil {

    private ObjectUtil() {
    }

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    public static String objectToJsonString(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }
    public static Object jsonStringToBroadcastMessageClass(String string) throws JsonProcessingException {
        return objectMapper.readValue(string, BroadcastMessage.class);
    }
}
