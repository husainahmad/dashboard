package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.security.RequiresPermission;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.layout.component.FocusModeToggle;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Route ({@code /product}) for product management. Hosts the product browser in
 * a single tab sheet, with the page header, toolbar and the focus-mode toggle
 * as siblings of the sheet so the editor can take the full height.
 */
@RequiredArgsConstructor
@RequiresPermission(Permissions.MENU_VIEW)
@Route(value = "product", layout = MainLayout.class)
@PageTitle("Products | POSHarmoni")
public class ProductLayout extends VerticalLayout {

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final AuthorizationService authorizationService;

    @Getter
    private transient FocusModeToggle focusModeToggle;

    private boolean built;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (built) {
            return;
        }
        built = true;
        renderPage();
    }

    void renderPage() {
        addClassNames(Css.LIST_VIEW, Css.PAGE);

        ProductListView productListView = new ProductListView(asyncRestClientMenuService,
                restClientMenuService, authorizationService);
        TabSheet tabSheet = new TabSheet();
        Icon listIcon = new Icon(VaadinIcon.PACKAGE);
        listIcon.setSize("16px");
        listIcon.addClassName("tab-icon");
        tabSheet.add(new Tab(listIcon, new Span(Messages.get("tab.productList"))), productListView);
        tabSheet.setSizeFull();

        add(PageHeader.of(Messages.Keys.PAGE_PRODUCT_TITLE, Messages.Keys.PAGE_PRODUCT_DESCRIPTION));
        add(productListView.getToolbarComponent());
        add(tabSheet);
        setFlexGrow(1, tabSheet);
        setPadding(false);
        setSizeFull();

        focusModeToggle = new FocusModeToggle(this);
        add(focusModeToggle);
    }
}
