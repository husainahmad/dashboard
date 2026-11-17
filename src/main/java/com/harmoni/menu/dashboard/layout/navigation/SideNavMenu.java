package com.harmoni.menu.dashboard.layout.navigation;

import com.harmoni.menu.dashboard.layout.DashboardView;
import com.harmoni.menu.dashboard.layout.customer.CustomerLayout;
import com.harmoni.menu.dashboard.layout.menu.customization.CustomizationLayout;
import com.harmoni.menu.dashboard.layout.menu.product.ProductLayout;
import com.harmoni.menu.dashboard.layout.organization.OrganizationLayout;
import com.harmoni.menu.dashboard.layout.organization.tier.TierLayout;
import com.harmoni.menu.dashboard.layout.organization.store.kitchen.KitchenLayout;
import com.harmoni.menu.dashboard.layout.security.EmployeeRoleLayout;
import com.harmoni.menu.dashboard.layout.setting.service.ServiceLayout;
import com.harmoni.menu.dashboard.layout.setting.table.TableLayout;
import com.harmoni.menu.dashboard.layout.menu.category.CategoryLayout;
import com.harmoni.menu.dashboard.layout.menu.promotion.PromotionLayout;
import com.harmoni.menu.dashboard.layout.report.ReportLayout;
import com.harmoni.menu.dashboard.configuration.RbacProperties;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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
        sideNavOverview.addItem(new SideNavItem(Messages.get("nav.dashboard"), DashboardView.class, VaadinIcon.HOME.create()));

        SideNav sideNavCatalog = group("nav.catalog");
        addIfPermitted(sideNavCatalog, "nav.category", CategoryLayout.class, VaadinIcon.TAGS.create(), Permissions.MENU_VIEW);
        addIfPermitted(sideNavCatalog, "nav.customization", CustomizationLayout.class, VaadinIcon.SLIDERS.create(), Permissions.MENU_VIEW);
        addIfPermitted(sideNavCatalog, "nav.product", ProductLayout.class, VaadinIcon.COFFEE.create(), Permissions.MENU_VIEW);
        addIfPermitted(sideNavCatalog, "nav.promotion", PromotionLayout.class, VaadinIcon.MONEY_EXCHANGE.create(), Permissions.MENU_VIEW);
        sideNavCatalog.addItem(placeholderItem(Messages.get("nav.inventoryStock"), VaadinIcon.CUBES.create()));

        SideNav sideNavCustomers = group("nav.customers");
        addIfPermitted(sideNavCustomers, "nav.customerList", CustomerLayout.class, VaadinIcon.USERS.create(), Permissions.CUSTOMER_READ);

        SideNav sideNavOperations = group("nav.operations");
        addIfPermitted(sideNavOperations, "nav.kitchen", KitchenLayout.class, VaadinIcon.PRINT.create(), Permissions.OPERATION_READ);
        addIfPermitted(sideNavOperations, "nav.table", TableLayout.class, VaadinIcon.TABLE.create(), Permissions.TABLE_VIEW);
        addIfPermitted(sideNavOperations, "nav.service", ServiceLayout.class, VaadinIcon.COG.create(), Permissions.TERMINAL_VIEW);

        SideNav sideNavReport = new SideNav();
        addIfPermitted(sideNavReport, "nav.report", ReportLayout.class, VaadinIcon.CHART_GRID.create(), Permissions.REPORT_VIEW);

        SideNav sideNavAdministration = group("nav.administration");
        addIfPermitted(sideNavAdministration, "nav.organization", OrganizationLayout.class, VaadinIcon.SITEMAP.create(), Permissions.ORGANIZATION_READ);
        addIfPermitted(sideNavAdministration, "nav.tier", TierLayout.class, VaadinIcon.GRID_BEVEL.create(), Permissions.ORGANIZATION_READ);
        if (rbacProperties.isEnabled()) {
            addIfPermitted(sideNavAdministration, "nav.employee", EmployeeRoleLayout.class,
                    VaadinIcon.USERS.create(), Permissions.EMPLOYEE_VIEW);
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
                                Icon icon, String permission) {
        if (authorizationService.hasPermission(permission)) {
            sideNav.addItem(new SideNavItem(Messages.get(labelKey), view, icon));
        }
    }

    /**
     * Builds a disabled navigation item for a section that is not implemented
     * yet, so the group shows its intended shape without offering a dead link.
     *
     * @param label the item caption
     * @param icon  the icon shown before the caption
     * @return the disabled item
     */
    private static SideNavItem placeholderItem(String label, Icon icon) {
        SideNavItem item = new SideNavItem(label);
        item.setPrefixComponent(icon);
        item.setEnabled(false);
        return item;
    }
}
