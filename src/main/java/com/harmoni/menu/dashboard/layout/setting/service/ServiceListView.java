package com.harmoni.menu.dashboard.layout.setting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.shared.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * List of services shown inside the service tab sheet: a {@link TreeGrid} of
 * services with their sub-services, a toolbar to add a new service and live
 * refresh driven by {@link Broadcaster} messages. Fetches data through the
 * injected {@link AsyncRestClientSettingService}.
 */
@RequiredArgsConstructor
@Slf4j
public class ServiceListView extends VerticalLayout {

    Registration broadcasterRegistration;

    private final TreeGrid<ServiceTreeItem> serviceTreeGrid = new TreeGrid<>(ServiceTreeItem.class);
    private final AsyncRestClientSettingService asyncRestClientSettingService;

    UI ui;
    LoadingBar loadingBar = new LoadingBar();

    private void renderLayout() {
        setSizeFull();
        setPadding(false);

        configureGrid();

        add(loadingBar, getContent());
    }

    private void configureGrid() {
        serviceTreeGrid.setSizeFull();
        serviceTreeGrid.removeAllColumns();
        serviceTreeGrid.addHierarchyColumn(ServiceTreeItem::getServiceName).setHeader("Service Name");
        serviceTreeGrid.addColumn(ServiceTreeItem::getSubServiceName).setHeader("Sub Service Name");

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
     * Toolbar with a "New Service" button that opens the create dialog.
     *
     * @return the toolbar layout
     */
    public HorizontalLayout getToolbarComponent() {
        Button addServiceButton = new Button("New Service", new Icon(VaadinIcon.PLUS));
        addServiceButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addServiceButton.addClickListener(event -> addService());
        HorizontalLayout toolbar = new HorizontalLayout(addServiceButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(serviceTreeGrid);
        content.setFlexGrow(1, serviceTreeGrid);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
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
                        fetchServices();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
        renderLayout();
        this.fetchServices();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void fetchServices() {
        loadingBar.start();
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
            ui.access(() -> {
                loadingBar.stop();
                serviceTreeGrid.setTreeData(serviceDtoTreeData);
            });
        });
    }

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

    private void addService() {
        serviceTreeGrid.asSingleSelect().clear();
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Service");
        dialog.setWidth("400px");
        dialog.add(new ServiceForm(dialog, FormAction.CREATE, new ServiceDto()));
        dialog.open();
    }

}
