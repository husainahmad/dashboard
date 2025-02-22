package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class TierServiceDto {

    private Integer id;
    private Integer tierId;

    @JsonProperty("tier")
    private TierDto tierDto;

    @JsonProperty("subService")
    private SubServiceDto subServiceDto;

    private boolean active;
    private Date createdAt;
    private Date updatedAt;

}