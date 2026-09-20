package com.harmoni.menu.dashboard.layout.menu.customization;

import com.harmoni.menu.dashboard.dto.CustomizationDto;
import com.harmoni.menu.dashboard.layout.enums.CustomizationItemType;
import lombok.Builder;
import lombok.Data;

/**
 * Tree node backing {@link CustomizationListView}'s grid: a root node is a
 * customization, expanded into option nodes and per-tier price leaf nodes.
 */
@Data
@Builder
public class CustomizationTreeItem {

    private String id;
    private String name;
    private CustomizationItemType type;
    private Integer customizationId;
    private Long optionId;
    private String optionStatus;
    private Integer tierId;
    private String tierName;
    private Double price;
    private String selectionTypeLabel;
    private CustomizationDto customizationDto;
}