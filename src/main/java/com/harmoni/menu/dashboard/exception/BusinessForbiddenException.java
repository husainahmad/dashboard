package com.harmoni.menu.dashboard.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Raised when the backend rejects a request with HTTP 403.
 *
 * <p>The signed-in user is authenticated but lacks the authority for the
 * operation, so retrying the same request cannot succeed. The exception
 * broadcasts {@link BroadcastMessage#FORBIDDEN} so the shell can show a single
 * permission message instead of a generic retry prompt.</p>
 */
@Slf4j
public class BusinessForbiddenException extends RuntimeException {

    @Getter
    private final transient RestAPIResponse restAPIResponse;

    public BusinessForbiddenException(RestAPIResponse restAPIResponse) {
        super(describe(restAPIResponse));
        this.restAPIResponse = restAPIResponse;
        log.warn("Forbidden response : {}", restAPIResponse);
        broadcast(restAPIResponse);
    }

    private static String describe(RestAPIResponse response) {
        if (response == null) {
            return "Request forbidden with no response body";
        }
        return "Request forbidden: httpStatus=" + response.getHttpStatus()
                + ", error=" + response.getError();
    }

    private static void broadcast(RestAPIResponse restAPIResponse) {
        try {
            Broadcaster.broadcast(ObjectUtil.objectToJsonString(BroadcastMessage.builder()
                    .type(BroadcastMessage.FORBIDDEN)
                    .data(restAPIResponse).build()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
