package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Date;

@Data
public class StoreDto {

    private Integer id;
    @NotEmpty
    private String name;
    @NotEmpty
    private Integer tierId;
    @JsonProperty("tier")
    private TierDto tierDto;
    @NotEmpty
    private Integer brandId;
    @JsonProperty("brand")
    private BrandDto brandDto;
    private String address;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

}