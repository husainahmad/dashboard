package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
public class SkuDto {

    private Integer id;
    private String name;
    private Integer productId;
    @JsonProperty("product")
    private ProductDto productDto;
    private Date createdAt;
    private Date updatedAt;
    private Boolean isActive;

}