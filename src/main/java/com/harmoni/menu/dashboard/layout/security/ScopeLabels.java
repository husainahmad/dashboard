package com.harmoni.menu.dashboard.layout.security;

import com.harmoni.menu.dashboard.util.Messages;

/**
 * Presentation helpers for the BRAND / CHAIN / STORE scope values.
 */
public final class ScopeLabels {

    public static final String BRAND = "BRAND";
    public static final String CHAIN = "CHAIN";
    public static final String STORE = "STORE";

    private ScopeLabels() {
        throw new IllegalStateException("Utility class");
    }

    public static String label(String scopeType) {
        if (scopeType == null) {
            return "-";
        }
        return switch (scopeType) {
            case BRAND -> Messages.get("scope.brand");
            case CHAIN -> Messages.get("scope.chain");
            case STORE -> Messages.get("scope.store");
            default -> scopeType;
        };
    }

    public static String cssClass(String scopeType) {
        return "scope-badge-" + (scopeType == null ? "unknown" : scopeType.toLowerCase());
    }
}
