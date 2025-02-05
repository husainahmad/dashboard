package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Date;

@Data
public class CategoryDto {

    private Integer id;
    private String name;
    private String description;
    private Integer brandId;
    @JsonProperty("brand")
    private BrandDto brandDto;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;

}