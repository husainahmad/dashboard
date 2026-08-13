package com.harmoni.menu.dashboard.layout.menu.customization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@UIScope
@PreserveOnRefresh
@Route(value = "customization-list", layout = MainLayout.class)
@PageTitle("Customization | POSHarmoni")
@Component
@Slf4j
public class CustomizationListView extends VerticalLayout implements BroadcastMessageService {

    Registration broadcasterRegistration;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;
    private final TabSheet tabSheet;
    private final Tab tab;

    Grid<CustomizationDto> customizationGrid = new Grid<>(CustomizationDto.class);
    TextField filterText = new TextField();
    ComboBox<BrandDto> brandDtoComboBox = new ComboBox<>();
    transient List<BrandDto> brandDtos = new ArrayList<>();

    UI ui;
    int totalPages;
    int currentPage = 1;
    Text pageInfoText;
    LoadingBar loadingBar = new LoadingBar();

    private void renderLayout() {
        addClassName("list-view");
        setSizeFull();
        configureGrid();
        add(loadingBar, getToolbar(), getContent());
        fetchBrands();
    }

    private void configureGrid() {
        customizationGrid.setSizeFull();
        customizationGrid.removeAllColumns();
        customizationGrid.setEmptyStateText(UiUtil.NO_RECORDS);
        customizationGrid.addColumn(CustomizationDto::getName).setHeader("Name");
        customizationGrid.addColumn(CustomizationDto::getSelectionType).setHeader("Type");
        customizationGrid.addComponentColumn(this::applyButton).setHeader("Action");
        customizationGrid.getColumns().forEach(c -> c.setAutoWidth(true));
    }

    private Button applyButton(CustomizationDto dto) {
        Button button = new Button("Apply");
        button.addClickListener(e -> {
            // Do something with dto
        });
        return button;
    }

    private HorizontalLayout getToolbar() {
        filterText.setLabel("Customization");
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                currentPage = 1;
                fetchCustomizations(brandDtoComboBox.getValue().getId(), filterText.getValue());
            }
        });

        brandDtoComboBox.setItems(brandDtos);
        brandDtoComboBox.setLabel("Brand");
        brandDtoComboBox.setItemLabelGenerator(BrandDto::getName);
        brandDtoComboBox.addValueChangeListener(valueChangeEvent -> {
            if (valueChangeEvent.isFromClient()) {
                fetchCustomizations(valueChangeEvent.getValue().getId(), filterText.getValue());
            }
        });

        Button searchButton = new Button("Search", this::onSearchCustomizationListener);
        Button addButton = UiUtil.addButton("Add Customization", this::onAddCustomizationListener);

        HorizontalLayout toolbar = new HorizontalLayout(brandDtoComboBox, filterText, searchButton, addButton);
        toolbar.addClassName("toolbar");
        toolbar.setAlignItems(Alignment.BASELINE);
        return toolbar;
    }

    private VerticalLayout getContent() {
        VerticalLayout content = new VerticalLayout(customizationGrid);
        content.setFlexGrow(1, customizationGrid);
        content.addClassNames("content");
        content.setSizeFull();
        content.add(getPaginationFooter());
        return content;
    }

    private HorizontalLayout getPaginationFooter() {
        HorizontalLayout footer = new HorizontalLayout();
        footer.addClassName("pagination");
        Button prev = new Button("Previous", e -> {
            if (currentPage > 1) {
                currentPage--;
                fetchCustomizations(brandDtoComboBox.getValue().getId(), filterText.getValue());
            }
        });
        Button next = new Button("Next", e -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchCustomizations(brandDtoComboBox.getValue().getId(), filterText.getValue());
            }
        });
        pageInfoText = new Text(getPaginationInfo());
        footer.add(prev, pageInfoText, next);
        footer.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return footer;
    }

    private String getPaginationInfo() {
        return "Page " + currentPage + " of " + totalPages;
    }

    private void fetchBrands() {
        restClientMenuService.getAllBrand().subscribe(this::acceptBrands);
    }

    private void fetchCustomizations(Integer brandId, String search) {
        int pageSize = 15;
        loadingBar.start();
        asyncRestClientMenuService.getAllCustomizationAsync(result -> {
            Object data = result.get("data");
            ui.access(() -> {
                loadingBar.stop();
                if (ObjectUtils.isNotEmpty(data) && data instanceof List<?> list) {
                    List<CustomizationDto> customizations = new ArrayList<>();
                    list.forEach(o -> customizations.add(ObjectUtil.convertValueToObject(o, CustomizationDto.class)));
                    totalPages = Integer.parseInt(result.get("page") == null ? "0" : result.get("page").toString());

                    customizationGrid.setItems(customizations);
                    pageInfoText.setText(getPaginationInfo());
                }
            });
        }, brandId, currentPage, pageSize, search);
    }

    private void acceptBrands(RestAPIResponse restAPIResponse) {
        if (!ObjectUtils.isEmpty(restAPIResponse.getData())) {
            brandDtos = ObjectUtil.convertObjectToObject(restAPIResponse.getData(), new TypeReference<>() {});
            if (!brandDtos.isEmpty()) {
                ui.access(() -> {
                    brandDtoComboBox.setItems(brandDtos);
                    brandDtoComboBox.setValue(brandDtos.getFirst());
                    fetchCustomizations(brandDtoComboBox.getValue().getId(), filterText.getValue());
                });
            }
        }
    }

    private void onAddCustomizationListener(ClickEvent<Button> event) {
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        Tab tabNewCustomization = new Tab();
        tabNewCustomization.setLabel("New Customization");
        tabSheet.add(tabNewCustomization, new CustomizationForm(this.restClientMenuService,
                tabSheet, tabNewCustomization));
        tabSheet.setSizeFull();
        tabSheet.setSelectedTab(tabNewCustomization);

    }

    private void onSearchCustomizationListener(ClickEvent<Button> event) {
        currentPage = 1;
        fetchCustomizations(brandDtoComboBox.getValue().getId(), filterText.getValue());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(this::acceptNotification);
        renderLayout();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void acceptNotification(String message) {
        try {
            BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
            if (broadcastMessage != null && (broadcastMessage.getType().equals(BroadcastMessage.CUSTOMIZATION_INSERT_SUCCESS)
                    || broadcastMessage.getType().equals(BroadcastMessage.CUSTOMIZATION_UPDATED_SUCCESS))) {
                    fetchCustomizations(brandDtoComboBox.getValue().getId(), filterText.getValue());
                }
        } catch (JsonProcessingException e) {
            log.error("Broadcast Handler Error", e);
        }
    }
}
