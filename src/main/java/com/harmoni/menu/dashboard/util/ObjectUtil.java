package com.harmoni.menu.dashboard.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Objects;

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

    public static <T> T convertObjectToObject(Object object, TypeReference<T> typeReference) {
       return objectMapper.convertValue(
                Objects.requireNonNull(object), typeReference);
    }

    public static <T> T convertValueToObject(Object object, Class<T> tClass) {
        return objectMapper.convertValue(object, tClass);
    }

    public static boolean isNotEmpty(Object object) {
        return ObjectUtils.isNotEmpty(object);
    }
}
