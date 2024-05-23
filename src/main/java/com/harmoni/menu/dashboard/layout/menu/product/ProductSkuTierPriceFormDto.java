package com.harmoni.menu.dashboard.layout.menu.product;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ProductSkuTierPriceFormDto {
    private Integer id;
    private Double price;
}
