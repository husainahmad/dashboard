package com.harmoni.menu.dashboard.layout.organization.tier.menu;

import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TierMenuTreeItem {
    private Integer rootIndex;
    private Integer tierId;
    private String id;
    private String name;
    private Integer categoryId;
    private String categoryName;
    private boolean active;
    private TreeLevel treeLevel;
}
