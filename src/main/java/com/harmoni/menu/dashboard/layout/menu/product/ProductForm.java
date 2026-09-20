package com.harmoni.menu.dashboard.layout.menu.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.event.product.ProductSaveEventListener;
import com.harmoni.menu.dashboard.event.product.ProductUpdateEventListener;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.menu.ProductFormLayout;
import com.harmoni.menu.dashboard.layout.util.AsyncUtil;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ImageUtil;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;

/**
 * Thin orchestrator for the product editor tab.
 *
 * <p>
 * This view only wires the shared pieces together (binder, category / naming
 * fields, image upload, the save/update button bar) and delegates the real
 * editing to its sections:
 * </p>
 *
 * <ul>
 *     <li>{@link SkuSection} — SKU rows and per-tier prices</li>
 *     <li>{@link CustomizationSection} — customization attachments and overrides</li>
 * </ul>
 *
 * <p>
 * Persistence is handled by {@link ProductSaveEventListener} and
 * {@link ProductUpdateEventListener}; the associations to exchanged services,
 * brand and tiers are injected through the constructor.
 * </p>
 *
 * <p>
 * When the tab is opened for an existing product (a non-null
 * {@link ProductTreeItem}), {@link #fetchProduct()} loads the details and
 * populates the fields; for a new product a single empty SKU row is shown.
 * </p>
 */
@RequiredArgsConstructor
@Route(value = "product-form", layout = MainLayout.class)
@Slf4j
public class ProductForm extends ProductFormLayout implements ProductFormDelegate {

    /** Binder driving validation for the whole form; button state follows its status. */
    @Getter
    BeanValidationBinder<ProductDto> binder = new BeanValidationBinder<>(ProductDto.class);
    /** Product display name. */
    @Getter
    TextField productNameField = new TextField();
    /** Free-text product description. */
    @Getter
    TextArea productDescTextArea = new TextArea();
    /** Category selection populated from {@link #categoryDtos}. */
    @Getter
    ComboBox<CategoryDto> categoryBox = new ComboBox<>();
    /** Image upload / preview tile. */
    @Getter
    ProductImageUploadView productImageUploadView;
    /** Editor for the SKU and tier-price grid. */
    @Getter
    SkuSection skuSection;
    /** Editor for the customization attachments. */
    @Getter
    CustomizationSection customizationSection;

    Button saveButton = new Button("Save");
    Button updateButton = new Button("Update");
    Button closeButton = new Button("Cancel");
    private final LoadingBar savingBar = new LoadingBar();
    private boolean saving;

    private final RestClientMenuService restClientMenuService;
    private final transient AsyncRestClientMenuService asyncRestClientMenuService;
    private final transient BrandDto brandDto;
    private final transient List<CategoryDto> categoryDtos;
    private final transient List<TierDto> tierDtos;
    private final Tab productTab;
    private final transient ProductTreeItem productTreeItem;

    /** The product being edited; {@code null} until loaded for existing products. */
    @Getter
    transient ProductDto productDto;

    private Span sectionCaption(String text) {
        Span caption = new Span(text.toUpperCase());
        caption.addClassName("section-caption");
        return caption;
    }

    private void renderLayout() {
        categoryBox.setLabel("Category");
        categoryBox.setItems(categoryDtos);
        categoryBox.setValue(categoryDtos.getLast());
        categoryBox.setItemLabelGenerator(CategoryDto::getName);
        categoryBox.setWidth("100%");

        productNameField.setLabel("Product name");
        productNameField.setPlaceholder("Enter Product name...");
        productNameField.setClearButtonVisible(true);
        productNameField.setValueChangeMode(ValueChangeMode.LAZY);
        productNameField.setWidth("100%");

        productDescTextArea.setLabel("Description");
        productDescTextArea.setPlaceholder("Enter Description");
        productDescTextArea.setValueChangeMode(ValueChangeMode.LAZY);
        productDescTextArea.setWidth("100%");
        productDescTextArea.getElement().setProperty("rows", 3);

        productImageUploadView = new ProductImageUploadView(restClientMenuService, getUi(), productTreeItem);

        skuSection = new SkuSection(tierDtos, this);
        skuSection.addSku(null);
        customizationSection = new CustomizationSection(restClientMenuService, asyncRestClientMenuService,
                brandDto, tierDtos, skuSection, this);

        add(savingBar, 2);
        add(sectionCaption("Product Info"), 2);
        add(categoryBox, productNameField);
        add(productDescTextArea, productImageUploadView);

        setSizeFull();
        setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("760px", 2));

