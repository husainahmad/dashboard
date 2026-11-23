package com.harmoni.menu.dashboard.layout.navigation;

import com.harmoni.menu.dashboard.layout.DashboardView;
import com.harmoni.menu.dashboard.layout.crm.AiSalesAssistantLayout;
import com.harmoni.menu.dashboard.layout.crm.CampaignsLayout;
import com.harmoni.menu.dashboard.layout.crm.CustomerAnalyticsLayout;
import com.harmoni.menu.dashboard.layout.crm.LoyaltyLayout;
import com.harmoni.menu.dashboard.layout.crm.RecommendationsLayout;
import com.harmoni.menu.dashboard.layout.customer.CustomerLayout;
import com.harmoni.menu.dashboard.layout.menu.product.ProductLayout;
import com.harmoni.menu.dashboard.layout.security.AccessDeniedView;
import com.harmoni.menu.dashboard.layout.security.EmployeeRoleLayout;
import com.harmoni.menu.dashboard.support.UiTestSupport;
import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void crmItemsRequireCustomerReadPermission() {
        List<Class<? extends Component>> crmViews = List.of(
                CustomerLayout.class, LoyaltyLayout.class, CampaignsLayout.class,
                AiSalesAssistantLayout.class, RecommendationsLayout.class, CustomerAnalyticsLayout.class);
        for (Class<? extends Component> view : crmViews) {
            assertTrue(SideNavMenu.hasAccess(UiTestSupport.authorization("customer:read"), view),
                    view.getSimpleName() + " should be visible with customer:read");
            assertFalse(SideNavMenu.hasAccess(UiTestSupport.authorization("menu:view"), view),
                    view.getSimpleName() + " should be hidden without customer:read");
        }
    }

}
