package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class SkuDto {

    private Integer id;
    private String name;
    private Integer productId;
    @JsonProperty("product")
    private ProductDto productDto;
    @JsonProperty("tierPrice")
    private SkuTierPriceDto skuTierPriceDto;

    private Date createdAt;
    private Date updatedAt;
    private Boolean active;

}