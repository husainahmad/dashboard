package com.harmoni.menu.dashboard.layout.menu.category;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.ScopeService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Unified Category page: a brand selector, the category list on the left and
 * the selected category's details on the right, with create/edit in a modal
 * dialog.
 */
public class CategoryView extends VerticalLayout {

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    private final ComboBox<BrandDto> brandBox = new ComboBox<>(Messages.get(Messages.Keys.LABEL_BRAND));
    private final CategoryListCard listCard = new CategoryListCard();
    private final CategoryDetailCard detailCard = new CategoryDetailCard();
    private final Button addButton = new Button(Messages.get(Messages.Keys.ACTION_NEW_CATEGORY),
            event -> openDialog(null));

    private final List<BrandDto> brands = new ArrayList<>();
    private List<CategoryDto> categories = new ArrayList<>();
    private CategoryDto selectedCategory;
    private Integer pendingSelectId;
    private boolean brandsLoaded;

    public CategoryView(AsyncRestClientMenuService asyncRestClientMenuService,
                        AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                        RestClientMenuService restClientMenuService,
                        AccessService accessService) {
        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientMenuService = restClientMenuService;
        this.accessService = accessService;

        addClassName("category-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        PageHeader header = PageHeader.of("page.category.title", "page.category.description");

        brandBox.setItemLabelGenerator(BrandDto::getName);
        brandBox.setWidth("240px");
        brandBox.addValueChangeListener(event -> {
            if (event.isFromClient() && event.getValue() != null) {
                selectedCategory = null;
                fetchCategories();
            }
        });

        addButton.setIcon(VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.setEnabled(false);

        HorizontalLayout toolbar = new HorizontalLayout(brandBox, addButton);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.addClassName("category-toolbar");
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        HorizontalLayout body = new HorizontalLayout(listCard, detailCard);
        body.addClassName("category-body");
        body.setWidthFull();
        body.setSpacing(true);

        add(header, toolbar, body);
        setFlexGrow(1, body);

        listCard.setSelectionListener(this::selectCategory);
        detailCard.setEditListener(this::openDialog);
        detailCard.setDeleteListener(this::confirmDelete);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!brandsLoaded) {
            loadBrands();
        }
    }

    private void loadBrands() {
        asyncRestClientOrganizationService.getAllBrandAsync(
                result -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    brandsLoaded = true;
                    ScopeService scopeService = ScopeService.current();
                    ScopeService.ScopeSnapshot scope = scopeService == null ? null : scopeService.snapshot();
                    List<BrandDto> allowed = scope == null ? result : scope.filterBrands(result);
                    brands.clear();
                    if (allowed != null) {
                        brands.addAll(allowed);
                    }
                    brandBox.setItems(brands);
                    brandBox.setEnabled(scope == null ? brands.size() > 1
                            : scope.canSelectBrand(brands.size()));
                    addButton.setEnabled(!brands.isEmpty());
                    BrandDto selected = preferredBrand();
                    if (selected != null) {
                        brandBox.setValue(selected);
                        fetchCategories();
                    }
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_BRAND_LOAD_FAILED),
                                () -> {
                                    brandsLoaded = false;
                                    loadBrands();
                                })));
    }

    /**
     * Resolves the brand to preselect: the brand of the signed-in user's store.
     * Matching by id is tried first, then by name, because the user payload and
     * the brand list can come from different services. Only when neither matches
     * does it fall back to the first brand.
     *
     * @return the brand to select by default, or {@code null} when none loaded
     */
    private BrandDto preferredBrand() {
        if (brands.isEmpty()) {
            return null;
        }
        Integer sessionBrandId = accessService.getBrandId();
        if (sessionBrandId != null) {
            BrandDto match = brands.stream()
                    .filter(brand -> sessionBrandId.equals(brand.getId()))
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                return match;
            }
        }
        BrandDto loginBrand = accessService.getBrand();
        if (loginBrand != null && loginBrand.getName() != null) {
            BrandDto match = brands.stream()
                    .filter(brand -> loginBrand.getName().equalsIgnoreCase(brand.getName()))
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                return match;
            }
        }
        return brands.getFirst();
    }

    private void fetchCategories() {
        BrandDto brand = brandBox.getValue();
        if (brand == null || brand.getId() == null) {
            return;
        }
        listCard.setLoading(true);
        asyncRestClientMenuService.getAllCategoryAsync(
                result -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    listCard.setLoading(false);
                    categories = result == null ? new ArrayList<>() : new ArrayList<>(result);
                    listCard.setCategories(categories);

                    CategoryDto target = findSelected();
                    if (target == null && !categories.isEmpty()) {
                        target = categories.getFirst();
                    }
                    selectCategory(target);
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    listCard.setLoading(false);
                    UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_CATEGORY_LOAD_FAILED),
                            this::fetchCategories);
                }),
                brand.getId());
    }

    private CategoryDto findSelected() {
        Integer id = pendingSelectId != null ? pendingSelectId
                : selectedCategory == null ? null : selectedCategory.getId();
        if (id == null) {
            return null;
        }
        CategoryDto match = categories.stream()
                .filter(category -> Objects.equals(category.getId(), id))
                .findFirst()
                .orElse(null);
        if (match != null) {
            pendingSelectId = null;
        }
        return match;
    }

    private void selectCategory(CategoryDto category) {
        selectedCategory = category;
        listCard.select(category);
        detailCard.show(category, brandName(category));
    }

    private String brandName(CategoryDto category) {
        if (category != null && category.getBrandDto() != null
                && category.getBrandDto().getName() != null) {
            return category.getBrandDto().getName();
        }
        BrandDto brand = brandBox.getValue();
        return brand == null ? null : brand.getName();
    }

    private void openDialog(CategoryDto category) {
        new CategoryDialog(restClientMenuService, brands, brandBox.getValue(), category, () -> {
            pendingSelectId = category == null ? null : category.getId();
            fetchCategories();
        }).open();
    }

    private void confirmDelete(CategoryDto category) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(Messages.get(Messages.Keys.DIALOG_CONFIRM_TITLE));
        dialog.setText(Messages.get("dialog.confirmDeleteCategory",
                category.getName() == null ? "" : category.getName()));
        dialog.setCancelable(true);
        dialog.setConfirmText(Messages.get(Messages.Keys.ACTION_DELETE));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> restClientMenuService.deleteCategory(category).subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    UiUtil.success(Messages.get("category.deleted"));
                    selectedCategory = null;
                    fetchCategories();
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("category.deleteFailed")))));
        dialog.open();
    }
}
