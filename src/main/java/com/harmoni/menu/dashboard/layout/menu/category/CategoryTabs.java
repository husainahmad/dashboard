package com.harmoni.menu.dashboard.layout.menu.category;

import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.AbstractListViewTabs;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet host for the category page.
 *
 * <p>
 * Adds a single "All Categories" tab holding a {@link CategoryListView} and
 * places the list's toolbar above the sheet.
 * </p>
 */
@RequiredArgsConstructor
@Slf4j
public class CategoryTabs extends AbstractListViewTabs {

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    @Override
    protected String tabLabelKey() {
        return "tab.categoryList";
    }

    @Override
    protected String pageTitleKey() {
        return Messages.Keys.PAGE_CATEGORY_TITLE;
    }

    @Override
    protected String pageDescriptionKey() {
        return Messages.Keys.PAGE_CATEGORY_DESCRIPTION;
    }

    @Override
    protected AbstractListView createListView() {
        return new CategoryListView(asyncRestClientMenuService, asyncRestClientOrganizationService,
                restClientMenuService, accessService);
    }
}
