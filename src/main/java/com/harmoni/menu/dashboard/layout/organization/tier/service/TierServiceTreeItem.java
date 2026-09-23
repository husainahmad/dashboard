package com.harmoni.menu.dashboard.layout.organization.tier.service;

import com.harmoni.menu.dashboard.layout.organization.tier.TierTreeItem;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import lombok.Builder;
import lombok.Data;

/**
 * Tree-grid item for the service-tier view. Carries the tier, service and
 * sub-service identities plus the activation flag and parent/level so the
 * {@code TreeGrid} can bind the hierarchy, checkboxes and per-row actions.
 */
@Data
@Builder
public class TierServiceTreeItem implements TierTreeItem {
    private Integer rootIndex;
    private String id;
    private String name;
    private Integer serviceId;
    private String serviceName;
    private Integer subServiceId;
    private String subServiceName;
    private boolean active;
    private TreeLevel treeLevel;
    private TierServiceTreeItem tierServiceTreeItemParent;
}
