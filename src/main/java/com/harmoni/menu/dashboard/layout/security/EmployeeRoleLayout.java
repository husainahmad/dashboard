package com.harmoni.menu.dashboard.layout.security;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientRbacService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;

/**
 * Route ({@code /employees}) for the unified "Karyawan & Peran" page.
 */
@RequiredArgsConstructor
@Route(value = "employees", layout = MainLayout.class)
@RequiresPermission(Permissions.EMPLOYEE_VIEW)
public class EmployeeRoleLayout extends VerticalLayout implements HasDynamicTitle {

    private final RestClientRbacService restClientRbacService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AccessService accessService;
    private final AuthorizationService authorizationService;

    private boolean built;

    @Override
    public String getPageTitle() {
        return Messages.get("page.employee.title") + " | POSHarmoni";
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

        EmployeeListView employeeListView = new EmployeeListView(restClientRbacService,
                restClientOrganizationService, asyncRestClientOrganizationService,
                accessService, authorizationService);
        RoleWorkspaceView roleWorkspaceView = new RoleWorkspaceView(restClientRbacService, authorizationService);

        Tab employeesTab = new Tab(Messages.get("employee.tab.employees"));
        Tab rolesTab = new Tab(Messages.get("employee.tab.roles"));

        TabSheet tabSheet = new TabSheet();
        tabSheet.add(employeesTab, employeeListView);
        tabSheet.add(rolesTab, roleWorkspaceView);
        tabSheet.setSizeFull();

        Button action = new Button();
        action.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        action.setIcon(VaadinIcon.PLUS.create());

        Runnable refreshAction = () -> {
            boolean employees = tabSheet.getSelectedTab() == employeesTab;
            action.setText(Messages.get(employees ? "employee.add" : "role.add"));
            action.setVisible(employees
                    ? authorizationService.hasPermission(Permissions.EMPLOYEE_EDIT)
                    : authorizationService.hasAnyPermission(Permissions.RBAC_MANAGE, Permissions.RBAC_WRITE));
        };
        action.addClickListener(event -> {
            if (tabSheet.getSelectedTab() == employeesTab) {
                employeeListView.openCreate();
            } else {
                roleWorkspaceView.openCreate();
            }
        });
        tabSheet.addSelectedChangeListener(event -> refreshAction.run());
        refreshAction.run();

        add(PageHeader.of("page.employee.title", "page.employee.description", action), tabSheet);
        setFlexGrow(1, tabSheet);
    }
}
