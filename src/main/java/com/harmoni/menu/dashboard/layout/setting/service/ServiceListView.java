package com.harmoni.menu.dashboard.layout.setting.service;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * List of services shown inside the service tab sheet: a {@link TreeGrid} of
 * services with their sub-services, a toolbar to add a new service and live
 * refresh driven by {@link Broadcaster} messages. Fetches data through the
 * injected {@link AsyncRestClientSettingService}.
 */
@RequiredArgsConstructor
@Slf4j
public class ServiceListView extends AbstractListView {

    private final TreeGrid<ServiceTreeItem> serviceTreeGrid = new TreeGrid<>(ServiceTreeItem.class);
    private final AsyncRestClientSettingService asyncRestClientSettingService;

    private final GridSkeleton gridSkeleton = new GridSkeleton(8);

    /**
     * Renders the grid layout with the service list and pagination footer.
     */
    private void renderLayout() {
        setSizeFull();
        setPadding(false);

        configureGrid();

        add(getContent());
    }

    /**
     * Configures the {@link TreeGrid} with the service name and sub-service name
     * columns, and adds expand/collapse listeners for logging.
     */
    private void configureGrid() {
        serviceTreeGrid.setSizeFull();
        serviceTreeGrid.removeAllColumns();
        serviceTreeGrid.addHierarchyColumn(ServiceTreeItem::getServiceName).setHeader(Messages.get(Messages.Keys.LABEL_FIELD_SERVICE_NAME));
        serviceTreeGrid.addColumn(ServiceTreeItem::getSubServiceName).setHeader(Messages.get("grid.header.subServiceName"));

        serviceTreeGrid.addCollapseListener(event -> event.getItems().forEach(serviceTreeItem ->
                log.debug("item collapse {}", serviceTreeItem)));

        serviceTreeGrid.addExpandListener(event -> {
            if (event.isFromClient()) {
                event.getItems().forEach(serviceTreeItem -> log.debug("item expand {}", serviceTreeItem));
            }
        });

        serviceTreeGrid.getColumns().forEach(productDtoColumn -> productDtoColumn.setAutoWidth(true));
    }

    /**
     * Toolbar with a "New Service" button that opens the create tab.
     *
     * @return the toolbar layout
     */
    public HorizontalLayout getToolbarComponent() {
        Button addServiceButton = new Button(Messages.get(Messages.Keys.ACTION_NEW_SERVICE), new Icon(VaadinIcon.PLUS));
        addServiceButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addServiceButton.addClickListener(event -> addService());
        HorizontalLayout toolbar = new HorizontalLayout(addServiceButton);
        toolbar.addClassName(Css.TOOLBAR);
        return toolbar;
    }

    private HorizontalLayout getContent() {
        return gridSlot(serviceTreeGrid, gridSkeleton);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshOnBroadcast(Set.of(BroadcastMessage.STORE_INSERT_SUCCESS,
                BroadcastMessage.STORE_UPDATED_SUCCESS), this::fetchServices);
        renderLayout();
        this.fetchServices();
    }

    /**
     * Fetches the list of services from the {@link AsyncRestClientSettingService}
     * and populates the {@link TreeGrid}. Shows a skeleton loader while fetching
     * and handles errors with a retry option.
     */
    private void fetchServices() {
        gridSkeleton.show();
        asyncRestClientSettingService.getAllService(result -> {
            TreeData<ServiceTreeItem> serviceDtoTreeData = new TreeData<>();
            result.forEach(serviceDto -> {
                ServiceTreeItem serviceTreeItem = ServiceTreeItem.builder()
                        .id(String.valueOf(serviceDto.getId()))
                        .serviceName(serviceDto.getName())
                        .build();
                serviceDtoTreeData.addItem(null, serviceTreeItem);
                if (ObjectUtils.isNotEmpty(serviceDto.getSubServices())) {
                    serviceDtoTreeData.addItems(serviceTreeItem, getSubServices(serviceDto));
                }
            });
            UiUtil.safeAccess(ui, () -> {
                gridSkeleton.hide();
                serviceTreeGrid.setTreeData(serviceDtoTreeData);
            });
        }, error -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_SERVICE_LOAD_FAILED), this::fetchServices);
        }));
    }

    /**
     * Converts a {@link ServiceDto} with its sub-services into a list of
     * {@link ServiceTreeItem} for the {@link TreeGrid}.
     *
     * @param serviceDto the service DTO containing sub-services
     * @return a list of tree items representing the sub-services
     */
    private List<ServiceTreeItem> getSubServices(ServiceDto serviceDto) {
        List<ServiceTreeItem> serviceTreeItems = new ArrayList<>();
        serviceDto.getSubServices().forEach(subServiceDto -> serviceTreeItems.add(ServiceTreeItem.builder()
                .id(String.valueOf(serviceDto.getId())
                        .concat("-")
                        .concat(String.valueOf(subServiceDto.getId())))
                .serviceName("")
                .subServiceName(subServiceDto.getName())
                .build()));
        return serviceTreeItems;
    }

    /**
     * Opens a new tab with the {@link ServiceForm} for creating a new service.
     * Clears any selection in the {@link TreeGrid} and uses the parent
     * {@link TabSheet} to manage the new tab.
     */
    private void addService() {
        serviceTreeGrid.asSingleSelect().clear();
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        TabManager tabManager = new TabManager(tabSheet);
        tabManager.addOrSelect(Messages.get(Messages.Keys.ACTION_NEW_SERVICE), tab ->
                new ServiceForm(tabManager, tab, FormAction.CREATE, new ServiceDto()));
    }

}
