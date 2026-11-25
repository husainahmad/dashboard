package com.harmoni.menu.dashboard.layout.navigation;

import com.harmoni.menu.dashboard.layout.DashboardView;
import com.harmoni.menu.dashboard.layout.customer.CustomerLayout;
import com.harmoni.menu.dashboard.layout.menu.product.ProductLayout;
import com.harmoni.menu.dashboard.layout.security.AccessDeniedView;
import com.harmoni.menu.dashboard.layout.security.EmployeeRoleLayout;
import com.harmoni.menu.dashboard.support.UiTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SideNavMenuTest {

    @Test
    void withoutPermissions_nothingIsVisible() {
        assertFalse(SideNavMenu.hasAccess(UiTestSupport.authorization(), DashboardView.class));
        assertFalse(SideNavMenu.hasAccess(UiTestSupport.authorization(), ProductLayout.class));
    }

    @Test
    void dashboardRequiresDashboardViewPermission() {
        assertFalse(SideNavMenu.hasAccess(UiTestSupport.authorization("menu:view"), DashboardView.class));
        assertTrue(SideNavMenu.hasAccess(UiTestSupport.authorization("dashboard:view"), DashboardView.class));
    }

    @Test
    void catalogItemsUseTheirRouteAnnotation() {
        assertTrue(SideNavMenu.hasAccess(UiTestSupport.authorization("menu:view"), ProductLayout.class));
        assertFalse(SideNavMenu.hasAccess(UiTestSupport.authorization("dashboard:view"), ProductLayout.class));
    }

    @Test
    void employeeItemRequiresEmployeeView() {
        assertTrue(SideNavMenu.hasAccess(
                UiTestSupport.authorization("employee:view"), EmployeeRoleLayout.class));
        assertFalse(SideNavMenu.hasAccess(
                UiTestSupport.authorization("menu:view"), EmployeeRoleLayout.class));
    }

    @Test
    void unguardedRoutesAreNotNavigable() {
        assertFalse(SideNavMenu.hasAccess(UiTestSupport.authorization("rbac:manage"), AccessDeniedView.class));
    }

    @Test
    void customerListRequiresCustomerReadPermission() {
        assertTrue(SideNavMenu.hasAccess(UiTestSupport.authorization("customer:read"), CustomerLayout.class));
        assertFalse(SideNavMenu.hasAccess(UiTestSupport.authorization("menu:view"), CustomerLayout.class));
    }

}
