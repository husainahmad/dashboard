package com.harmoni.menu.dashboard.layout.menu.product.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ProductSkuFormDto {
    private Integer id;
    private String name;
    private ProductSkuTierPriceFormDto tierPrice;
}
