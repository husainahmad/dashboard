package com.harmoni.menu.dashboard.layout.menu.product.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class ProductFormDto {
    private Integer id;
    private String name;
    private Integer categoryId;
    private List<ProductSkuFormDto> skus;
}
