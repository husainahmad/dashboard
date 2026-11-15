package com.harmoni.menu.dashboard.layout.navigation;

import com.harmoni.menu.dashboard.layout.DashboardView;
import com.harmoni.menu.dashboard.layout.customer.CustomerLayout;
import com.harmoni.menu.dashboard.layout.menu.customization.CustomizationLayout;
import com.harmoni.menu.dashboard.layout.menu.product.ProductLayout;
import com.harmoni.menu.dashboard.layout.organization.OrganizationLayout;
import com.harmoni.menu.dashboard.layout.organization.tier.TierLayout;
import com.harmoni.menu.dashboard.layout.organization.store.kitchen.KitchenLayout;
import com.harmoni.menu.dashboard.layout.organization.user.UserLayout;
import com.harmoni.menu.dashboard.layout.setting.service.ServiceLayout;
import com.harmoni.menu.dashboard.layout.setting.table.TableLayout;
import com.harmoni.menu.dashboard.layout.menu.category.CategoryLayout;
import com.harmoni.menu.dashboard.layout.menu.promotion.PromotionLayout;
import com.harmoni.menu.dashboard.layout.report.DailyReportView;
import com.harmoni.menu.dashboard.layout.report.OrderVolumeReportView;
import com.harmoni.menu.dashboard.layout.report.SalesReportView;
import com.harmoni.menu.dashboard.layout.report.SettlementReportView;
import com.harmoni.menu.dashboard.layout.report.TopProductReportView;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

/**
 * Side navigation rendered in the main layout drawer. Groups the POS
 * administration sections into collapsible entries that navigate to their
 * respective routes.
 */
public class SideNavMenu extends Div {

    /**
     * Builds the collapsible POS administration navigation groups.
     */
    public SideNavMenu() {
        this.addClassName("side-nav-sample");

        SideNav sideNavOverview = new SideNav();
        sideNavOverview.setLabel(Messages.get("nav.overview"));
        sideNavOverview.setCollapsible(true);
        sideNavOverview.addItem(new SideNavItem(Messages.get("nav.dashboard"), DashboardView.class, VaadinIcon.HOME.create()));

        SideNav sideNavCatalog = new SideNav();
        sideNavCatalog.setLabel(Messages.get("nav.catalog"));
        sideNavCatalog.setCollapsible(true);
        sideNavCatalog.addItem(new SideNavItem(Messages.get("nav.category"), CategoryLayout.class, VaadinIcon.TAGS.create()));
        sideNavCatalog.addItem(new SideNavItem(Messages.get("nav.customization"), CustomizationLayout.class, VaadinIcon.SLIDERS.create()));
        sideNavCatalog.addItem(new SideNavItem(Messages.get("nav.product"), ProductLayout.class, VaadinIcon.COFFEE.create()));
        sideNavCatalog.addItem(new SideNavItem(Messages.get("nav.promotion"), PromotionLayout.class, VaadinIcon.MONEY_EXCHANGE.create()));
        sideNavCatalog.addItem(placeholderItem(Messages.get("nav.inventoryStock"), VaadinIcon.CUBES.create()));

        SideNav sideNavCustomers = new SideNav();
        sideNavCustomers.setLabel(Messages.get("nav.customers"));
        sideNavCustomers.setCollapsible(true);
        sideNavCustomers.addItem(new SideNavItem(Messages.get("nav.customerList"),
                CustomerLayout.class, VaadinIcon.USERS.create()));

        SideNav sideNavOperations = new SideNav();
        sideNavOperations.setLabel(Messages.get("nav.operations"));
        sideNavOperations.setCollapsible(true);
        sideNavOperations.addItem(new SideNavItem(Messages.get("nav.kitchen"), KitchenLayout.class, VaadinIcon.PRINT.create()));
        sideNavOperations.addItem(new SideNavItem(Messages.get("nav.table"), TableLayout.class, VaadinIcon.TABLE.create()));
        sideNavOperations.addItem(new SideNavItem(Messages.get("nav.service"), ServiceLayout.class, VaadinIcon.COG.create()));

        SideNav sideNavReport = new SideNav();
        sideNavReport.setLabel(Messages.get("nav.report"));
        sideNavReport.setCollapsible(true);
        sideNavReport.addItem(new SideNavItem(Messages.get("nav.report.settlement"),
                SettlementReportView.class, VaadinIcon.CREDIT_CARD.create()));
        sideNavReport.addItem(new SideNavItem(Messages.get("nav.report.topProducts"),
                TopProductReportView.class, VaadinIcon.TROPHY.create()));
        sideNavReport.addItem(new SideNavItem(Messages.get("nav.report.daily"),
                DailyReportView.class, VaadinIcon.CALENDAR.create()));
        sideNavReport.addItem(new SideNavItem(Messages.get("nav.report.sales"),
                SalesReportView.class, VaadinIcon.CHART_GRID.create()));
        sideNavReport.addItem(new SideNavItem(Messages.get("nav.report.orderVolume"),
                OrderVolumeReportView.class, VaadinIcon.BAR_CHART.create()));

        SideNav sideNavAdministration = new SideNav();
        sideNavAdministration.setLabel(Messages.get("nav.administration"));
        sideNavAdministration.setCollapsible(true);
        sideNavAdministration.addItem(new SideNavItem(Messages.get("nav.organization"),
                OrganizationLayout.class, VaadinIcon.SITEMAP.create()));

        sideNavAdministration.addItem(new SideNavItem(Messages.get("nav.tier"),
                TierLayout.class, VaadinIcon.GRID_BEVEL.create()));
        sideNavAdministration.addItem(new SideNavItem(Messages.get("nav.user"), UserLayout.class, VaadinIcon.USER.create()));

        VerticalLayout navWrapper = new VerticalLayout(
                sideNavOverview,
                sideNavAdministration,
                sideNavCatalog,
                sideNavCustomers,
                sideNavOperations,
                sideNavReport);
        navWrapper.setSpacing(true);
        navWrapper.setSizeUndefined();
        sideNavOverview.setWidthFull();
        sideNavAdministration.setWidthFull();
        sideNavCatalog.setWidthFull();
        sideNavCustomers.setWidthFull();
        sideNavOperations.setWidthFull();
        sideNavReport.setWidthFull();

        sideNavOverview.setExpanded(true);
        sideNavAdministration.setExpanded(false);
        sideNavCatalog.setExpanded(true);
        sideNavCustomers.setExpanded(false);
        sideNavOperations.setExpanded(false);
        sideNavReport.setExpanded(false);

        Scroller scroller = new Scroller(new Div(navWrapper));
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        scroller.getStyle()
                .set(Css.BORDER_BOTTOM, "1px solid var(--lumo-contrast-20pct)")
                .set(Css.PADDING, "var(--lumo-space-m)");
        add(scroller);
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
