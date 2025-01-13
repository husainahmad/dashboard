package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.Data;


@Data
public class TierMenuDto {

    private Integer id;

    @JsonProperty("tier")
    private @Valid TierDto tierDto;

    @JsonProperty("active")
    private boolean active;

}