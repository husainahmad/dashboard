package com.harmoni.menu.dashboard.exception;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusinessBadRequestException extends RuntimeException {
    private final static Logger log = LoggerFactory.getLogger(BusinessBadRequestException.class);
    public BusinessBadRequestException(RestAPIResponse restAPIResponse) {
        log.warn("Serve Response : {}", restAPIResponse);
        Broadcaster.broadcast("%s|%s".formatted(restAPIResponse.getHttpStatus(), restAPIResponse.getError()));
    }
}
