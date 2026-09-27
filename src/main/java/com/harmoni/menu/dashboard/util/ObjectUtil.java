package com.harmoni.menu.dashboard.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Objects;

/**
 * JSON/object conversion helpers built on a lenient Jackson
 * {@link ObjectMapper}.
 *
 * <p>The shared mapper ignores unknown properties and allows unquoted field
 * names, so it can convert arbitrary backend payloads without failing on
 * fields the DTOs do not model.
 *
 * <p>{@link JavaTimeModule} is registered because the promotion DTOs carry
 * {@code LocalDate}, {@code LocalTime} and {@code LocalDateTime}; without it
 * Jackson refuses to map them. Dates are written as ISO-8601 text rather than
 * numeric arrays, which is both what the menu service sends and what
 * {@code spring.jackson} does by default, so payloads keep matching the
 * contract the backend documents.
 */
public final class ObjectUtil {

    private ObjectUtil() {
    }

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
