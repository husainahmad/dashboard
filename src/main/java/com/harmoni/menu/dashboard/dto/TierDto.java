package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
public class TierDto {

    private Integer id;
    @NotEmpty
    private String name;
    @NotEmpty
    private Integer brandId;
    @NotEmpty
    @JsonProperty("brand")
    private BrandDto brandDto;
    private Date createdAt;
    private Date updatedAt;

}