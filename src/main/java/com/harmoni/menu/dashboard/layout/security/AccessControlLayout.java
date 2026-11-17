package com.harmoni.menu.dashboard.layout.security;

import com.harmoni.menu.dashboard.configuration.RbacProperties;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientRbacService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;

/**
 * Route ({@code /access-control}) for role and permission administration.
 */
@RequiredArgsConstructor
@Route(value = "access-control", layout = MainLayout.class)
@PageTitle("Access Control | POSHarmoni")
public class AccessControlLayout extends VerticalLayout implements BeforeEnterObserver {

    private final RestClientRbacService restClientRbacService;
    private final AuthorizationService authorizationService;
    private final RbacProperties rbacProperties;

    private boolean built;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.forwardTo(EmployeeRoleLayout.class);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (built) {
            return;
        }
        built = true;
        addClassName(Css.LIST_VIEW);
        setSizeFull();
        setPadding(false);

        if (!rbacProperties.isEnabled()) {
            add(PageHeader.of("page.accessControl.title", "page.accessControl.description"),
                    new Paragraph(Messages.get("rbac.disabled")));
            return;
        }

        TabSheet tabSheet = new TabSheet();
        tabSheet.add(new Tab(Messages.get("rbac.tab.roles")),
                new RoleListView(restClientRbacService, authorizationService));
        tabSheet.add(new Tab(Messages.get("rbac.tab.permissions")),
                new PermissionListView(restClientRbacService, authorizationService));
        tabSheet.setSizeFull();

        add(PageHeader.of("page.accessControl.title", "page.accessControl.description"), tabSheet);
        setFlexGrow(1, tabSheet);
    }
}
