package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Date;

@Data
public class ChainDto {

    private Integer id;
    @NotEmpty
    private String name;
    @NotEmpty
    private Integer brandId;
    @JsonProperty("brand")
    private BrandDto brandDto;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

}