package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Envelope returned by the menu backend for every REST call.
 *
 * <p>Carries the response timestamp, HTTP status and either the payload in
 * {@link #data} or the server-side error details in {@link #error}. Used by the
 * reactive clients to surface business failures.
 */
@Getter
@Builder
@ToString
public class RestAPIResponse {

    /** When the response was produced by the server. */
    private long timeStamp;
    /** The HTTP status code of the response. */
    private int httpStatus;
    /** The response payload, {@code null} when the call failed. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object data;
    /** Server-side error details, {@code null} on success. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object error;

}
