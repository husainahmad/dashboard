package com.harmoni.menu.dashboard.layout.navigation;

import com.harmoni.menu.dashboard.layout.menu.product.ProductListView;
import com.harmoni.menu.dashboard.layout.menu.sku.SkuListView;
import com.harmoni.menu.dashboard.layout.organization.brand.BrandListView;
import com.harmoni.menu.dashboard.layout.menu.category.CategoryListView;
import com.harmoni.menu.dashboard.layout.organization.chain.ChainListView;
import com.harmoni.menu.dashboard.layout.organization.store.StoreListView;
import com.harmoni.menu.dashboard.layout.organization.tier.TierListView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Route;

@Route("side-nav-labelled")
public class SideNavMenu extends Div {
    public SideNavMenu() {
        SideNav sideNavMenu = new SideNav();
        sideNavMenu.setLabel("Menu");
        sideNavMenu.setCollapsible(true);
        sideNavMenu.addItem(new SideNavItem("Category", CategoryListView.class));
        sideNavMenu.addItem(new SideNavItem("Product", ProductListView.class));

        SideNav sideNavAdmin = new SideNav();
        sideNavAdmin.setLabel("Admin");
        sideNavAdmin.setCollapsible(true);
        sideNavAdmin.addItem(new SideNavItem("Chain", ChainListView.class));
        sideNavAdmin.addItem(new SideNavItem("Brand", BrandListView.class));
        sideNavAdmin.addItem(new SideNavItem("Tier", TierListView.class));
        sideNavAdmin.addItem(new SideNavItem("Store", StoreListView.class));

        VerticalLayout navWrapper = new VerticalLayout(sideNavMenu, sideNavAdmin);
        navWrapper.setSpacing(true);
        navWrapper.setSizeUndefined();
        sideNavMenu.setWidthFull();
        sideNavAdmin.setWidthFull();

        Scroller scroller = new Scroller(new Div(navWrapper));
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        scroller.getStyle()
                .set("border-bottom", "1px solid var(--lumo-contrast-20pct)")
                .set("padding", "var(--lumo-space-m)");
        add(scroller);

        this.addClassName("side-nav-sample");
    }
}
