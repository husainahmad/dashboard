package com.harmoni.menu.dashboard.rest.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
public class RestAPIResponse {
    private long timeStamp;
    private int httpStatus;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object data = HttpStatus.CREATED;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object error;


}
