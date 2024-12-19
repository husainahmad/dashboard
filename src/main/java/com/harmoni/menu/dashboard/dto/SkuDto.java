package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class SkuDto {

    private Integer id;
    private String name;
    private String description;
    private Integer productId;
    @JsonProperty("product")
    private ProductDto productDto;
    @JsonProperty("tierPrices")
    private List<SkuTierPriceDto> skuTierPriceDtos;

    private Date createdAt;
    private Date updatedAt;
    private Boolean active;

}