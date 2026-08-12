package com.harmoni.menu.dashboard.layout.navigation;

import com.harmoni.menu.dashboard.layout.menu.customization.CustomizationLayout;
import com.harmoni.menu.dashboard.layout.menu.customization.CustomizationListView;
import com.harmoni.menu.dashboard.layout.menu.product.ProductLayout;
import com.harmoni.menu.dashboard.layout.organization.store.StoreLayout;
import com.harmoni.menu.dashboard.layout.organization.tier.menu.TierMenuListView;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TierServiceListView;
import com.harmoni.menu.dashboard.layout.organization.user.UserLayout;
import com.harmoni.menu.dashboard.layout.setting.service.ServiceListView;
import com.harmoni.menu.dashboard.layout.organization.brand.BrandListView;
import com.harmoni.menu.dashboard.layout.menu.category.CategoryListView;
import com.harmoni.menu.dashboard.layout.organization.chain.ChainListView;
import com.harmoni.menu.dashboard.layout.organization.tier.price.TierPriceListView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Route;

@Route("side-nav-labelled")
public class SideNavMenu extends Div {
    public SideNavMenu() {
        this.addClassName("side-nav-sample");

        SideNav sideNavAdmin = new SideNav();
        sideNavAdmin.setLabel("Admin");
        sideNavAdmin.setCollapsible(true);
        sideNavAdmin.addItem(new SideNavItem("Brand", BrandListView.class, VaadinIcon.SHOP.create()));
        sideNavAdmin.addItem(new SideNavItem("Chain", ChainListView.class, VaadinIcon.BUILDING_O.create()));

        SideNavItem sideNavTier = new SideNavItem("Tier");
        sideNavTier.setPrefixComponent(VaadinIcon.GRID_BEVEL.create());
        sideNavTier.addItem(new SideNavItem("Price", TierPriceListView.class, VaadinIcon.DOLLAR.create()));
        sideNavTier.addItem(new SideNavItem("Service", TierServiceListView.class, VaadinIcon.COG_O.create()));
        sideNavTier.addItem(new SideNavItem("Menu", TierMenuListView.class, VaadinIcon.LIST.create()));

        sideNavAdmin.addItem(sideNavTier);
        sideNavAdmin.addItem(new SideNavItem("Store", StoreLayout.class, VaadinIcon.STORAGE.create()));
        sideNavAdmin.addItem(new SideNavItem("User", UserLayout.class, VaadinIcon.USER.create()));

        SideNav sideNavMenu = new SideNav();
        sideNavMenu.setLabel("Menu");
        sideNavMenu.setCollapsible(true);
        sideNavMenu.addItem(new SideNavItem("Category", CategoryListView.class, VaadinIcon.TAGS.create()));
        sideNavMenu.addItem(new SideNavItem("Customization", CustomizationLayout.class, VaadinIcon.SLIDERS.create()));
        sideNavMenu.addItem(new SideNavItem("Product", ProductLayout.class, VaadinIcon.COFFEE.create()));

        SideNav sideNavSetting = new SideNav();
        sideNavSetting.setLabel("Setting");
        sideNavSetting.setCollapsible(true);
        sideNavSetting.addItem(new SideNavItem("Service", ServiceListView.class, VaadinIcon.COG.create()));

        VerticalLayout navWrapper = new VerticalLayout(sideNavAdmin, sideNavMenu, sideNavSetting);
        navWrapper.setSpacing(true);
        navWrapper.setSizeUndefined();
        sideNavMenu.setWidthFull();
        sideNavAdmin.setWidthFull();
        sideNavSetting.setWidthFull();

        Scroller scroller = new Scroller(new Div(navWrapper));
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        scroller.getStyle()
                .set("border-bottom", "1px solid var(--lumo-contrast-20pct)")
                .set("padding", "var(--lumo-space-m)");
        add(scroller);
    }
}