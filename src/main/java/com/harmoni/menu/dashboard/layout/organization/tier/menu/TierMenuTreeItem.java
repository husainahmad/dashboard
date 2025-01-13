package com.harmoni.menu.dashboard.layout.organization.tier.menu;

import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TierMenuTreeItem {
    private Integer rootIndex;
    private TierDto tierDto;
    private String id;
    private String name;
    private CategoryDto categoryDto;
    private boolean active;
    private TreeLevel treeLevel;
    private TierMenuTreeItem itemParent;
}
