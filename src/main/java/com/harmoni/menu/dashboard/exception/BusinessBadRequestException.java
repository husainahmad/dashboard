package com.harmoni.menu.dashboard.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusinessBadRequestException extends RuntimeException {
    private final static Logger log = LoggerFactory.getLogger(BusinessBadRequestException.class);
    public BusinessBadRequestException(RestAPIResponse restAPIResponse) {
        log.warn("Serve Response : {}", restAPIResponse);
        try {
            Broadcaster.broadcast(ObjectUtil.objectToJsonString(BroadcastMessage.builder()
                    .type(BroadcastMessage.BAD_REQUEST_FAILED)
                    .data(restAPIResponse).build()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
