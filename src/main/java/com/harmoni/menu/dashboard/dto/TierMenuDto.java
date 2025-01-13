package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;


@Data
public class TierMenuDto {

    private Integer id;
    private Integer tierId;
    @JsonProperty("tier")
    private TierDto tierDto;

    private Integer categoryId;
    @JsonProperty("category")
    private CategoryDto categoryDto;

    private Boolean active;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

}