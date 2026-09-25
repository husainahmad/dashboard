package com.harmoni.menu.dashboard.layout.organization.user;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.event.user.UserDeleteEventListener;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.enums.RoleType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Vaadin grid view listing the users of the current user's chain with
 * pagination. Shows the role of each user, opens add/edit {@link UserForm}s in
 * tabs via {@link TabManager}, and refreshes on BROADCAST insert/update.
 */
@RequiredArgsConstructor
@Route(value = "users-list", layout = MainLayout.class)
@PageTitle("User | POSHarmoni")
@Slf4j
public class UserListView extends AbstractListView {

    private final Grid<UserDto> userDtoGrid = new Grid<>(UserDto.class);
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    private final GridSkeleton gridSkeleton = new GridSkeleton(10);

    /**
     * Renders the grid layout with the user list and pagination footer.
     */
    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        configureGrid();
        add(getContent(), getPaginationFooter());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshOnBroadcast(Set.of(BroadcastMessage.STORE_INSERT_SUCCESS,
                BroadcastMessage.STORE_UPDATED_SUCCESS), this::fetchUsers);

        renderLayout();
        fetchUsers();
    }

    /**
     * Configures the user grid with columns for username, role, and action buttons.
     * Sets the grid to full size and defines the empty state text.
     */
    private void configureGrid() {
        userDtoGrid.setSizeFull();
        userDtoGrid.removeAllColumns();
        userDtoGrid.setEmptyStateText(Messages.get("grid.empty.users"));
        userDtoGrid.addColumn(UserDto::getUsername).setHeader(Messages.get(Messages.Keys.GRID_HEADER_NAME));
        userDtoGrid.addComponentColumn(userDto -> {
            return switch (userDto.getAuthId()) {
                case 1 -> new Span(RoleType.ADMIN.name());
                case 2 -> new Span(RoleType.MANAGER.name());
                default -> new Span(RoleType.USER.name());
            };
        }).setHeader(Messages.get("grid.header.auth"));
        userDtoGrid.addComponentColumn(this::applyGroupButton).setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTION));
        userDtoGrid.getColumns().forEach(storeDtoColumn -> storeDtoColumn.setAutoWidth(true));
    }

    /**
     * Creates a horizontal layout with edit and delete buttons for a given user.
     *
     * @param userDto the user data transfer object
     * @return a horizontal layout containing the action buttons
     */
    private Component applyGroupButton(UserDto userDto) {
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.add(UiUtil.editButton(
                event -> showAddEditUser(userDto, Messages.get("action.editUser"), FormAction.EDIT)));
        horizontalLayout.add(UiUtil.deleteButton(
                new UserDeleteEventListener(userDto, this.restClientOrganizationService)));
        return horizontalLayout;
    }

    /**
     * Opens a new tab with the {@link UserForm} for adding or editing a user.
     *
     * @param userDto the user data transfer object to edit, or a new instance for adding
     * @param title   the title of the tab
     * @param action  the form action (CREATE or EDIT)
     */
    private void showAddEditUser(UserDto userDto, String title, FormAction action) {
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        String tabLabel = action == FormAction.EDIT
                && ObjectUtils.isNotEmpty(userDto.getUsername())
                ? Messages.get(Messages.Keys.ACTION_EDIT_NAME, userDto.getUsername()) : title;
        new TabManager(tabSheet).addOrSelect(tabLabel, tab ->
                new UserForm(this.asyncRestClientOrganizationService,
                        this.restClientOrganizationService, this.accessService, tab, action, userDto));
    }

    /**
     * Creates a horizontal layout containing the user grid and a skeleton loader.
     *
     * @return a horizontal layout with the grid and skeleton
     */
    private HorizontalLayout getContent() {
        return gridSlot(userDtoGrid, gridSkeleton);
    }

    /**
     * Builds the toolbar with a lazy name filter and a "New User" button.
     *
     * @return the toolbar layout to place above the grid
     */
    public HorizontalLayout getToolbarComponent() {
        configureSearchFilter();
        filterText.getElement().setAttribute(Css.AUTOCOMPLETE, "off");
        filterText.addValueChangeListener(changeEvent -> {
            if (!changeEvent.getOldValue().equals(changeEvent.getValue())) {
                currentPage = 1;
                fetchUsers();
            }
        });

        Button addChainButton = UiUtil.addButton(Messages.get(Messages.Keys.ACTION_NEW_USER),
                event -> showAddEditUser(new UserDto(), Messages.get(Messages.Keys.ACTION_NEW_USER), FormAction.CREATE));
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addChainButton);
        toolbar.addClassName(Css.TOOLBAR);
        return toolbar;
    }

    /**
     * Creates a pagination footer with previous and next buttons.
     * The buttons update the current page and fetch users accordingly.
     *
     * @return a horizontal layout containing the pagination controls
     */
    private HorizontalLayout getPaginationFooter() {
        return paginationFooter(() -> {
            if (currentPage > 1) {
                currentPage--;
                fetchUsers();
            }
        }, () -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchUsers();
            }
        });
    }

    /**
     * Fetches the list of users from the backend service asynchronously.
     * Updates the grid with the retrieved users and handles pagination.
     * Displays a skeleton loader while fetching and shows an error notification on failure.
     */
    private void fetchUsers() {
        int pageSize = 10;
        gridSkeleton.show();
        asyncRestClientOrganizationService.getAllUserByChainAsync(result -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            if (ObjectUtils.isNotEmpty(result.get("data"))
                    && result.get("data") instanceof List<?> dataList && !dataList.isEmpty()) {
                totalPages = Integer.parseInt(result.get("page") == null ? "0" :result.get("page").toString());

                List<UserDto> userDtos = new ArrayList<>();
                dataList.forEach(object -> {
                    UserDto userDto = ObjectUtil.convertValueToObject(object, UserDto.class);
                    userDtos.add(userDto);
                });

                userDtoGrid.setItems(userDtos);
                updatePagination();
            } else {
                userDtoGrid.setItems(new ArrayList<>());
                totalPages = 0;
                updatePagination();
            }
        }), error -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            UiUtil.errorWithRetry(Messages.get("notification.user.loadFailed"), this::fetchUsers);
        }), accessService.getUserDetail().getStoreDto().getChainId(), currentPage, pageSize, filterText.getValue());
    }

}
