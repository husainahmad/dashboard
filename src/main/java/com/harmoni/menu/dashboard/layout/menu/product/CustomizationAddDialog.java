package com.harmoni.menu.dashboard.layout.menu.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CustomizationDto;
import com.harmoni.menu.dashboard.dto.ProductCustomizationDto;
import com.harmoni.menu.dashboard.layout.util.AsyncUtil;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * "Add Customization" dialog: searches the customization master for the brand
 * with server-side pagination and lets the user pick the items to attach to
 * the product. Already-attached customizations are hidden because they are
 * visible in the product form's own grid. Selections are kept across page
 * turns so the user can pick items from several pages before confirming.
 * Confirmation delegates to {@link CustomizationSection#onAddSelected}.
 */
public class CustomizationAddDialog extends Dialog {

    private static final int PAGE_SIZE = 15;

    private final CustomizationSection section;
    private final ProductFormDelegate delegate;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final BrandDto brandDto;

    private final Grid<CustomizationDto> grid = new Grid<>(CustomizationDto.class);
    private final Map<Integer, CustomizationDto> selectedById = new LinkedHashMap<>();
    private final Set<Integer> attachedIds;
    private List<CustomizationDto> currentPageItems = new ArrayList<>();

    private final TextField searchField = new TextField();
    private final Text pageInfoText = new Text("");
    private Button previousPageButton;
    private Button nextPageButton;
    private Button addSelectedButton;
    private final AtomicInteger requestGeneration = new AtomicInteger();

    private int currentPage = 1;
    private int totalPages;

    /**
     * @param section                      the owner section that persists the selection
     * @param delegate                     the owner form, used for UI feedback
     * @param asyncRestClientMenuService   async client feeding the search list
     * @param brandDto                     brand filter for the customization master
     */
    public CustomizationAddDialog(CustomizationSection section, ProductFormDelegate delegate,
                                  AsyncRestClientMenuService asyncRestClientMenuService,
                                  BrandDto brandDto) {
        this.section = section;
        this.delegate = delegate;
        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.brandDto = brandDto;

        this.attachedIds = section.getProductCustomizations().stream()
                .map(ProductCustomizationDto::getCustomizationId)
                .collect(Collectors.toSet());

        addClassName("customization-add-dialog");
        setHeaderTitle(Messages.get("label.addCustomization"));
        setWidth("920px");

        configureSearch();
        configureGrid();

        VerticalLayout dialogContent = new VerticalLayout(searchField, grid, buildPaginationFooter());
        dialogContent.setPadding(false);
        dialogContent.setSpacing(true);
        add(dialogContent);

        Button cancelButton = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> close());
        addSelectedButton = UiUtil.addButton(Messages.get("action.addSelected"),
                event -> section.onAddSelected(getSelectedCustomizations(), this));
        updateAddSelectedState();
        getFooter().add(cancelButton, addSelectedButton);

        fetchCustomizations();
    }

    private void configureSearch() {
        searchField.setLabel(Messages.get(Messages.Keys.LABEL_SEARCH));
        searchField.setPlaceholder(Messages.get("placeholder.searchCustomizations"));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                currentPage = 1;
                fetchCustomizations();
            }
        });
    }

    private void configureGrid() {
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.setEmptyStateText(Messages.get("grid.empty.customizationPicker"));
        grid.setAllRowsVisible(true);
        grid.removeAllColumns();
        grid.addClassName("customization-picker-grid");
        grid.setSizeFull();
        grid.addColumn(CustomizationDto::getName)
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_NAME));
        grid.addColumn(dto -> dto.getSelectionType() == null ? "-" : dto.getSelectionType().getLabel())
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_TYPE));
        grid.addComponentColumn(this::renderRequiredColumn)
                .setHeader(Messages.get(Messages.Keys.LABEL_REQUIRED)).setWidth("90px");
        grid.addSelectionListener(event -> {
            if (!event.isFromClient()) {
                return;
            }
            Set<CustomizationDto> nowSelected = new HashSet<>(event.getAllSelectedItems());
            currentPageItems.forEach(item -> selectedById.remove(item.getId()));
            nowSelected.forEach(item -> selectedById.put(item.getId(), item));
            updateAddSelectedState();
        });
    }

    private void fetchCustomizations() {
        if (previousPageButton != null) {
            previousPageButton.setEnabled(false);
            nextPageButton.setEnabled(false);
        }
        int generation = requestGeneration.incrementAndGet();
        asyncRestClientMenuService.getAllCustomizationAsync(
                result -> AsyncUtil.onUi(delegate.getUi(), () -> {
                    if (generation != requestGeneration.get()) {
                        return;
                    }
                    List<CustomizationDto> list = ObjectUtils.isEmpty(result.get("data"))
                            ? new ArrayList<>()
                            : ObjectUtil.convertObjectToObject(result.get("data"), new TypeReference<>() {
                    });
                    totalPages = result.get("page") == null ? 0 : Integer.parseInt(result.get("page").toString());
                    applyPage(list);
                    updatePagination();
                }),
                throwable -> AsyncUtil.onUi(delegate.getUi(), () -> {
                    if (generation != requestGeneration.get()) {
                        return;
                    }
                    delegate.showErrorDialog(Messages.get(Messages.Keys.NOTIFICATION_CUSTOMIZATION_LOAD_FAILED));
                    updatePagination();
                }),
                brandDto.getId(), currentPage, PAGE_SIZE, normalizeSearch(searchField.getValue()));
    }

    private void applyPage(List<CustomizationDto> items) {
        currentPageItems = items.stream()
                .filter(item -> !attachedIds.contains(item.getId()))
                .toList();
        grid.setItems(currentPageItems);
        currentPageItems.stream()
                .filter(item -> selectedById.containsKey(item.getId()))
                .forEach(grid::select);
    }

    private Set<CustomizationDto> getSelectedCustomizations() {
        return new LinkedHashSet<>(selectedById.values());
    }

    private void updateAddSelectedState() {
        if (addSelectedButton != null) {
            addSelectedButton.setEnabled(!selectedById.isEmpty());
        }
    }

    private HorizontalLayout buildPaginationFooter() {
        previousPageButton = new Button(Messages.get("action.previous"), event -> {
            if (currentPage > 1) {
                currentPage--;
                fetchCustomizations();
            }
        });
        nextPageButton = new Button(Messages.get("action.next"), event -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchCustomizations();
            }
        });
        HorizontalLayout footer = new HorizontalLayout(previousPageButton, pageInfoText, nextPageButton);
        footer.addClassName("pagination");
        footer.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        updatePagination();
        return footer;
    }

    private void updatePagination() {
        pageInfoText.setText(Messages.get("pagination.page", currentPage, totalPages));
        if (previousPageButton != null) {
            previousPageButton.setEnabled(currentPage > 1);
            nextPageButton.setEnabled(currentPage < totalPages);
        }
    }

    private String normalizeSearch(String value) {
        return value == null ? "" : value.trim();
    }

    private Component renderRequiredColumn(CustomizationDto dto) {
        HorizontalLayout wrapper = new HorizontalLayout();
        wrapper.setWidthFull();
        wrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        if (Boolean.TRUE.equals(dto.getRequired())) {
            Icon check = new Icon(VaadinIcon.CHECK);
            check.setColor("var(--lumo-primary-color)");
            wrapper.add(check);
        }
        return wrapper;
    }
}