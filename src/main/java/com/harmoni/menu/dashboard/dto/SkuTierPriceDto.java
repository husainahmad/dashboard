package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class SkuTierPriceDto {

    private Integer id;
    private Integer skuId;
    private Integer tierId;
    @JsonProperty("tier")
    private TierDto tierDto;
    private Double price;
    private Date createdAt;
    private Date updatedAt;

}