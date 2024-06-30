package com.harmoni.menu.dashboard.layout.menu.product.binder;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class ProductBinderBean {
    private Integer id;
    private Integer skuId;
    private String skuName;

    private Integer tierId;
    private Double price;
}
