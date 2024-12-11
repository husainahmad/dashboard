package com.harmoni.menu.dashboard.layout.organization.tier.service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TierServiceTreeItem {
    private Integer rootIndex;
    private String id;
    private String name;
    private String serviceName;
    private String subServiceName;
    private boolean active;
    private TreeLevel treeLevel;
    private TierServiceTreeItem tierServiceTreeItemParent;
}
