package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class TierServiceDto {

    private Integer id;

    @NotEmpty
    @JsonProperty("tier")
    private TierDto tierDto;

    @JsonProperty("subServices")
    private List<SubServiceDto> subServiceDtos;

}