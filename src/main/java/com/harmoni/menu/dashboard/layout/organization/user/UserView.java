package com.harmoni.menu.dashboard.layout.organization.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.dto.OrganizationTreeDto;
import com.harmoni.menu.dashboard.dto.RoleDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.ScopeService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientRbacService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unified Users page: a searchable list of the chain's users on the left and
 * the selected user's profile on the right, with create/edit in a modal dialog.
 */
public class UserView extends VerticalLayout {

    private static final int PAGE_SIZE = 10;

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final RestClientRbacService restClientRbacService;
    private final AuthorizationService authorizationService;

    private final UserListCard listCard = new UserListCard();
    private final UserDetailCard detailCard = new UserDetailCard();
    private final TextField search = new TextField();
    private final Button addButton = new Button(Messages.get(Messages.Keys.ACTION_NEW_USER),
            event -> openDialog(null));

    private final List<StoreDto> stores = new ArrayList<>();
    private final Map<Integer, String> storeNames = new HashMap<>();
    private final List<RoleDto> roles = new ArrayList<>();
    private final Map<Long, List<String>> roleNamesByUser = new HashMap<>();

    private List<UserDto> pageUsers = new ArrayList<>();
    private UserDto selectedUser;
    private Integer pendingSelectId;
    private int currentPage = 1;
    private int totalPages;
    private int totalUsers;
    private boolean loaded;
    private boolean dataLoaded;

    public UserView(AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                    RestClientOrganizationService restClientOrganizationService,
                    RestClientRbacService restClientRbacService,
                    AuthorizationService authorizationService) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;
        this.restClientRbacService = restClientRbacService;
        this.authorizationService = authorizationService;

        addClassName("user-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        PageHeader header = PageHeader.of("page.user.title", "page.user.description");

        search.setLabel(Messages.get(Messages.Keys.LABEL_SEARCH));
        search.setPlaceholder(Messages.get("user.search.placeholder"));
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setWidthFull();
        search.addValueChangeListener(event -> {
            if (!Objects.equals(event.getOldValue(), event.getValue())) {
                currentPage = 1;
                fetchUsers();
            }
        });

        addButton.setIcon(VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.setEnabled(false);

        HorizontalLayout toolbar = new HorizontalLayout(search, addButton);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.addClassName("user-toolbar");
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        HorizontalLayout body = new HorizontalLayout(listCard, detailCard);
        body.addClassName("user-body");
        body.setWidthFull();
        body.setSpacing(true);

        add(header, toolbar, body);
        setFlexGrow(1, body);

        listCard.setSelectionListener(this::selectUser);
        listCard.setPreviousListener(() -> {
            if (currentPage > 1) {
                currentPage--;
                fetchUsers();
            }
        });
        listCard.setNextListener(() -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchUsers();
            }
        });

