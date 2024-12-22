package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkuTreeItem {
    private String id;
    private Integer skuId;
    private String skuName;
    private String skuDesc;
    private Integer tierId;
    private String tierName;
    private Double price;
    private TreeLevel treeLevel;
}

