package com.harmoni.menu.dashboard.layout.menu.customization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CustomizationDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Slf4j
public class CustomizationListView extends VerticalLayout implements BroadcastMessageService {

    static final String TAB_LABEL_LIST = "All Customizations";
    static final String TAB_LABEL_NEW = "New Customization";

    private static final int PAGE_SIZE = 15;
    private static final long SEARCH_TIMEOUT_MS = 400;

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    private final Grid<CustomizationDto> customizationGrid = new Grid<>();
    private final TextField filterText = new TextField();
    private final ComboBox<BrandDto> brandDtoComboBox = new ComboBox<>();
    private final Text pageInfoText = new Text("");
    private final LoadingBar loadingBar = new LoadingBar();
    private final Button previousButton = new Button("Previous");
    private final Button nextButton = new Button("Next");
    private final AtomicInteger requestGeneration = new AtomicInteger();

    private transient List<BrandDto> brandDtos = new ArrayList<>();
    private Registration broadcasterRegistration;
    private transient UI ui;
    private int totalPages;
    private int currentPage = 1;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        if (broadcasterRegistration == null) {
            broadcasterRegistration = Broadcaster.register(this::acceptNotification);
        }
        buildLayout();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }

    private void buildLayout() {
        setSizeFull();
        setPadding(false);

        configureGrid();
        configureSearch();
        configureBrandSelector();
        configurePagination();

        VerticalLayout browsePanel = new VerticalLayout(loadingBar, getContent(), getPaginationFooter());
        browsePanel.setSizeFull();
        browsePanel.setPadding(false);
        browsePanel.setSpacing(false);

        add(browsePanel);
        setFlexGrow(1, browsePanel);

        fetchBrands();
    }

    private void configureGrid() {
        customizationGrid.setSizeFull();
        customizationGrid.setEmptyStateText(UiUtil.NO_RECORDS);
        customizationGrid.addColumn(CustomizationDto::getName).setHeader("Name").setAutoWidth(true);
        customizationGrid.addColumn(customization -> customization.getSelectionType() != null
                        ? customization.getSelectionType().getLabel() : "-")
                .setHeader("Type").setAutoWidth(true);
        customizationGrid.getColumns().forEach(column -> column.setResizable(true));
    }

    private void configureSearch() {
        filterText.setLabel("Search");
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setPrefixComponent(VaadinIcon.SEARCH.create());
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                currentPage = 1;
                fetchCustomizations();
            }
        });
    }

    private void configureBrandSelector() {
        brandDtoComboBox.setLabel("Brand");
        brandDtoComboBox.setPlaceholder("Select brand");
        brandDtoComboBox.setClearButtonVisible(true);
        brandDtoComboBox.setItemLabelGenerator(BrandDto::getName);
        brandDtoComboBox.setItems(Collections.emptyList());
        brandDtoComboBox.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                currentPage = 1;
                fetchCustomizations();
            }
        });
    }

    private void configurePagination() {
        previousButton.addClickListener(event -> {
            if (currentPage > 1) {
                currentPage--;
                fetchCustomizations();
            }
        });
        nextButton.addClickListener(event -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchCustomizations();
            }
        });
    }

    public HorizontalLayout getToolbarComponent() {
        Button addButton = UiUtil.addButton("New Customization", this::onAddCustomizationListener);
        HorizontalLayout toolbar = new HorizontalLayout(brandDtoComboBox, filterText, addButton);
        toolbar.addClassName("toolbar");
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        return toolbar;
    }

    private VerticalLayout getContent() {
        VerticalLayout content = new VerticalLayout(customizationGrid);
        content.setSizeFull();
        content.addClassNames("content");
        content.setFlexGrow(1, customizationGrid);
        return content;
    }

    private HorizontalLayout getPaginationFooter() {
        HorizontalLayout footer = new HorizontalLayout(previousButton, pageInfoText, nextButton);
        footer.addClassName("pagination");
        footer.setWidthFull();
        footer.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return footer;
    }

    private void updatePaginationState() {
        pageInfoText.setText("Page " + currentPage + " of " + Math.max(totalPages, 1));
        previousButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);
    }

    private void onAddCustomizationListener(ClickEvent<Button> event) {
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        TabManager tabManager = new TabManager(tabSheet);
        tabManager.addOrSelect(TAB_LABEL_NEW, tab -> {
            CustomizationForm form = new CustomizationForm(restClientMenuService, tabManager, tab);
            form.restructureButton(FormAction.CREATE);
            return form;
        });
    }

    private void fetchBrands() {
        restClientMenuService.getAllBrand()
                .subscribe(this::acceptBrands, error -> log.error("Failed to load brands", error));
    }

    private void acceptBrands(RestAPIResponse response) {
        if (ObjectUtils.isEmpty(response.getData())) {
            return;
        }
        brandDtos = ObjectUtil.convertObjectToObject(response.getData(), new TypeReference<>() {
        });
        if (brandDtos.isEmpty() || ui == null) {
            return;
        }
        ui.access(() -> {
            brandDtoComboBox.setItems(brandDtos);
            brandDtoComboBox.setValue(defaultBrand());
            fetchCustomizations();
        });
    }

    private BrandDto defaultBrand() {
        try {
            Integer brandId = accessService.getUserDetail().getStoreDto().getChainDto().getBrandId();
            return brandDtos.stream()
                    .filter(brand -> brandId != null && brandId.equals(brand.getId()))
                    .findFirst()
                    .orElse(brandDtos.getFirst());
        } catch (NullPointerException e) {
            return brandDtos.getFirst();
        }
    }

    private void fetchCustomizations() {
        if (ui == null) {
            return;
        }
        ui.access(() -> {
            BrandDto brand = brandDtoComboBox.getValue();
            if (brand == null) {
                customizationGrid.setItems(Collections.emptyList());
                totalPages = 0;
                updatePaginationState();
                return;
            }
            int generation = requestGeneration.incrementAndGet();
            loadingBar.start();
            asyncRestClientMenuService.getAllCustomizationAsync(
                    result -> ui.access(() -> {
                        if (generation != requestGeneration.get()) {
                            return;
                        }
                        loadingBar.stop();
                        applyCustomizations(result);
                    }),
                    error -> ui.access(() -> {
                        if (generation != requestGeneration.get()) {
                            return;
                        }
                        loadingBar.stop();
                        handleLoadError(error);
                    }),
                    brand.getId(), currentPage, PAGE_SIZE, normalizeSearch(filterText.getValue()));
        });
    }

    private void applyCustomizations(Map<String, Object> result) {
        Object data = result.get("data");
        if (data instanceof List<?> list && !list.isEmpty()) {
            List<CustomizationDto> customizations = new ArrayList<>();
            list.forEach(object -> customizations.add(ObjectUtil.convertValueToObject(object, CustomizationDto.class)));
            totalPages = result.get("page") == null ? 0 : Integer.parseInt(result.get("page").toString());
            customizationGrid.setItems(customizations);
        } else {
            customizationGrid.setItems(Collections.emptyList());
            totalPages = 0;
        }
        updatePaginationState();
    }

    private void handleLoadError(Throwable error) {
        log.error("Failed to load customizations", error);
        if (!(error instanceof BusinessBadRequestException)) {
            UiUtil.error("Unable to load customizations");
        }
    }

    private String normalizeSearch(String value) {
        return value == null ? "" : value.trim();
    }

    private void acceptNotification(String message) {
        try {
            BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
            if (broadcastMessage != null
                    && (BroadcastMessage.CUSTOMIZATION_INSERT_SUCCESS.equals(broadcastMessage.getType())
                    || BroadcastMessage.CUSTOMIZATION_UPDATED_SUCCESS.equals(broadcastMessage.getType()))) {
                fetchCustomizations();
            }
        } catch (JsonProcessingException e) {
            log.error("Broadcast handler error", e);
        }
    }
}
