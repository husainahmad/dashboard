package com.harmoni.menu.dashboard.layout.organization.tier;

import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;

/**
 * Common shape of the nodes rendered by the tier tree-grid views, so both the
 * service-tier and menu-tier lists can share their naming, checkbox and action
 * column logic.
 */
public interface TierTreeItem {

    Integer getRootIndex();

    String getId();

    String getName();

    boolean isActive();

    void setActive(boolean active);

    TreeLevel getTreeLevel();
}