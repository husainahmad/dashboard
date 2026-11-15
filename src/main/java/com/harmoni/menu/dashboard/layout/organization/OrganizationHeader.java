package com.harmoni.menu.dashboard.layout.organization;

import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Page heading of the organization page, using the same {@link PageHeader} as
 * the other list pages.
 */
public class OrganizationHeader extends VerticalLayout {

    public OrganizationHeader() {
        addClassName("org-header");
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        add(new PageHeader(Messages.get("organization.title"),
                Messages.get("organization.description")));
    }
}
