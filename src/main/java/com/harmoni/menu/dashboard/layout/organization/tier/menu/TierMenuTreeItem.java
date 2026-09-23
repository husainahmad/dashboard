package com.harmoni.menu.dashboard.layout.organization.tier.menu;

import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.organization.tier.TierTreeItem;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import lombok.Builder;
import lombok.Data;

/**
 * Tree-grid item for the menu-tier view. Carries the owning {@link TierDto},
 * an optional {@link CategoryDto} child node, the activation flag and the
 * parent/level so the {@code TreeGrid} can bind the hierarchy, checkboxes and
 * per-row actions.
 */
@Data
@Builder
public class TierMenuTreeItem implements TierTreeItem {
    private Integer rootIndex;
    private TierDto tierDto;
    private String id;
    private String name;
    private CategoryDto categoryDto;
    private boolean active;
    private TreeLevel treeLevel;
    private TierMenuTreeItem itemParent;
}