        detailCard.setEditListener(this::openDialog);
        detailCard.setDeleteListener(this::confirmDelete);
        detailCard.setCanEdit(authorizationService.hasPermission(Permissions.USER_WRITE));
        detailCard.setCanDelete(authorizationService.hasPermission(Permissions.USER_DELETE));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!loaded) {
            loaded = true;
            Shortcuts.addShortcutListener(this, search::focus, Key.SLASH);
        }
        // The organisation tree and the role list are stable for the session;
        // reloading them on every attach made the page feel slow for no gain.
        if (!dataLoaded) {
            dataLoaded = true;
            loadStores();
            loadRoles();
        }
    }

    private void loadRoles() {
        if (!authorizationService.hasPermission(Permissions.RBAC_READ)) {
            return;
        }
        restClientRbacService.getRoles().subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    roles.clear();
                    if (response != null && response.getData() != null) {
                        roles.addAll(ObjectUtil.convertObjectToObject(
                                response.getData(), new TypeReference<List<RoleDto>>() { }));
                    }
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("rbac.role.loadFailed"))));
    }

    private void loadStores() {
        asyncRestClientOrganizationService.getOrganizationTreeAsync(
                tree -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    List<StoreDto> loaded = new ArrayList<>();
                    if (tree != null && tree.getBrands() != null) {
                        for (OrganizationTreeDto.BrandNode brand : tree.getBrands()) {
                            if (brand.getChains() == null) {
                                continue;
                            }
                            for (OrganizationTreeDto.ChainNode chain : brand.getChains()) {
                                if (chain.getStores() == null) {
                                    continue;
                                }
                                for (OrganizationTreeDto.StoreNode store : chain.getStores()) {
                                    StoreDto dto = new StoreDto();
                                    dto.setId(store.getId());
                                    dto.setName(store.getName());
                                    dto.setChainId(store.getChainId());
                                    loaded.add(dto);
                                }
                            }
                        }
                    }
                    ScopeService.ScopeSnapshot scope = scope();
                    List<StoreDto> allowed = scope == null ? loaded : scope.filterStores(loaded);
                    stores.clear();
                    storeNames.clear();
                    for (StoreDto store : allowed) {
                        stores.add(store);
                        storeNames.put(store.getId(), store.getName());
                    }
                    listCard.setUsers(pageUsers, storeNames);
                    if (selectedUser != null) {
                        detailCard.show(selectedUser, storeName(selectedUser));
                    }
                    addButton.setEnabled(authorizationService.hasPermission(Permissions.USER_WRITE));
                    fetchUsers();
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    dataLoaded = false;
                    addButton.setEnabled(authorizationService.hasPermission(Permissions.USER_WRITE));
                    fetchUsers();
                }));
    }

    private void fetchUsers() {
        List<Integer> storeIds = stores.stream()
                .map(StoreDto::getId)
                .filter(Objects::nonNull)
                .toList();
        // An empty selection is intentional: the menu service expands the
        // caller's own scope (chain/brand/store), so a chain operator still
        // sees every store of the chain even when the organisation tree could
        // not be loaded. Falling back to the login store here would hide the
        // rest of the chain.
        String storeIdsParam = storeIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        listCard.setLoading(true);
        asyncRestClientOrganizationService.getUsersPageAsync(
                result -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    listCard.setLoading(false);
                    pageUsers = new ArrayList<>();
                    totalPages = 0;
                    totalUsers = 0;
                    if (result != null) {
                        totalPages = parseInt(result.get("page"));
                        totalUsers = parseInt(result.get("total"));
                        Object data = result.get("data");
                        if (data instanceof List<?> rows) {
                            for (Object row : rows) {
                                UserDto user = ObjectUtil.convertValueToObject(row, UserDto.class);
                                if (user != null) {
                                    pageUsers.add(user);
                                }
                            }
                        }
                    }
                    listCard.setUsers(pageUsers, storeNames);
                    listCard.setPageInfo(currentPage, totalPages, totalUsers);

                    UserDto target = findSelected();
                    if (target == null && !pageUsers.isEmpty()) {
                        target = pageUsers.getFirst();
                    }
                    selectUser(target);
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    listCard.setLoading(false);
                    UiUtil.errorWithRetry(Messages.get("notification.user.loadFailed"), this::fetchUsers);
                }),
                storeIdsParam, currentPage, PAGE_SIZE, search.getValue(), null, null);
    }

    private UserDto findSelected() {
        Integer id = pendingSelectId != null ? pendingSelectId
                : selectedUser == null ? null : selectedUser.getId();
        if (id == null) {
            return null;
        }
        UserDto match = pageUsers.stream()
                .filter(user -> Objects.equals(user.getId(), id))
                .findFirst()
                .orElse(null);
        if (match != null) {
            pendingSelectId = null;
        }
        return match;
    }

    private void selectUser(UserDto user) {
        selectedUser = user;
        listCard.select(user);
        if (user == null) {
            detailCard.show(null, null, List.of());
            return;
        }
        List<String> cached = user.getAuthUserId() == null ? null : roleNamesByUser.get(user.getAuthUserId());
        if (cached != null) {
            detailCard.show(user, storeName(user), cached);
            return;
        }
        detailCard.show(user, storeName(user), List.of());
        if (user.getAuthUserId() != null) {
            restClientRbacService.getUserRoles(user.getAuthUserId()).subscribe(
                    response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                        List<String> names = parseRoles(response).stream()
                                .map(RoleDto::getName)
                                .toList();
                        roleNamesByUser.put(user.getAuthUserId(), names);
                        if (selectedUser == user) {
                            detailCard.show(user, storeName(user), names);
                        }
                    }),
                    error -> { });
        }
    }

    private List<RoleDto> parseRoles(RestAPIResponse response) {
        if (response == null || response.getData() == null) {
            return List.of();
        }
        return ObjectUtil.convertObjectToObject(response.getData(), new TypeReference<List<RoleDto>>() { });
    }

    private String storeName(UserDto user) {
        return user == null || user.getStoreId() == null ? null : storeNames.get(user.getStoreId());
    }

    private void openDialog(UserDto user) {
        if (!authorizationService.hasPermission(Permissions.USER_WRITE)) {
            return;
        }
        if (user == null || user.getAuthUserId() == null) {
            showDialog(user);
            return;
        }
        restClientRbacService.getUserRoles(user.getAuthUserId()).subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    Set<Long> roleIds = parseRoles(response).stream()
                            .map(RoleDto::getId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                    user.setRoleIds(roleIds);
                    showDialog(user);
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null), () -> showDialog(user)));
    }

    private void showDialog(UserDto user) {
        new UserDialog(restClientOrganizationService, stores, roles, user, () -> {
            pendingSelectId = user == null ? null : user.getId();
            if (user == null) {
                currentPage = 1;
            }
            roleNamesByUser.clear();
            fetchUsers();
        }).open();
    }

    private void confirmDelete(UserDto user) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(Messages.get(Messages.Keys.DIALOG_CONFIRM_TITLE));
        dialog.setText(Messages.get("dialog.confirmDeleteUser",
                user.getUsername() == null ? "" : user.getUsername()));
        dialog.setCancelable(true);
        dialog.setConfirmText(Messages.get(Messages.Keys.ACTION_DELETE));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> restClientOrganizationService.deleteUser(user).subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    UiUtil.success(Messages.get("user.deleted"));
                    selectedUser = null;
                    fetchUsers();
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("user.deleteFailed")))));
        dialog.open();
    }

    private int parseInt(Object value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.toString());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static ScopeService.ScopeSnapshot scope() {
        ScopeService service = ScopeService.current();
        return service == null ? null : service.snapshot();
    }
}
