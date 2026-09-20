package com.harmoni.menu.dashboard.layout.organization.tier.service;

/**
 * Depth of a node in the tier tree-grid views: a tier root ({@code ROOT}), a
 * service group ({@code PARENT}) or a child node such as a sub-service or
 * category ({@code CHILD}).
 */
public enum TreeLevel {
    ROOT, PARENT, CHILD
}
