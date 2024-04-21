package com.harmoni.menu.dashboard.exception;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusinessServerRequestException extends RuntimeException {
    private final static Logger log = LoggerFactory.getLogger(BusinessServerRequestException.class);
    public BusinessServerRequestException(RestAPIResponse restAPIResponse) {
        log.warn("Serve Response : {}", restAPIResponse);
        Broadcaster.broadcast(STR."\{BroadcastMessage.BRAND_INSERT_FAILED}|\{restAPIResponse.getError()}");
    }
}
