package com.harmoni.menu.dashboard.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BusinessServerRequestException extends RuntimeException {
    public BusinessServerRequestException(RestAPIResponse restAPIResponse) {
        super(describe(restAPIResponse));
        log.warn("Serve Response : {}", restAPIResponse);
        try {
            Broadcaster.broadcast(ObjectUtil.objectToJsonString(BroadcastMessage.builder()
                    .type(BroadcastMessage.PROCESS_FAILED)
                    .data(restAPIResponse).build()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Builds the exception message from the failed response.
     *
     * <p>Without this the exception renders as {@code null}, which hides the
     * only clue about a failing backend: Spring's default error body carries the
     * status and error name but no message, and the payload of interest often
     * sits in {@code data} rather than {@code error}.</p>
     *
     * @param response the failed response, may be {@code null}
     * @return a message naming the status and whatever detail the service sent
     */
    private static String describe(RestAPIResponse response) {
        if (response == null) {
            return "Server request failed with no response body";
        }
        return "Server request failed: httpStatus=" + response.getHttpStatus()
                + ", error=" + response.getError()
                + ", data=" + response.getData();
    }
}