        add(sectionCaption("SKU & Pricing"), 2);
        add(skuSection.getToolbar(), 2);
        add(getContent(skuSection.getGrid()), 2);
        add(customizationSection.getLayout(), 2);
        add(getButtonBar(), 2);

        addValidation();
        bindButtonState();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        renderLayout();
        fetchProduct();
        productNameField.focus();
    }

    private void fetchProduct() {
        if (ObjectUtils.isEmpty(productTreeItem)) {
            return;
        }
        AsyncUtil.subscribe(restClientMenuService.getProduct(productTreeItem.getProductId()),
                getUi(), "Failed to load product",
                this::applyProduct);
    }

    private void applyProduct(RestAPIResponse restAPIResponse) {
        if (ObjectUtils.isEmpty(restAPIResponse.getData())) {
            return;
        }
        productDto = ObjectUtil.convertObjectToObject(restAPIResponse.getData(), new TypeReference<>() {
        });
        categoryBox.setValue(productDto.getCategoryDto());
        productNameField.setValue(productDto.getName());
        productDescTextArea.setValue(productDto.getDescription() == null ? "" : productDto.getDescription());
        skuSection.load(productDto.getSkuDtos());
        refreshImage();
        binder.validate();
        updateButtonStates();
        if (ObjectUtils.isNotEmpty(productDto.getId())) {
            customizationSection.load(productDto.getId());
        }
    }

    private void refreshImage() {
        if (ObjectUtils.isNotEmpty(productDto.getProductImageDto()) &&
                ObjectUtils.isNotEmpty(productDto.getProductImageDto().getImageBlob())) {
            this.productImageUploadView.setImage(ImageUtil.createStreamResource(
                    productDto.getProductImageDto().getImageBlob(),
                    productDto.getProductImageDto().getFileName()));
        }
    }

    /**
     * Closes the editor tab by removing this form's tab from the parent
     * {@link TabSheet}. Called after a successful save as well as on cancel.
     */
    public void removeFromSheet() {
        getUi().access(() -> {
            if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
                return;
            }
            tabSheet.remove(productTab);
        });
    }

    private void addValidation() {
        binder.forField(categoryBox)
                .withValidator(value -> (value == null || value.getId() > 0), "Category not allow to be empty")
                .bind(ProductDto::getCategoryDto, ProductDto::setCategoryDto);
        binder.forField(productNameField)
                .withValidator(value -> value.length() > 2,
                        "Name must contain at least three characters")
                .bind(ProductDto::getName, ProductDto::setName);
    }

    private void bindButtonState() {
        binder.addStatusChangeListener(event -> updateButtonStates());
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (saving) {
            return;
        }
        boolean valid = binder.validate().isOk();
        saveButton.setEnabled(valid);
        updateButton.setEnabled(valid);
    }

    private HorizontalLayout getButtonBar() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.setIcon(new Icon(VaadinIcon.CHECK));
        updateButton.setIcon(new Icon(VaadinIcon.REFRESH));
        closeButton.setIcon(new Icon(VaadinIcon.CLOSE_SMALL));

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);
        closeButton.addClickShortcut(Key.ESCAPE);

        updateButton.addClickListener(new ProductUpdateEventListener(this, restClientMenuService));
        saveButton.addClickListener(new ProductSaveEventListener(this, restClientMenuService));
        closeButton.addClickListener(this::onButtonClose);

        HorizontalLayout toolbar = new HorizontalLayout((this.productTreeItem != null ? updateButton : saveButton), closeButton);
        toolbar.addClassName("toolbar");
        toolbar.addClassName("form-actions");
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        return toolbar;
    }

    /**
     * Invoked by the save/update listeners right before the REST call: disables
     * the action buttons and shows the loading bar.
     */
    public void onSaveStart() {
        saving = true;
        UI ui = getUi();
        if (ui != null) {
            ui.access(() -> {
                saveButton.setEnabled(false);
                updateButton.setEnabled(false);
                savingBar.start();
            });
        }
    }

    /**
     * Invoked by the save/update listeners when the REST call finishes (success
     * or failure) without closing the tab: re-enables the actions and stops the
     * loading bar. Also called by the error path.
     */
    public void onSaveEnd() {
        UI ui = getUi();
        if (ui != null) {
            ui.access(() -> {
                saving = false;
                savingBar.stop();
                updateButtonStates();
            });
        }
    }

    private void onButtonClose(ClickEvent<Button> buttonClickEvent) {
        removeFromSheet();
    }

    @Override
    public Integer getProductId() {
        return productDto != null ? productDto.getId() : null;
    }
}