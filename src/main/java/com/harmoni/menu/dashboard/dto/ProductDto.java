package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ProductDto {

    private Integer id;
    private String name;
    @JsonProperty("description")
    private String description;
    private Integer categoryId;
    @JsonProperty("category")
    private CategoryDto categoryDto;
    @JsonProperty("skus")
    private List<SkuDto> skuDtos;
    @JsonProperty("productImage")
    private ProductImageDto productImageDto;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;
}