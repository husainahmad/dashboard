package com.harmoni.menu.dashboard.layout.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.dto.AssignmentDto;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.EmployeeDto;
import com.harmoni.menu.dashboard.dto.OrganizationTreeDto;
import com.harmoni.menu.dashboard.dto.RoleDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.layout.component.PaginationBar;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.ScopeService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientRbacService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Employee table for the Karyawan & Peran page.
 */
public class EmployeeListView extends VerticalLayout {

    private final RestClientRbacService restClientRbacService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AccessService accessService;
    private final AuthorizationService authorizationService;
    private final boolean canEdit;

    private final Grid<EmployeeDto> grid = new Grid<>(EmployeeDto.class, false);
    private final TextField search = new TextField();
    private final ComboBox<RoleDto> roleFilter = new ComboBox<>();
    private final ComboBox<String> scopeFilter = new ComboBox<>();

    private final List<RoleDto> roles = new ArrayList<>();
    private final List<BrandDto> brands = new ArrayList<>();
    private final List<ChainDto> chains = new ArrayList<>();
    private final List<StoreDto> stores = new ArrayList<>();
    private final Map<Integer, String> brandNames = new HashMap<>();
    private final Map<Integer, String> chainNames = new HashMap<>();
    private final Map<Integer, String> storeNames = new HashMap<>();

    private final PaginationBar paginationBar = new PaginationBar(this::previousPage, this::nextPage);
    private int currentPage = 1;
    private int totalPages;
    private static final int PAGE_SIZE = 10;

    private boolean loaded;

    public EmployeeListView(RestClientRbacService restClientRbacService,
                            RestClientOrganizationService restClientOrganizationService,
                            AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                            AccessService accessService,
                            AuthorizationService authorizationService) {
        this.restClientRbacService = restClientRbacService;
        this.restClientOrganizationService = restClientOrganizationService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.accessService = accessService;
        this.authorizationService = authorizationService;
        this.canEdit = authorizationService.hasPermission(Permissions.EMPLOYEE_EDIT);

        setSizeFull();
        setPadding(false);
        setSpacing(true);

        configureFilters();
        configureGrid();
        add(toolbar(), grid, paginationBar);
        setFlexGrow(1, grid);
    }

    /**
     * Opens the create-employee dialog, used by the page header action.
     */
    public void openCreate() {
        openDialog(null);
    }

    private void configureFilters() {
        search.setLabel(Messages.get(Messages.Keys.LABEL_SEARCH));
        search.setPlaceholder(Messages.get("employee.search.placeholder"));
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setWidth("260px");
        search.addValueChangeListener(event -> reloadFirstPage());

        roleFilter.setPlaceholder(Messages.get("employee.filter.roleAll"));
        roleFilter.setItemLabelGenerator(RoleDto::getName);
        roleFilter.setClearButtonVisible(true);
        roleFilter.setWidth("180px");
        roleFilter.addValueChangeListener(event -> reloadFirstPage());

        scopeFilter.setPlaceholder(Messages.get("employee.filter.scopeAll"));
        scopeFilter.setItems(ScopeLabels.BRAND, ScopeLabels.CHAIN, ScopeLabels.STORE);
        scopeFilter.setItemLabelGenerator(ScopeLabels::label);
        scopeFilter.setClearButtonVisible(true);
        scopeFilter.setWidth("160px");
        scopeFilter.addValueChangeListener(event -> reloadFirstPage());
    }

