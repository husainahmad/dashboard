package com.harmoni.menu.dashboard.layout.util;

import com.harmoni.menu.dashboard.service.AuthorizationService;

/**
 * Central write-permission rules for the dashboard actions.
 *
 * <p>Each rule mirrors the authorities accepted by the menu service, including
 * the transitional legacy names and the {@code rbac:manage} override, so views
 * do not have to repeat the same permission set.</p>
 */
public final class ActionPermissions {

    private ActionPermissions() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Whether the user may create or edit catalog records.
     *
     * @param authorizationService the signed-in user's authorities
     * @return {@code true} when catalog editing is allowed
     */
    public static boolean canEditCatalog(AuthorizationService authorizationService) {
        return authorizationService.hasAnyPermission(
                Permissions.MENU_EDIT, Permissions.CATALOG_WRITE, Permissions.RBAC_MANAGE);
    }

    /**
     * Whether the user may delete catalog records.
     *
     * @param authorizationService the signed-in user's authorities
     * @return {@code true} when catalog deletion is allowed
     */
    public static boolean canDeleteCatalog(AuthorizationService authorizationService) {
        return authorizationService.hasAnyPermission(
                Permissions.CATALOG_DELETE, Permissions.RBAC_MANAGE);
    }

    /**
     * Whether the user may edit SKU or customization prices.
     *
     * @param authorizationService the signed-in user's authorities
     * @return {@code true} when price editing is allowed
     */
    public static boolean canEditPrice(AuthorizationService authorizationService) {
        return authorizationService.hasAnyPermission(
                Permissions.MENU_EDIT, Permissions.CATALOG_WRITE,
                Permissions.ORGANIZATION_WRITE, Permissions.RBAC_MANAGE);
    }

    /**
     * Whether the user may create or edit organization and tier records.
     *
     * @param authorizationService the signed-in user's authorities
     * @return {@code true} when organization editing is allowed
     */
    public static boolean canEditOrganization(AuthorizationService authorizationService) {
        return authorizationService.hasAnyPermission(
                Permissions.ORGANIZATION_WRITE, Permissions.RBAC_MANAGE);
    }

    /**
     * Whether the user may delete organization and tier records.
     *
     * @param authorizationService the signed-in user's authorities
     * @return {@code true} when organization deletion is allowed
     */
    public static boolean canDeleteOrganization(AuthorizationService authorizationService) {
        return authorizationService.hasAnyPermission(
                Permissions.ORGANIZATION_DELETE, Permissions.RBAC_MANAGE);
    }

    /**
     * Whether the user may edit terminal devices.
     *
     * @param authorizationService the signed-in user's authorities
     * @return {@code true} when device editing is allowed
     */
    public static boolean canEditTerminal(AuthorizationService authorizationService) {
        return authorizationService.hasAnyPermission(
                Permissions.TERMINAL_EDIT, Permissions.RBAC_MANAGE);
    }

    /**
     * Whether the user may manage role templates and permissions.
     *
     * @param authorizationService the signed-in user's authorities
     * @return {@code true} when RBAC management is allowed
     */
    public static boolean canManageRbac(AuthorizationService authorizationService) {
        return authorizationService.hasAnyPermission(
                Permissions.RBAC_MANAGE, Permissions.RBAC_WRITE);
    }
}
