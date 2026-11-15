package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.util.ObjectUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * What the operator sees when a request fails: a short summary for the banner
 * and the server envelope for the details view.
 */
public final class ErrorDetail {

    private static final DateTimeFormatter REFERENCE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").withZone(ZoneId.systemDefault());

    private final String summary;
    private final Integer httpStatus;
    private final Long timeStamp;
    private final Object error;
    private final Object data;

    private ErrorDetail(String summary, Integer httpStatus, Long timeStamp, Object error, Object data) {
        this.summary = summary;
        this.httpStatus = httpStatus;
        this.timeStamp = timeStamp;
        this.error = error;
        this.data = data;
    }

    /**
     * Builds the detail from the envelope the backend returned.
     *
     * @param summary  the short operator-facing message
     * @param response the failed response, may be {@code null}
     * @return the populated detail
     */
    public static ErrorDetail from(String summary, RestAPIResponse response) {
        if (response == null) {
            return new ErrorDetail(summary, null, null, null, null);
        }
        return new ErrorDetail(summary, response.getHttpStatus() <= 0 ? null : response.getHttpStatus(),
                response.getTimeStamp() == 0 ? null : response.getTimeStamp(),
                response.getError(), response.getData());
    }

    /**
     * Builds the detail when the response could not be decoded.
     *
     * @param summary the message to show
     * @param raw     the raw failure text
     * @return the populated detail
     */
    public static ErrorDetail raw(String summary, String raw) {
        return new ErrorDetail(summary, null, null, raw, null);
    }

    public String getSummary() {
        return summary;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public Object getError() {
        return error;
    }

    public Object getData() {
        return data;
    }

    /**
     * A short, user-safe identifier the operator can quote to support. It is
     * derived from the failure time, so it lines up with the same moment in the
     * server log without exposing the payload.
     *
     * @return the support reference, e.g. {@code ERR-20261115-1123}
     */
    public String getReference() {
        long moment = timeStamp == null ? System.currentTimeMillis() : timeStamp;
        return "ERR-" + REFERENCE_FORMAT.format(Instant.ofEpochMilli(moment));
    }

    /**
     * Renders the server envelope as the copyable, pretty-printed text shown in
     * the details dialog.
     *
     * @return the technical detail
     */
    public String technicalText() {
        StringBuilder text = new StringBuilder();
        text.append("reference: ").append(getReference()).append('\n');
        text.append("summary: ").append(summary == null ? "-" : summary).append('\n');
        if (httpStatus != null) {
            text.append("httpStatus: ").append(httpStatus).append('\n');
        }
        if (timeStamp != null) {
            text.append("timeStamp: ").append(timeStamp).append('\n');
        }
        if (error != null) {
            text.append("error: ").append(ObjectUtil.objectToPrettyJsonString(error)).append('\n');
        }
        if (data != null) {
            text.append("data: ").append(ObjectUtil.objectToPrettyJsonString(data)).append('\n');
        }
        return text.toString();
    }
}
