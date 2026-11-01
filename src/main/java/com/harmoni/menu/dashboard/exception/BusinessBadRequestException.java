package com.harmoni.menu.dashboard.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BusinessBadRequestException extends RuntimeException {
    @lombok.Getter
    private final transient RestAPIResponse restAPIResponse;

    public BusinessBadRequestException(RestAPIResponse restAPIResponse) {
        super("Server responded httpStatus=" + (restAPIResponse == null ? "null"
                : restAPIResponse.getHttpStatus()) + " error=" + (restAPIResponse == null ? "null"
                : restAPIResponse.getError()));
        this.restAPIResponse = restAPIResponse;
        log.warn("Serve Response : {}", restAPIResponse);
        try {
            Broadcaster.broadcast(ObjectUtil.objectToJsonString(BroadcastMessage.builder()
                    .type(BroadcastMessage.BAD_REQUEST_FAILED)
                    .data(restAPIResponse).build()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }

    }
}
