package com.harmoni.menu.dashboard.layout.setting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Route(value = "service", layout = MainLayout.class)
@PageTitle("Service | POSHarmoni")
@Slf4j
public class ServiceListView extends VerticalLayout {

    private static final String EDITING = "editing";
    Registration broadcasterRegistration;

    private final TreeGrid<ServiceTreeItem> serviceTreeGrid = new TreeGrid<>(ServiceTreeItem.class);
    private final AsyncRestClientSettingService asyncRestClientSettingService;

    private ServiceForm serviceForm;
    private UI ui;

    private void renderLayout() {
        addClassName("list-view");
        setSizeFull();

        configureGrid();
        configureForm();

        add(getToolbar(), getContent());
        closeEditor();
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

    private void configureForm() {
        this.serviceForm = new ServiceForm(this.asyncRestClientSettingService);
        this.serviceForm.setWidth("25em");
    }

    private HorizontalLayout getToolbar() {
        Button addServiceButton = new Button("Add Service");
        addServiceButton.addClickListener(_ -> addService());
        HorizontalLayout toolbar = new HorizontalLayout( addServiceButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(serviceTreeGrid, serviceForm);
        content.setFlexGrow(1, serviceTreeGrid);
        content.setFlexGrow(1, serviceForm);
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
                        ui.access(()->{
                            serviceForm.setVisible(false);
                            removeClassName(EDITING);
                        });
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
            ui.access(() -> serviceTreeGrid.setTreeData(serviceDtoTreeData));
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
        editService(new ServiceDto());
    }

    public void editService(ServiceDto serviceDto) {
        if (serviceDto == null) {
            closeEditor();
        }
    }

    private void closeEditor() {
        serviceForm.setVisible(false);
        removeClassName(EDITING);
    }

}
