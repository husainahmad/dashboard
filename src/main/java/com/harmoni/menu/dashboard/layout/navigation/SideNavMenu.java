package com.harmoni.menu.dashboard.layout.navigation;

import com.harmoni.menu.dashboard.layout.DashboardView;
import com.harmoni.menu.dashboard.layout.customer.CustomerLayout;
import com.harmoni.menu.dashboard.layout.menu.customization.CustomizationLayout;
import com.harmoni.menu.dashboard.layout.menu.product.ProductLayout;
import com.harmoni.menu.dashboard.layout.organization.OrganizationLayout;
import com.harmoni.menu.dashboard.layout.organization.tier.TierLayout;
import com.harmoni.menu.dashboard.layout.organization.store.kitchen.KitchenLayout;
import com.harmoni.menu.dashboard.layout.security.EmployeeRoleLayout;
import com.harmoni.menu.dashboard.layout.setting.device.DeviceLayout;
import com.harmoni.menu.dashboard.layout.setting.service.ServiceLayout;
import com.harmoni.menu.dashboard.layout.setting.table.TableLayout;
import com.harmoni.menu.dashboard.layout.menu.category.CategoryLayout;
import com.harmoni.menu.dashboard.layout.menu.promotion.PromotionLayout;
import com.harmoni.menu.dashboard.layout.report.ReportLayout;
import com.harmoni.menu.dashboard.configuration.RbacProperties;
import com.harmoni.menu.dashboard.layout.security.RequiresPermission;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import lombok.extern.slf4j.Slf4j;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Side navigation rendered in the main layout drawer. Groups the POS
 * administration sections into collapsible entries that navigate to their
 * respective routes. Items the signed-in user cannot access are hidden.
 */
@Slf4j
public class SideNavMenu extends Div {

    private final AuthorizationService authorizationService;
    private final RbacProperties rbacProperties;

    /**
     * Builds the collapsible POS administration navigation groups.
     *
     * @param authorizationService decides which items the user may see
     * @param rbacProperties       whether the access-control module is enabled
     */
    public SideNavMenu(AuthorizationService authorizationService, RbacProperties rbacProperties) {
        this.authorizationService = authorizationService;
        this.rbacProperties = rbacProperties;
        this.addClassName("side-nav-sample");

        SideNav sideNavOverview = group("nav.overview");
        addIfPermitted(sideNavOverview, "nav.dashboard", DashboardView.class, VaadinIcon.HOME.create());

        SideNav sideNavCatalog = group("nav.catalog");
        addIfPermitted(sideNavCatalog, "nav.category", CategoryLayout.class, VaadinIcon.TAGS.create());
        addIfPermitted(sideNavCatalog, "nav.customization", CustomizationLayout.class, VaadinIcon.SLIDERS.create());
        addIfPermitted(sideNavCatalog, "nav.product", ProductLayout.class, VaadinIcon.COFFEE.create());
        addIfPermitted(sideNavCatalog, "nav.promotion", PromotionLayout.class, VaadinIcon.MONEY_EXCHANGE.create());

        SideNav sideNavCustomers = group("nav.customers");
        addIfPermitted(sideNavCustomers, "nav.customerList", CustomerLayout.class, VaadinIcon.USERS.create());

        SideNav sideNavOperations = group("nav.operations");
        addIfPermitted(sideNavOperations, "nav.kitchen", KitchenLayout.class, VaadinIcon.PRINT.create());
        addIfPermitted(sideNavOperations, "nav.table", TableLayout.class, VaadinIcon.TABLE.create());
        addIfPermitted(sideNavOperations, "nav.service", ServiceLayout.class, VaadinIcon.COG.create());
        addIfPermitted(sideNavOperations, "nav.devices", DeviceLayout.class, VaadinIcon.DESKTOP.create());

        SideNav sideNavReport = new SideNav();
        addIfPermitted(sideNavReport, "nav.report", ReportLayout.class, VaadinIcon.CHART_GRID.create());

        SideNav sideNavAdministration = group("nav.administration");
        addIfPermitted(sideNavAdministration, "nav.organization", OrganizationLayout.class, VaadinIcon.SITEMAP.create());
        addIfPermitted(sideNavAdministration, "nav.tier", TierLayout.class, VaadinIcon.GRID_BEVEL.create());
        if (rbacProperties.isEnabled()) {
            addIfPermitted(sideNavAdministration, "nav.employee", EmployeeRoleLayout.class,
                    VaadinIcon.USERS.create());
        }

        List<SideNav> groups = new ArrayList<>();
        groups.add(sideNavOverview);
        groups.add(sideNavAdministration);
        groups.add(sideNavCatalog);
        groups.add(sideNavCustomers);
        groups.add(sideNavOperations);
        groups.add(sideNavReport);

        VerticalLayout navWrapper = new VerticalLayout();
        navWrapper.setSpacing(true);
        navWrapper.setSizeUndefined();
        for (SideNav group : groups) {
            if (!group.getItems().isEmpty()) {
                group.setWidthFull();
                navWrapper.add(group);
            }
        }

        sideNavOverview.setExpanded(true);
        sideNavAdministration.setExpanded(false);
        sideNavCatalog.setExpanded(true);
        sideNavCustomers.setExpanded(false);
        sideNavOperations.setExpanded(false);

        Scroller scroller = new Scroller(new Div(navWrapper));
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        scroller.getStyle()
                .set(Css.BORDER_BOTTOM, "1px solid var(--lumo-contrast-20pct)")
                .set(Css.PADDING, "var(--lumo-space-m)");
        add(scroller);
    }

    private static SideNav group(String labelKey) {
        SideNav sideNav = new SideNav();
        sideNav.setLabel(Messages.get(labelKey));
        sideNav.setCollapsible(true);
        return sideNav;
    }

    private void addIfPermitted(SideNav sideNav, String labelKey, Class<? extends Component> view,
                                Icon icon) {
        if (hasAccess(authorizationService, view)) {
            sideNav.addItem(new SideNavItem(Messages.get(labelKey), view, icon));
        }
    }

    /**
     * Whether the navigation target may be shown. The route annotation is the
     * single source of truth, so the menu cannot drift from the route guard.
     *
     * @param authorizationService the signed-in user's authorities
     * @param view                 the navigation target
     * @return {@code true} when the target declares a permission the user holds
     */
    static boolean hasAccess(AuthorizationService authorizationService,
                             Class<? extends Component> view) {
        RequiresPermission required = view.getAnnotation(RequiresPermission.class);
        if (required == null) {
            log.warn("Navigation target {} has no @RequiresPermission; hiding it", view.getName());
            return false;
        }
        return authorizationService.hasAnyPermission(required.value());
    }

}
