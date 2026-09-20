package com.harmoni.menu.dashboard.layout.menu.customization;

import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import lombok.Builder;
import lombok.Data;

/**
 * Mutable node backing one row of the option grid in {@link CustomizationForm}:
 * a root node models a customization option and its children model the
 * per-tier prices for that option.
 */
@Data
@Builder
public class CustomizationOptionTreeItem {
    private String id;
    private Long optionId;
    private String name;
    private String status;
    private Integer tierId;
    private String tierName;
    private Double price;
    private TreeLevel treeLevel;
}