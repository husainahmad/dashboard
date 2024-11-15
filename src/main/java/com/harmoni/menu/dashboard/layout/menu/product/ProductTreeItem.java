package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.SkuDto;
import com.harmoni.menu.dashboard.layout.enums.ProductItemType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductTreeItem {
    private String id;
    private String name;
    private ProductItemType productItemType;
    private Integer productId;
    private Integer skuId;
    private Integer categoryId;
    private String categoryName;
    private Double price;
    private String tierName;
    private List<SkuDto> skus;
}
