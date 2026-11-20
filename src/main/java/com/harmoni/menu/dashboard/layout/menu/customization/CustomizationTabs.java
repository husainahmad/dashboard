package com.harmoni.menu.dashboard.layout.menu.customization;

import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.AbstractListViewTabs;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.icon.VaadinIcon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet host for the customization page.
 *
 * <p>
 * Adds a single "All Customizations" tab holding a {@link CustomizationListView}
 * and places the list's toolbar above the sheet.
 * </p>
 */
@RequiredArgsConstructor
@Slf4j
public class CustomizationTabs extends AbstractListViewTabs {

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;
    private final AuthorizationService authorizationService;

    @Override
    protected String tabLabelKey() {
        return "tab.customizationList";
    }

    @Override
    protected String pageTitleKey() {
        return Messages.Keys.PAGE_CUSTOMIZATION_TITLE;
    }

    @Override
    protected String pageDescriptionKey() {
        return Messages.Keys.PAGE_CUSTOMIZATION_DESCRIPTION;
    }

    @Override
    protected VaadinIcon tabIcon() {
        return VaadinIcon.COG;
    }

    @Override
    protected AbstractListView createListView() {
        return new CustomizationListView(asyncRestClientMenuService, restClientMenuService, accessService, authorizationService);
    }
}