    private HorizontalLayout toolbar() {
        HorizontalLayout toolbar = new HorizontalLayout(search, roleFilter, scopeFilter);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);
        return toolbar;
    }

    private void configureGrid() {
        grid.addComponentColumn(this::employeeCell).setHeader(Messages.get("employee.column.employee")).setAutoWidth(true);
        grid.addColumn(this::roleLabel).setHeader(Messages.get("employee.column.role")).setAutoWidth(true);
        grid.addComponentColumn(this::scopeCell).setHeader(Messages.get("employee.column.scope")).setAutoWidth(true);
        grid.addComponentColumn(this::statusCell).setHeader(Messages.get("employee.column.status")).setAutoWidth(true);
        grid.addComponentColumn(this::actionCell).setHeader("").setAutoWidth(true);
        grid.setSizeFull();
    }

    private Div employeeCell(EmployeeDto employee) {
        Span name = new Span(employee.getUsername() == null ? "-" : employee.getUsername());
        name.addClassName("employee-name");
        Span code = new Span(employee.getEmployeeCode() == null ? "-" : employee.getEmployeeCode());
        code.addClassName("employee-code");
        Div cell = new Div(name, code);
        cell.addClassName("employee-cell");
        return cell;
    }

    private String roleLabel(EmployeeDto employee) {
        return employee.getAssignments().stream()
                .map(AssignmentDto::getRoleName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("-");
    }

    private Div scopeCell(EmployeeDto employee) {
        Div cell = new Div();
        cell.addClassName("employee-scope-cell");
        if (employee.getAssignments().isEmpty()) {
            cell.add(new Span("-"));
            return cell;
        }
        for (AssignmentDto assignment : employee.getAssignments()) {
            Span badge = new Span(ScopeLabels.label(assignment.getScopeType()));
            badge.addClassName("scope-badge");
            badge.addClassName(ScopeLabels.cssClass(assignment.getScopeType()));
            Span detail = new Span(scopeDetail(assignment));
            detail.addClassName("employee-scope-detail");
            HorizontalLayout row = new HorizontalLayout(badge, detail);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setSpacing(true);
            cell.add(row);
        }
        return cell;
    }

    private String scopeDetail(AssignmentDto assignment) {
        if (assignment.getScopeId() == null) {
            return "-";
        }
        int id = assignment.getScopeId().intValue();
        return switch (assignment.getScopeType() == null ? "" : assignment.getScopeType()) {
            case ScopeLabels.BRAND -> brandNames.getOrDefault(id, String.valueOf(id));
            case ScopeLabels.CHAIN -> chainNames.getOrDefault(id, String.valueOf(id));
            case ScopeLabels.STORE -> storeNames.getOrDefault(id, String.valueOf(id));
            default -> String.valueOf(id);
        };
    }

    private Div statusCell(EmployeeDto employee) {
        boolean active = !"INACTIVE".equalsIgnoreCase(employee.getStatus());
        Span dot = new Span();
        dot.addClassName("status-dot");
        dot.addClassName(active ? "status-dot-active" : "status-dot-inactive");
        Span label = new Span(Messages.get(active ? "employee.status.active" : "employee.status.inactive"));
        HorizontalLayout row = new HorizontalLayout(dot, label);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        Div cell = new Div(row);
        return cell;
    }

    private HorizontalLayout actionCell(EmployeeDto employee) {
        Button edit = new Button(Messages.get("employee.edit"), event -> openDialog(employee));
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        edit.setVisible(canEdit);
        HorizontalLayout actions = new HorizontalLayout(edit);
        actions.setSpacing(false);
        return actions;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!loaded) {
            loaded = true;
            loadOrganization();
            loadRoles();
        }
    }

    private void loadOrganization() {
        asyncRestClientOrganizationService.getOrganizationTreeAsync(
                tree -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    brands.clear();
                    chains.clear();
                    stores.clear();
                    brandNames.clear();
                    chainNames.clear();
                    storeNames.clear();
                    if (tree != null) {
                        for (OrganizationTreeDto.BrandNode brand : tree.getBrands()) {
                            BrandDto brandDto = new BrandDto();
                            brandDto.setId(brand.getId());
                            brandDto.setName(brand.getName());
                            brands.add(brandDto);
                            brandNames.put(brand.getId(), brand.getName());
                            for (OrganizationTreeDto.ChainNode chain : brand.getChains()) {
                                ChainDto chainDto = new ChainDto();
                                chainDto.setId(chain.getId());
                                chainDto.setName(chain.getName());
                                chainDto.setBrandId(chain.getBrandId());
                                chains.add(chainDto);
                                chainNames.put(chain.getId(), chain.getName());
                                for (OrganizationTreeDto.StoreNode store : chain.getStores()) {
                                    StoreDto storeDto = new StoreDto();
                                    storeDto.setId(store.getId());
                                    storeDto.setName(store.getName());
                                    storeDto.setChainId(store.getChainId());
                                    stores.add(storeDto);
                                    storeNames.put(store.getId(), store.getName());
                                }
                            }
                        }
                    }
                    applyScopeFilter();
                    loadEmployees();
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("employee.loadFailed"))));
    }

    private void applyScopeFilter() {
        ScopeService.ScopeSnapshot scope = scope();
        if (scope == null || scope.isGlobal()) {
            return;
        }
        brands.removeIf(brand -> !scope.canAccessBrand(brand.getId()));
        chains.removeIf(chain -> !scope.canAccessChain(chain.getId(), chain.getBrandId()));
        stores.removeIf(store -> !scope.canAccessStore(store.getId(), store.getChainId()));
    }

    private static ScopeService.ScopeSnapshot scope() {
        ScopeService service = ScopeService.current();
        return service == null ? null : service.snapshot();
    }

    private void loadRoles() {
        restClientRbacService.getRoles().subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    roles.clear();
                    if (response != null && response.getData() != null) {
                        roles.addAll(ObjectUtil.convertObjectToObject(
                                response.getData(), new TypeReference<List<RoleDto>>() { }));
                    }
                    boolean systemAdmin = authorizationService.hasRoleCode("SYS");
                    roles.removeIf(role -> "SYS".equalsIgnoreCase(role.getCode())
                            || (!systemAdmin && "OWN".equalsIgnoreCase(role.getCode())));
                    roleFilter.setItems(roles);
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("rbac.role.loadFailed"))));
    }

    private void loadEmployees() {
        List<Integer> storeIds = resolveStoreIds();
        if (storeIds.isEmpty()) {
            totalPages = 0;
            grid.setItems(List.of());
            paginationBar.update(currentPage, 1);
            return;
        }
        String storeIdsParam = storeIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        RoleDto role = roleFilter.getValue();
        String scope = scopeFilter.getValue();
        String searchTerm = search.getValue() == null ? "" : search.getValue().trim();
        asyncRestClientOrganizationService.getUsersPageAsync(
                result -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    List<UserDto> users = new ArrayList<>();
                    Object data = result == null ? null : result.get("data");
                    if (data instanceof List<?> rows) {
                        for (Object row : rows) {
                            UserDto user = ObjectUtil.convertValueToObject(row, UserDto.class);
                            if (user != null) {
                                users.add(user);
                            }
                        }
                    }
                    totalPages = intValue(result == null ? null : result.get("page"));
                    loadAssignments(users);
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("employee.loadFailed"))),
                storeIdsParam, currentPage, PAGE_SIZE, searchTerm,
                role == null ? null : String.valueOf(role.getId()), scope);
    }

    private List<Integer> resolveStoreIds() {
        Set<Integer> storeIds = stores.stream()
                .map(StoreDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (storeIds.isEmpty()) {
            Integer storeId = accessService.getStoreId();
            if (storeId != null) {
                storeIds.add(storeId);
            }
        }
        return new ArrayList<>(storeIds);
    }

    private int intValue(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void loadAssignments(List<UserDto> users) {
        List<Long> userIds = users.stream()
                .map(UserDto::getAuthUserId)
                .filter(Objects::nonNull)
                .toList();
        if (userIds.isEmpty()) {
            merge(users, Map.of());
            return;
        }
        restClientRbacService.batchAssignments(userIds).subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    Map<Long, List<AssignmentDto>> assignments = new HashMap<>();
                    if (response != null && response.getData() != null) {
                        assignments.putAll(ObjectUtil.convertObjectToObject(
                                response.getData(),
                                new TypeReference<Map<Long, List<AssignmentDto>>>() { }));
                    }
                    merge(users, assignments);
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null), () -> merge(users, Map.of())));
    }

    private void merge(List<UserDto> users, Map<Long, List<AssignmentDto>> assignments) {
        List<EmployeeDto> pageItems = new ArrayList<>();
        for (UserDto user : users) {
            pageItems.add(EmployeeDto.builder()
                    .id(user.getId())
                    .authUserId(user.getAuthUserId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .employeeCode(user.getEmployeeCode())
                    .status(user.getStatus())
                    .storeId(user.getStoreId())
                    .assignments(assignments.getOrDefault(user.getAuthUserId(), List.of()))
                    .build());
        }
        grid.setItems(pageItems);
        paginationBar.update(currentPage, Math.max(totalPages, 1));
    }

    private void reloadFirstPage() {
        currentPage = 1;
        loadEmployees();
    }

    private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            loadEmployees();
        }
    }

    private void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadEmployees();
        }
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }

    private void openDialog(EmployeeDto employee) {
        if (!canEdit) {
            return;
        }
        new EmployeeDialog(restClientOrganizationService, assignableRoles(), brands, chains, stores,
                employee, this::loadEmployees).open();
    }

    private List<RoleDto> assignableRoles() {
        Set<String> granted = authorizationService.getPermissions();
        return roles.stream()
                .filter(role -> role.getPermissions() == null || role.getPermissions().stream()
                        .allMatch(permission -> granted.contains(permission.getName())))
                .toList();
    }
}
