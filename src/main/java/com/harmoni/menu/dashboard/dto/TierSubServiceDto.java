package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.Data;


@Data
public class TierSubServiceDto {

    private Integer id;

    @JsonProperty("service")
    private ServiceDto serviceDto;

    @JsonProperty("tier")
    private @Valid TierDto tierDto;

    @JsonProperty("subService")
    private SubServiceDto subServiceDto;

    @JsonProperty("subServiceId")
    private Integer subServiceId;

    @JsonProperty("active")
    private boolean active;

}