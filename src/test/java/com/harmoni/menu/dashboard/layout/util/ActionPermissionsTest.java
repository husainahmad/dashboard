package com.harmoni.menu.dashboard.layout.util;

import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.support.UiTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionPermissionsTest {

    @Test
    void catalogEditAcceptsMenuEditAndLegacyWrite() {
        assertTrue(ActionPermissions.canEditCatalog(auth("menu:edit")));
        assertTrue(ActionPermissions.canEditCatalog(auth("catalog:write")));
        assertTrue(ActionPermissions.canEditCatalog(auth("rbac:manage")));
        assertFalse(ActionPermissions.canEditCatalog(auth("menu:view")));
    }

    @Test
    void catalogDeleteIsSeparateFromEdit() {
        assertTrue(ActionPermissions.canDeleteCatalog(auth("catalog:delete")));
        assertFalse(ActionPermissions.canDeleteCatalog(auth("catalog:write")));
        assertFalse(ActionPermissions.canDeleteCatalog(auth("menu:edit")));
    }

    @Test
    void priceEditAcceptsMenuOrOrganizationWrite() {
        assertTrue(ActionPermissions.canEditPrice(auth("menu:edit")));
        assertTrue(ActionPermissions.canEditPrice(auth("organization:write")));
        assertFalse(ActionPermissions.canEditPrice(auth("menu:view")));
    }

    @Test
    void organizationEditAndDeleteUseTheirOwnPermissions() {
        assertTrue(ActionPermissions.canEditOrganization(auth("organization:write")));
        assertFalse(ActionPermissions.canEditOrganization(auth("organization:read")));
        assertTrue(ActionPermissions.canDeleteOrganization(auth("organization:delete")));
        assertFalse(ActionPermissions.canDeleteOrganization(auth("organization:write")));
    }

    private static AuthorizationService auth(String... permissions) {
        return UiTestSupport.authorization(permissions);
    }
}
