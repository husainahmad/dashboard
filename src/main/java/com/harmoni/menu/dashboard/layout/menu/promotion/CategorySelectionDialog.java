package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CategorySelectionDialog extends Dialog {

    private final AsyncRestClientMenuService menuService;
    private final Integer brandId;
    private final List<CategoryDto> selectedCategories = new ArrayList<>();
    private final Consumer<List<CategoryDto>> onConfirm;
    private final ListDataProvider<CategoryDto> dataProvider;

    private final TextField searchField = new TextField();
    private final VerticalLayout categoryList = new VerticalLayout();
    private final Div loadingIndicator = new Div();
    private final Button confirmButton = new Button();
    private final Button cancelButton = new Button();
    private final Button selectAllButton = new Button();
    private final Button clearAllButton = new Button();

    private List<CategoryDto> allCategories = new ArrayList<>();

    private boolean initialLoadStarted;

    public CategorySelectionDialog(AsyncRestClientMenuService menuService,
                                   Integer brandId,
                                   List<CategoryDto> preSelectedCategories,
                                   Consumer<List<CategoryDto>> onConfirm) {
        this.menuService = menuService;
        this.brandId = brandId;
        this.onConfirm = onConfirm;

        if (preSelectedCategories != null) {
            this.selectedCategories.addAll(preSelectedCategories);
        }

        setHeaderTitle(Messages.get("dialog.categorySelection.title"));
        setWidth("500px");
        setMaxWidth("90vw");
        setHeight("60vh");
        setDraggable(true);
        setModal(true);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);

        dataProvider = new ListDataProvider<>(allCategories);

        buildContent();
    }

    /**
     * Defers the first load until the dialog is attached to a UI. Loading from the
     * constructor would run while {@code getUI()} is still empty, so
     * {@link UiUtil#safeAccess} would drop both the success and the error callback
     * and leave the list spinning forever.
     */
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

        searchField.setPlaceholder(Messages.get("dialog.categorySelection.searchPlaceholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();
        searchField.addValueChangeListener(e -> filterCategories(e.getValue()));
        searchLayout.add(searchField);

        // Toolbar
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(true);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);

        selectAllButton.setText(Messages.get("dialog.categorySelection.selectAll"));
        selectAllButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        selectAllButton.addClickListener(e -> selectAll(true));

        clearAllButton.setText(Messages.get("dialog.categorySelection.clearAll"));
        clearAllButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        clearAllButton.addClickListener(e -> selectAll(false));

        toolbar.add(selectAllButton, clearAllButton);
        toolbar.setFlexGrow(1, selectAllButton);

        // Category list with loading
        categoryList.setSizeFull();
        categoryList.setPadding(false);
        categoryList.setSpacing(false);

        loadingIndicator.setText(Messages.get("dialog.categorySelection.loading"));
        loadingIndicator.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("height", "200px")
                .set("color", "var(--lumo-secondary-text-color)");
        loadingIndicator.setVisible(false);

        VerticalLayout listContainer = new VerticalLayout(categoryList, loadingIndicator);
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
        footer.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");

        cancelButton.setText(Messages.get("action.cancel"));
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> close());

        confirmButton.setText(Messages.get("action.confirm"));
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirmButton.addClickListener(e -> {
            onConfirm.accept(new ArrayList<>(selectedCategories));
            close();
        });
        updateConfirmButton();

        footer.add(cancelButton, confirmButton);

        dialogLayout.add(searchLayout, toolbar, listContainer, footer);
        dialogLayout.setFlexGrow(1, listContainer);
        add(dialogLayout);
    }

    private void loadCategories() {
        loadingIndicator.setVisible(true);
        categoryList.removeAll();

        menuService.getAllCategoryAsync(
                categories -> {
                    UiUtil.safeAccess(getUI().orElse(null), () -> {
                        loadingIndicator.setVisible(false);
                        allCategories = categories != null ? categories : new ArrayList<>();
                        dataProvider.getItems().clear();
                        dataProvider.getItems().addAll(allCategories);
                        renderCategoryList();
                    });
                },
                error -> {
                    UiUtil.safeAccess(getUI().orElse(null), () -> {
                        loadingIndicator.setVisible(false);
                        UiUtil.error(Messages.get("dialog.categorySelection.loadFailed") + ": " + error.getMessage());
                    });
                },
                brandId
        );
    }

    private void filterCategories(String searchText) {
        String filter = searchText != null ? searchText.toLowerCase() : "";
        dataProvider.setFilter(category -> {
            if (filter.isEmpty()) return true;
            String name = category.getName() != null ? category.getName().toLowerCase() : "";
            String desc = category.getDescription() != null ? category.getDescription().toLowerCase() : "";
            return name.contains(filter) || desc.contains(filter);
        });
        renderCategoryList();
    }

    private void renderCategoryList() {
        categoryList.removeAll();

        List<CategoryDto> filteredCategories = dataProvider.getItems()
                .stream()
                .collect(Collectors.toList());

        if (filteredCategories.isEmpty()) {
            Div emptyState = new Div();
            emptyState.setText(Messages.get("dialog.categorySelection.empty"));
            emptyState.getStyle()
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("justify-content", "center")
                    .set("height", "200px")
                    .set("color", "var(--lumo-tertiary-text-color)");
            categoryList.add(emptyState);
            return;
        }

        for (CategoryDto category : filteredCategories) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setSpacing(true);
            row.setPadding(true);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.getStyle()
                    .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                    .set("transition", "background-color 0.15s");

            row.getElement().addEventListener("mouseenter", e ->
                    row.getStyle().set("background-color", "var(--lumo-contrast-5pct)"));
            row.getElement().addEventListener("mouseleave", e ->
                    row.getStyle().set("background-color", "transparent"));

            Checkbox checkbox = new Checkbox();
            checkbox.setValue(selectedCategories.stream()
                    .anyMatch(c -> c.getId() != null && c.getId().equals(category.getId())));
            checkbox.addValueChangeListener(e -> {
                if (e.getValue()) {
                    if (selectedCategories.stream().noneMatch(c -> c.getId() != null && c.getId().equals(category.getId()))) {
                        selectedCategories.add(category);
                    }
                } else {
                    selectedCategories.removeIf(c -> c.getId() != null && c.getId().equals(category.getId()));
                }
                updateConfirmButton();
            });

            Div categoryInfo = new Div();
            categoryInfo.setWidthFull();

            H4 nameLabel = new H4(category.getName());
            nameLabel.getStyle()
                    .set("margin", "0")
                    .set("font-size", "var(--lumo-font-size-m)")
                    .set("font-weight", "500")
                    .set("color", "var(--lumo-primary-text-color)");

            if (category.getDescription() != null && !category.getDescription().isEmpty()) {
                Div descLabel = new Div(category.getDescription());
                descLabel.getStyle()
                        .set("font-size", "var(--lumo-font-size-s)")
                        .set("color", "var(--lumo-secondary-text-color)")
                        .set("margin-top", "var(--lumo-space-xs)");
                categoryInfo.add(nameLabel, descLabel);
            } else {
                categoryInfo.add(nameLabel);
            }

            row.add(checkbox, categoryInfo);
            row.setFlexGrow(1, categoryInfo);
            categoryList.add(row);
        }
    }

    private void selectAll(boolean select) {
        List<CategoryDto> filteredCategories = dataProvider.getItems()
                .stream()
                .collect(Collectors.toList());

        if (select) {
            for (CategoryDto category : filteredCategories) {
                if (selectedCategories.stream().noneMatch(c -> c.getId() != null && c.getId().equals(category.getId()))) {
                    selectedCategories.add(category);
                }
            }
        } else {
            for (CategoryDto category : filteredCategories) {
                selectedCategories.removeIf(c -> c.getId() != null && c.getId().equals(category.getId()));
            }
        }
        renderCategoryList();
        updateConfirmButton();
    }

    private void updateConfirmButton() {
        confirmButton.setText(Messages.get("action.confirmSelected", selectedCategories.size()));
        confirmButton.setEnabled(!selectedCategories.isEmpty());
    }
}
