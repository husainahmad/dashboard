package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class TierDto {

    private Integer id;
    @NotEmpty
    private String name;
    @NotEmpty
    private Integer brandId;
    @NotEmpty
    private TierTypeDto type;
    @NotEmpty
    @JsonProperty("brand")
    private BrandDto brandDto;
    @NotEmpty
    @JsonProperty("service")
    private ServiceDto serviceDto;
    @JsonProperty("tierServices")
    private List<TierServiceDto> tierServices;

    private Date createdAt;
    private Date updatedAt;

}