package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ProductSelectionDialog extends Dialog {

    private final AsyncRestClientMenuService menuService;
    private final Integer brandId;
    private final List<ProductDto> selectedProducts = new ArrayList<>();
    private final Consumer<List<ProductDto>> onConfirm;
    private final ListDataProvider<ProductDto> dataProvider;

    private final TextField searchField = new TextField();
    private final ComboBox<CategoryDto> categoryFilter = new ComboBox<>();
    private final VerticalLayout productList = new VerticalLayout();
    private final Div loadingIndicator = new Div();
    private final Button confirmButton = new Button();
    private final Button cancelButton = new Button();
    private final Button selectAllButton = new Button();
    private final Button clearAllButton = new Button();

    private List<CategoryDto> allCategories = new ArrayList<>();
    private Integer selectedCategoryId = null;
    private boolean initialLoadStarted;

    public ProductSelectionDialog(AsyncRestClientMenuService menuService,
                                  Integer brandId,
                                  List<ProductDto> preSelectedProducts,
                                  Consumer<List<ProductDto>> onConfirm) {
        this.menuService = menuService;
        this.brandId = brandId;
        this.onConfirm = onConfirm;

        if (preSelectedProducts != null) {
            this.selectedProducts.addAll(preSelectedProducts);
        }

        setHeaderTitle(Messages.get("dialog.productSelection.title"));
        setWidth(Css.DIALOG_WIDTH);
        setMaxWidth("90vw");
        setHeight("70vh");
        setDraggable(true);
        setModal(true);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);

        dataProvider = new ListDataProvider<>(new ArrayList<>());

        buildContent();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!initialLoadStarted) {
            initialLoadStarted = true;
            loadCategories();
        }
    }

    private void buildContent() {
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSizeFull();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setMargin(false);

        // Search field
        HorizontalLayout searchLayout = new HorizontalLayout();
        searchLayout.setWidthFull();
        searchLayout.setPadding(true);
        searchLayout.setSpacing(true);
        searchLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        searchField.setPlaceholder(Messages.get("dialog.productSelection.searchPlaceholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();
        searchField.addValueChangeListener(e -> filterProducts(e.getValue()));
        searchLayout.add(searchField);

        // Category filter
        HorizontalLayout categoryLayout = new HorizontalLayout();
        categoryLayout.setWidthFull();
        categoryLayout.setPadding(true);
        categoryLayout.setSpacing(true);
        categoryLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        categoryFilter.setLabel(Messages.get("dialog.productSelection.category"));
        categoryFilter.setItemLabelGenerator(CategoryDto::getName);
        categoryFilter.setWidthFull();
        categoryFilter.setClearButtonVisible(false);
        categoryFilter.addValueChangeListener(e -> {
            CategoryDto selected = e.getValue();
            selectedCategoryId = selected != null ? selected.getId() : null;
            loadProducts();
        });
        categoryLayout.add(categoryFilter);

        // Toolbar
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(true);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);

        selectAllButton.setText(Messages.get("dialog.productSelection.selectAll"));
        selectAllButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        selectAllButton.addClickListener(e -> selectAll(true));

        clearAllButton.setText(Messages.get("dialog.productSelection.clearAll"));
        clearAllButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        clearAllButton.addClickListener(e -> selectAll(false));

        toolbar.add(selectAllButton, clearAllButton);
        toolbar.setFlexGrow(1, selectAllButton);

        // Product list with loading
        productList.setSizeFull();
        productList.setPadding(false);
        productList.setSpacing(false);

        loadingIndicator.setText(Messages.get("dialog.productSelection.loading"));
        loadingIndicator.getStyle()
                .set(Css.DISPLAY, "flex")
                .set(Css.ALIGN_ITEMS, "center")
                .set(Css.JUSTIFY_CONTENT, "center")
                .set(Css.HEIGHT, "200px")
                .set(Css.COLOR, "var(--lumo-secondary-text-color)");
        loadingIndicator.setVisible(false);

        VerticalLayout listContainer = new VerticalLayout(productList, loadingIndicator);
        listContainer.setSizeFull();
        listContainer.setPadding(false);
        listContainer.setSpacing(false);
        listContainer.getStyle().set("overflow-y", "auto").set("flex-grow", "1");

        // Footer
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setPadding(true);
        footer.setSpacing(true);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.getStyle().set("border-top", Css.HAIRLINE_BORDER);

        cancelButton.setText(Messages.get(Messages.Keys.ACTION_CANCEL));
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> close());

        confirmButton.setText(Messages.get("action.confirm"));
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirmButton.addClickListener(e -> {
            onConfirm.accept(new ArrayList<>(selectedProducts));
            close();
        });
        updateConfirmButton();

        footer.add(cancelButton, confirmButton);

        dialogLayout.add(searchLayout, categoryLayout, toolbar, listContainer, footer);
        dialogLayout.setFlexGrow(1, listContainer);
        add(dialogLayout);
    }

    private void loadProducts() {
        if (selectedCategoryId == null) {
            renderNoCategories();
            return;
        }
        loadingIndicator.setVisible(true);
        productList.removeAll();

        menuService.getAllProductCategoryBrandAsync(
                result -> {
                    UiUtil.safeAccess(getUI().orElse(null), () -> {
                        loadingIndicator.setVisible(false);
                        List<ProductDto> products = new ArrayList<>();
                        if (result != null && result.get("data") instanceof List<?> dataList) {
                            dataList.forEach(item -> products.add(
                                    ObjectUtil.convertValueToObject(item, ProductDto.class)));
                        }
                        dataProvider.getItems().clear();
                        dataProvider.getItems().addAll(products);
                        renderProductList();
                    });
                },
                error -> {
                    UiUtil.safeAccess(getUI().orElse(null), () -> {
                        loadingIndicator.setVisible(false);
                        UiUtil.error(Messages.get("dialog.productSelection.loadFailed") + ": " + error.getMessage());
                    });
                },
                selectedCategoryId, // categoryId - the product endpoint requires one
                brandId,
                0,    // page
                1000, // size - large page to get all
                ""    // search
        );
    }

    private void loadCategories() {
        loadingIndicator.setVisible(true);
        menuService.getAllCategoryAsync(
                categories -> {
                    UiUtil.safeAccess(getUI().orElse(null), () -> {
                        loadingIndicator.setVisible(false);
                        allCategories = categories != null ? categories : new ArrayList<>();
                        categoryFilter.setItems(allCategories);
                        if (allCategories.isEmpty()) {
                            renderNoCategories();
                            return;
                        }
                        categoryFilter.setValue(allCategories.getFirst());
                    });
                },
                error -> {
                    UiUtil.safeAccess(getUI().orElse(null), () -> {
                        loadingIndicator.setVisible(false);
                        UiUtil.error(Messages.get("dialog.productSelection.categoryLoadFailed") + ": " + error.getMessage());
                    });
                },
                brandId
        );
    }

    private void renderNoCategories() {
        productList.removeAll();
        Div emptyState = new Div();
        emptyState.setText(Messages.get("dialog.productSelection.noCategories"));
        emptyState.getStyle()
                .set(Css.DISPLAY, "flex")
                .set(Css.ALIGN_ITEMS, "center")
                .set(Css.JUSTIFY_CONTENT, "center")
                .set(Css.HEIGHT, "200px")
                .set(Css.COLOR, "var(--lumo-tertiary-text-color)");
        productList.add(emptyState);
    }

    private void filterProducts(String searchText) {
        String filter = searchText != null ? searchText.toLowerCase() : "";
        dataProvider.setFilter(product -> {
            if (filter.isEmpty()) return true;
            String name = product.getName() != null ? product.getName().toLowerCase() : "";
            String desc = product.getDescription() != null ? product.getDescription().toLowerCase() : "";
            return name.contains(filter) || desc.contains(filter);
        });
        renderProductList();
    }

    private void renderProductList() {
        productList.removeAll();

        List<ProductDto> filteredProducts = dataProvider.getItems()
                .stream()
                .collect(Collectors.toList());

        if (filteredProducts.isEmpty()) {
            Div emptyState = new Div();
            emptyState.setText(Messages.get("dialog.productSelection.empty"));
            emptyState.getStyle()
                    .set(Css.DISPLAY, "flex")
                    .set(Css.ALIGN_ITEMS, "center")
                    .set(Css.JUSTIFY_CONTENT, "center")
                    .set(Css.HEIGHT, "200px")
                    .set(Css.COLOR, "var(--lumo-tertiary-text-color)");
            productList.add(emptyState);
            return;
        }

        for (ProductDto product : filteredProducts) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setSpacing(true);
            row.setPadding(true);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.getStyle()
                    .set(Css.BORDER_BOTTOM, Css.HAIRLINE_BORDER)
                    .set("transition", "background-color 0.15s");

            row.getElement().addEventListener("mouseenter", e ->
                    row.getStyle().set(Css.BACKGROUND_COLOR, "var(--lumo-contrast-5pct)"));
            row.getElement().addEventListener("mouseleave", e ->
                    row.getStyle().set(Css.BACKGROUND_COLOR, "transparent"));

            Checkbox checkbox = new Checkbox();
            checkbox.setValue(selectedProducts.stream()
                    .anyMatch(p -> p.getId() != null && p.getId().equals(product.getId())));
            checkbox.addValueChangeListener(e -> {
                if (e.getValue()) {
                    if (selectedProducts.stream().noneMatch(p -> p.getId() != null && p.getId().equals(product.getId()))) {
                        selectedProducts.add(product);
                    }
                } else {
                    selectedProducts.removeIf(p -> p.getId() != null && p.getId().equals(product.getId()));
                }
                updateConfirmButton();
            });

            Div productInfo = new Div();
            productInfo.setWidthFull();

            H4 nameLabel = new H4(product.getName());
            nameLabel.getStyle()
                    .set(Css.MARGIN, "0")
                    .set(Css.FONT_SIZE, "var(--lumo-font-size-m)")
                    .set(Css.FONT_WEIGHT, "500")
                    .set(Css.COLOR, "var(--lumo-primary-text-color)");

            if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                Div descLabel = new Div(product.getDescription());
                descLabel.getStyle()
                        .set(Css.FONT_SIZE, "var(--lumo-font-size-s)")
                        .set(Css.COLOR, "var(--lumo-secondary-text-color)")
                        .set("margin-top", "var(--lumo-space-xs)");
                productInfo.add(nameLabel, descLabel);
            } else {
                productInfo.add(nameLabel);
            }

            row.add(checkbox, productInfo);
            row.setFlexGrow(1, productInfo);
            productList.add(row);
        }
    }

    private void selectAll(boolean select) {
        List<ProductDto> filteredProducts = dataProvider.getItems()
                .stream()
                .collect(Collectors.toList());

        if (select) {
            for (ProductDto product : filteredProducts) {
                if (selectedProducts.stream().noneMatch(p -> p.getId() != null && p.getId().equals(product.getId()))) {
                    selectedProducts.add(product);
                }
            }
        } else {
            for (ProductDto product : filteredProducts) {
                selectedProducts.removeIf(p -> p.getId() != null && p.getId().equals(product.getId()));
            }
        }
        renderProductList();
        updateConfirmButton();
    }

    private void updateConfirmButton() {
        confirmButton.setText(Messages.get("action.confirmSelected", selectedProducts.size()));
        confirmButton.setEnabled(!selectedProducts.isEmpty());
    }
}
