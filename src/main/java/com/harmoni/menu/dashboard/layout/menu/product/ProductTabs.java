package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.layout.component.FocusModeToggle;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Hosts the product management tab sheet.
 *
 * <p>Renders an "All Products" tab backed by {@link ProductListView}, sharing
 * the list toolbar and the injected menu/state services.</p>
 *
 * <p>Also the page shell the {@link FocusModeToggle} collapses: the header and the
 * toolbar are siblings of the tab sheet, not children of it, so hiding them hands their
 * height to the one flexible child and the product editor grows without anything being
 * re-parented. The control is a sibling of both for the same reason it is not a child of
 * them: an element that is hidden cannot undo its own hiding.</p>
 */
@RequiredArgsConstructor
@Slf4j
@Route("product-tabs")
public class ProductTabs extends VerticalLayout {

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    /**
     * Hides this shell's header and toolbar, and brings them back.
     *
     * <p>Not final, so {@code @RequiredArgsConstructor} leaves it out: it cannot be
     * built before the shell it collapses, and the caller's three services are all the
     * constructor has ever needed.</p>
     */
    @Getter
    private transient FocusModeToggle focusModeToggle;

    private void renderTabSheet() {
        TabSheet tabSheet = new TabSheet();
        Tab browseTab = new Tab();
        browseTab.setLabel(Messages.get("tab.productList"));
        ProductListView productListView = new ProductListView(asyncRestClientMenuService, restClientMenuService,
                accessService, browseTab);
        tabSheet.add(browseTab, productListView);
        tabSheet.setSizeFull();

        add(PageHeader.of(Messages.Keys.PAGE_PRODUCT_TITLE, Messages.Keys.PAGE_PRODUCT_DESCRIPTION));
        add(productListView.getToolbarComponent());
        add(tabSheet);
        setFlexGrow(1, tabSheet);
        setPadding(false);
        setSizeFull();

        // Out of flow, so it takes no room in either state, and added last so the row
        // above never has to make space for it. The shell is the positioned ancestor
        // the stylesheet pins it against, which is what keeps it in the top right of
        // the page: over the header's empty half normally, and over the tab strip's
        // empty end once the header is gone.
        focusModeToggle = new FocusModeToggle(this);
        add(focusModeToggle);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName(Css.PAGE);
        renderTabSheet();
    }
}
