package com.harmoni.menu.dashboard.layout.organization.user;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientRbacService;
import com.harmoni.menu.dashboard.layout.security.EmployeeRoleLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;

/**
 * Route ({@code /users}) for user administration. Hosts the unified
 * {@link UserView} inside the {@link MainLayout} shell.
 */
@RequiredArgsConstructor
@Route(value = "users", layout = MainLayout.class)
@PageTitle("Users | POSHarmoni")
public class UserLayout extends VerticalLayout implements BeforeEnterObserver {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final RestClientRbacService restClientRbacService;
    private final AuthorizationService authorizationService;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.forwardTo(EmployeeRoleLayout.class);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName(Css.LIST_VIEW);
        add(new UserView(asyncRestClientOrganizationService, restClientOrganizationService,
                restClientRbacService, authorizationService));
        setSizeFull();
    }
}
