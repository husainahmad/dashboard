package com.harmoni.menu.dashboard.layout.organization.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.event.user.UserDeleteEventListener;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.enums.RoleType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Route(value = "users-list", layout = MainLayout.class)
@PageTitle("User | POSHarmoni")
@Slf4j
public class UserListView extends VerticalLayout {

    Registration broadcasterRegistration;
    private final Grid<UserDto> userDtoGrid = new Grid<>(UserDto.class);
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    TextField filterText = new TextField();
    Text pageInfoText;

    UI ui;
    int totalPages;
    int currentPage = 1;
    static final int TEMP_BRAND_ID = 1;

    private void renderLayout() {
        addClassName("list-view");
        setSizeFull();
        configureGrid();
        add(getToolbar(), getContent(), getPaginationFooter());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                        && (broadcastMessage.getType().equals(BroadcastMessage.STORE_INSERT_SUCCESS) ||
                    broadcastMessage.getType().equals(BroadcastMessage.STORE_UPDATED_SUCCESS))) {
                        fetchUsers();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });

        renderLayout();
        fetchUsers();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void configureGrid() {
        userDtoGrid.setSizeFull();
        userDtoGrid.removeAllColumns();
        userDtoGrid.addColumn(UserDto::getUsername).setHeader("Name");
        userDtoGrid.addComponentColumn(userDto -> {
            return switch (userDto.getAuthId()) {
                case 1 -> new Span(RoleType.ADMIN.name());
                case 2 -> new Span(RoleType.MANAGER.name());
                default -> new Span(RoleType.USER.name());
            };
        }).setHeader("Auth");
        userDtoGrid.addComponentColumn(this::applyGroupButton).setHeader("Action");
        userDtoGrid.getColumns().forEach(storeDtoColumn -> storeDtoColumn.setAutoWidth(true));
    }

    private Component applyGroupButton(UserDto userDto) {
        HorizontalLayout horizontalLayout = new HorizontalLayout();

        Button editButton = new Button("Edit");
        editButton.addClickListener(_ -> showAddEditUser(userDto, "Edit User", FormAction.EDIT));

        horizontalLayout.add(editButton);

        Button deleteButton = new Button("Delete");
        deleteButton.addClickListener(new UserDeleteEventListener(userDto, this.restClientOrganizationService));
        horizontalLayout.add(deleteButton);

        return horizontalLayout;
    }

    private void showAddEditUser(UserDto userDto, String title, FormAction action) {
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        Tab tabNewStore = new Tab();
        tabNewStore.setLabel(title);
        tabSheet.add(tabNewStore, new UserForm(this.asyncRestClientOrganizationService,
                this.restClientOrganizationService, this.accessService, tabNewStore, action, userDto));
        tabSheet.setSizeFull();
        tabSheet.setSelectedTab(tabNewStore);
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(userDtoGrid);
        content.setFlexGrow(1, userDtoGrid);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(changeEvent -> {
            if (!changeEvent.getOldValue().equals(changeEvent.getValue())) {
                currentPage = 1;
                fetchUsers();
            }
        });

        Button addChainButton = new Button("Add User");
        addChainButton.addClickListener(_ -> showAddEditUser(new UserDto(), "New User", FormAction.CREATE));
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addChainButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private HorizontalLayout getPaginationFooter() {
        HorizontalLayout paginationFooter = new HorizontalLayout();
        Button previousButton = new Button("Previous", _ -> {
            if (currentPage > 1) {
                currentPage--;
                fetchUsers();
            }
        });
        Button nextButton = new Button("Next", _ -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchUsers();
            }
        });
        pageInfoText = new Text(getPaginationInfo());
        paginationFooter.add(previousButton, pageInfoText, nextButton);
        paginationFooter.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        paginationFooter.setWidthFull();
        paginationFooter.setJustifyContentMode(JustifyContentMode.BETWEEN);
        return paginationFooter;
    }

    private String getPaginationInfo() {
        return "Page "
                .concat(String.valueOf(currentPage))
                .concat(" of ")
                .concat(String.valueOf(totalPages));
    }

    private void fetchUsers() {
        int pageSize = 10;
        asyncRestClientOrganizationService.getAllUserByChainAsync(result -> {
            if (ObjectUtils.isNotEmpty(result.get("data"))
                    && result.get("data") instanceof List<?> dataList && !dataList.isEmpty()) {
                totalPages = Integer.parseInt(result.get("page") == null ? "0" :result.get("page").toString());

                List<UserDto> userDtos = new ArrayList<>();
                dataList.forEach(object -> {
                    UserDto userDto = ObjectUtil.convertValueToObject(object, UserDto.class);
                    userDtos.add(userDto);
                });

                ui.access(()-> {
                    userDtoGrid.setItems(userDtos);
                    pageInfoText.setText(getPaginationInfo());
                });
            }
        }, accessService.getUserDetail().getStoreDto().getChainId(), currentPage, pageSize, filterText.getValue());
    }

}
