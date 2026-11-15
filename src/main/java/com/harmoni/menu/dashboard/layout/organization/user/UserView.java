package com.harmoni.menu.dashboard.layout.organization.user;

import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
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

/**
 * Unified Users page: a searchable list of the chain's users on the left and
 * the selected user's profile on the right, with create/edit in a modal dialog.
 */
public class UserView extends VerticalLayout {

    private static final int PAGE_SIZE = 10;

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    private final UserListCard listCard = new UserListCard();
    private final UserDetailCard detailCard = new UserDetailCard();
    private final TextField search = new TextField();
    private final Button addButton = new Button(Messages.get(Messages.Keys.ACTION_NEW_USER),
            event -> openDialog(null));

    private final List<StoreDto> stores = new ArrayList<>();
    private final Map<Integer, String> storeNames = new HashMap<>();

    private List<UserDto> pageUsers = new ArrayList<>();
    private UserDto selectedUser;
    private Integer pendingSelectId;
    private int currentPage = 1;
    private int totalPages;
    private int totalUsers;
    private boolean loaded;

    public UserView(AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                    RestClientOrganizationService restClientOrganizationService,
                    AccessService accessService) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;
        this.accessService = accessService;

        addClassName("user-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        PageHeader header = PageHeader.of("page.user.title", "page.user.description");

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
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!loaded) {
            loaded = true;
            Shortcuts.addShortcutListener(this, search::focus, Key.SLASH);
        }
        loadStores();
        fetchUsers();
    }

    private void loadStores() {
        Integer chainId = accessService.getChainId();
        if (chainId == null) {
            return;
        }
        asyncRestClientOrganizationService.getAllStoreAsync(
                result -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    stores.clear();
                    storeNames.clear();
                    Object data = result == null ? null : result.get("data");
                    if (data instanceof List<?> rows) {
                        for (Object row : rows) {
                            StoreDto store = ObjectUtil.convertValueToObject(row, StoreDto.class);
                            if (store != null) {
                                stores.add(store);
                                storeNames.put(store.getId(), store.getName());
                            }
                        }
                    }
                    listCard.setUsers(pageUsers, storeNames);
                    if (selectedUser != null) {
                        detailCard.show(selectedUser, storeName(selectedUser));
                    }
                    addButton.setEnabled(true);
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null), () -> addButton.setEnabled(true)),
                chainId, 1, 500, "");
    }

    private void fetchUsers() {
        Integer chainId = accessService.getChainId();
        if (chainId == null) {
            return;
        }
        listCard.setLoading(true);
        asyncRestClientOrganizationService.getAllUserByChainAsync(
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
                chainId, currentPage, PAGE_SIZE, search.getValue());
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
        detailCard.show(user, storeName(user));
    }

    private String storeName(UserDto user) {
        return user == null || user.getStoreId() == null ? null : storeNames.get(user.getStoreId());
    }

    private void openDialog(UserDto user) {
        new UserDialog(restClientOrganizationService, stores, user, () -> {
            pendingSelectId = user == null ? null : user.getId();
            if (user == null) {
                currentPage = 1;
            }
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
}
