package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.layout.enums.ProductItemAction;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class ProductBinderBean {
    private ProductItemAction itemAction;
    private Integer id;
    private Integer skuId;
    private String skuName;

    private Integer tierId;
    private Double price;
}
