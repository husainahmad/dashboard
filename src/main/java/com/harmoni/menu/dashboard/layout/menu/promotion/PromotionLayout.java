package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientPromotionService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientPromotionService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;

/**
 * Main route for the promotion administration page.
 */
@AllArgsConstructor
@Route(value = "promotion", layout = MainLayout.class)
@PageTitle("Promotion | POSHarmoni")
public class PromotionLayout extends VerticalLayout {

    private final AsyncRestClientPromotionService asyncRestClientPromotionService;
    private final RestClientPromotionService restClientPromotionService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName(Css.LIST_VIEW);
        add(new PromotionTabs(asyncRestClientPromotionService, restClientPromotionService));
        setSizeFull();
    }
}
