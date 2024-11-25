package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class TierServiceDto {

    private Integer id;
    @NotEmpty
    @JsonProperty("tier")
    private TierDto tierDto;
    @NotEmpty
    private Integer subServiceId;

}