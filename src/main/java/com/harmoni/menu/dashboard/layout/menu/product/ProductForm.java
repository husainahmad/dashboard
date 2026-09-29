package com.harmoni.menu.dashboard.layout.menu.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.event.product.ProductSaveEventListener;
import com.harmoni.menu.dashboard.event.product.ProductUpdateEventListener;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.menu.ProductFormLayout;
import com.harmoni.menu.dashboard.layout.util.AsyncUtil;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.StatusChangeEvent;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Objects;
import com.harmoni.menu.dashboard.layout.util.Css;

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
 * {@link ProductUpdateEventListener}. The constructor takes the two exchanged
 * services plus the {@link ProductEditorContext} the list view assembled, which is
 * where the brand, categories, tiers, tab and edited product all come from.
 * </p>
 *
 * <p>
 * When the tab is opened for an existing product, {@link #fetchProduct()} loads the
 * details and populates the fields; for a new product a single empty SKU row is
 * shown. {@link ProductEditorContext#isEditingExistingProduct()} is that test.
 * </p>
 */
@RequiredArgsConstructor
@Route(value = Css.PRODUCT_FORM, layout = MainLayout.class)
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

    Button saveButton = new Button(Messages.get(Messages.Keys.ACTION_SAVE));
    Button updateButton = new Button(Messages.get(Messages.Keys.ACTION_UPDATE));
    Button closeButton = new Button(Messages.get(Messages.Keys.ACTION_CANCEL));
    private final LoadingBar savingBar = new LoadingBar();
    private boolean saving;

    private final RestClientMenuService restClientMenuService;
    private final transient AsyncRestClientMenuService asyncRestClientMenuService;
    /**
     * The list view's state this editor was opened with: the brand, its categories and
     * tiers, the tab itself, and the manager that closes it.
     *
     * <p>{@code tabManager} is held rather than found by walking up from this
     * component: a {@code TabSheet} does not make its tab content a child of itself, so
     * the form's parent chain stops at {@link ProductFormWithPreview} and never reaches
     * the sheet.</p>
     */
    private final transient ProductEditorContext context;

    /** The product being edited; {@code null} until loaded for existing products. */
    @Getter
    transient ProductDto productDto;

    @Override
    public void onContentChanged() {
        firePreviewUpdate();
    }

    /** The preview kept in step with this form, or {@code null} when none is attached. */
    private ProductPreview productPreview;

    /**
     * Attaches the preview this form keeps in step with.
     *
     * <p>The form owns the repaint rather than handing the preview a callback, because
     * a preview of a product needs more than the product: the price matrix is built from
     * the live variant rows and the brand's tiers, both of which live here.</p>
     *
     * @param productPreview the preview to keep in step, or {@code null} to detach
     */
    public void setPreview(ProductPreview productPreview) {
        this.productPreview = productPreview;
        firePreviewUpdate();
    }

    /**
     * Hands the preview a detached copy of what the form currently shows.
     *
     * <p>The copy is deliberate. The fields are the source of truth for what is on
     * screen, while the bound {@link #productDto} lags behind an uncommitted edit, and
     * this must not write to the binder's own bean: the binder tracks dirty state
     * against it, and quietly refreshing it here would leave the Save button lit for a
     * product nobody has actually changed.</p>
     */
    public void firePreviewUpdate() {
        if (productPreview == null) {
            return;
        }
        ProductDto snapshot = new ProductDto();
        snapshot.setName(productNameField.getValue());
        snapshot.setDescription(productDescTextArea.getValue());
        snapshot.setCategoryDto(categoryBox.getValue());
        if (productImageUploadView != null && productImageUploadView.getProductImageDto() != null) {
            snapshot.setProductImageDto(productImageUploadView.getProductImageDto());
        }
        productPreview.updatePreview(snapshot, skuSection == null ? List.of() : skuSection.skuItems(),
                context.tierDtos(),
                customizationSection == null ? List.of() : customizationSection.productCustomizations());
    }

    /**
     * Creates an uppercase section caption used as a form section header.
     *
     * @param text the caption text
     * @return the caption span
     */
    private Span sectionCaption(String text) {
        Span caption = new Span(text.toUpperCase());
        caption.addClassName("section-caption");
        return caption;
    }

    /**
     * Builds the editor layout: category select, name and description fields,
     * image upload, the {@link SkuSection} and {@link CustomizationSection}
     * grids, then wires validation and the action buttons.
     */
    private void renderLayout() {
        // Exactly one action, as this form's own contract says: Update for a product
        // that exists, Save for a new one. They used to both stay visible, and they are
        // not interchangeable - create answers 201, update answers 200, and each
        // listener treats the other's status as a failure. So Save on an existing
        // product posted to the create endpoint with an id, and if the backend declined
        // it the listener stopped the progress bar and did nothing else at all: no
        // close, no refresh, no message to say why.
        applyActionFor(context.isEditingExistingProduct());

        categoryBox.setLabel(Messages.get(Messages.Keys.LABEL_CATEGORY));
        categoryBox.setItems(context.categoryDtos());
        if (ObjectUtils.isNotEmpty(context.categoryDtos())) {
            categoryBox.setValue(context.categoryDtos().getLast());
        }
        categoryBox.setItemLabelGenerator(CategoryDto::getName);
        categoryBox.setWidth("100%");

        productNameField.setLabel(Messages.get("label.field.productName"));
        productNameField.setPlaceholder(Messages.get("placeholder.productName"));
        productNameField.setClearButtonVisible(true);
        productNameField.setValueChangeMode(ValueChangeMode.LAZY);
        productNameField.setWidth("100%");

        productDescTextArea.setLabel(Messages.get(Messages.Keys.LABEL_DESCRIPTION));
        productDescTextArea.setPlaceholder(Messages.get("placeholder.productDescription"));
        productDescTextArea.setValueChangeMode(ValueChangeMode.LAZY);
        productDescTextArea.setWidth("100%");
        productDescTextArea.getElement().setProperty("rows", 3);

        productImageUploadView = new ProductImageUploadView(restClientMenuService, getUi(),
                context.productTreeItem());
        // The picture is uploaded and cropped before the product is saved, so the
        // preview can show it straight away; without this it would only ever appear
        // after a reload.
        productImageUploadView.setOnImageChanged(this::firePreviewUpdate);

        skuSection = new SkuSection(context.tierDtos(), this);
        skuSection.addSku(null);
        customizationSection = new CustomizationSection(restClientMenuService, asyncRestClientMenuService,
                context.brandDto(), context.tierDtos(), skuSection, this);

        add(savingBar, 2);
        add(sectionCaption(Messages.get("section.productInfo")), 2);
        add(categoryBox, productNameField);
        add(productDescTextArea, productImageUploadView);

        setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("760px", 2));

        add(buildSkuCustomizationTabs(), 2);
        add(getButtonBar(), 2);

        productNameField.addValueChangeListener(change -> firePreviewUpdate());
        productDescTextArea.addValueChangeListener(change -> firePreviewUpdate());
        categoryBox.addValueChangeListener(change -> firePreviewUpdate());

        addValidation();
        bindButtonState();
    }

    /**
     * Groups the {@link SkuSection} and {@link CustomizationSection} editors into
     * a nested {@link TabSheet} so the form scrolls less and each editor gets the
     * full row width.
     *
     * <p>
     * The sheet is intentionally left to size itself instead of filling the form:
     * the form is its own scroll container (absolutely positioned in the outer
     * product tab), so a {@code sizeFull} sheet would pin the action bar out of
     * reach. Panel padding is dropped because the form already provides it, and
     * each grid keeps its existing {@code .content} wrapper so the height caps in
     * {@code product-form.css} still apply.
     * </p>
     *
     * @return the SKU / customization tab sheet
     */
    private TabSheet buildSkuCustomizationTabs() {
        TabSheet tabSheet = new TabSheet();
        tabSheet.setWidthFull();

        Tab skuTab = new Tab(Messages.get("section.skuPricing"));
        Div skuPanel = new Div(getToolbar(skuSection.getToolbar()), getContent(skuSection.getGrid()));
        tabSheet.add(skuTab, skuPanel);

        Tab customizationTab = new Tab(Messages.get("section.customization"));
        Div customizationPanel = new Div(customizationSection.getLayout());
        tabSheet.add(customizationTab, customizationPanel);

        return tabSheet;
    }

    /**
     * Builds the editor, seeds it from the clicked {@link ProductTreeItem} when
     * present, loads the product details asynchronously and focuses the name
     * field.
     *
     * @param attachEvent the attach event of this view
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        renderLayout();
        applyTreeItem();
        fetchProduct();
        productNameField.focus();
    }

    /**
     * Loads the product details for an existing product and populates the form
     * via {@link #applyProduct(RestAPIResponse)}; a no-op when creating a new
     * product.
     */
    private void fetchProduct() {
        if (!context.isEditingExistingProduct()) {
            return;
        }
        AsyncUtil.subscribe(restClientMenuService.getProduct(context.productTreeItem().getProductId()),
                getUi(), Messages.get(Messages.Keys.NOTIFICATION_PRODUCT_LOAD_FAILED),
                this::applyProduct);
    }

    /**
     * Seeds the editor straight from the list's {@link ProductTreeItem}, which
     * already carries the name, category id and SKUs. This guarantees the form
     * is correctly populated even before (or when) the detail endpoint returns
     * an empty or malformed payload.
     */
    private void applyTreeItem() {
        ProductTreeItem productTreeItem = context.productTreeItem();
        if (ObjectUtils.isEmpty(productTreeItem)) {
            return;
        }
        if (ObjectUtils.isEmpty(productDto)) {
            productDto = new ProductDto();
        }
        productDto.setId(productTreeItem.getProductId());
        productDto.setCategoryId(productTreeItem.getCategoryId());
        productDto.setName(productTreeItem.getName());
        productDto.setSkuDtos(productTreeItem.getSkus());

        if (ObjectUtils.isNotEmpty(productTreeItem.getCategoryId())) {
            context.categoryDtos().stream()
                    .filter(c -> Objects.equals(c.getId(), productTreeItem.getCategoryId()))
                    .findFirst()
                    .ifPresent(categoryBox::setValue);
        }

        productNameField.setValue(productTreeItem.getName() == null ? "" : productTreeItem.getName());
        productDescTextArea.setValue(productDto.getDescription() == null ? "" : productDto.getDescription());
        skuSection.load(productDto.getSkuDtos());
        firePreviewUpdate();
        updateButtonStates();
        if (ObjectUtils.isNotEmpty(productDto.getId())) {
            customizationSection.load(productDto.getId());
        }
    }

    /**
     * Merges the detail response into the editor. Non-null fields from the
     * response override the values seeded from {@link #applyTreeItem()}; fields
     * the detail response omits keep whatever was already populated so a partial
     * payload can never wipe name, category, SKUs or image.
     */
    private void applyProduct(RestAPIResponse restAPIResponse) {
        if (ObjectUtils.isEmpty(restAPIResponse.getData())) {
            return;
        }
        ProductDto fetched = ObjectUtil.convertObjectToObject(restAPIResponse.getData(), new TypeReference<>() {
        });
        if (ObjectUtils.isEmpty(productDto)) {
            productDto = new ProductDto();
        }

        if (ObjectUtils.isNotEmpty(fetched.getId())) {
            productDto.setId(fetched.getId());
        }
        if (ObjectUtils.isNotEmpty(fetched.getCategoryId())) {
            productDto.setCategoryId(fetched.getCategoryId());
        }
        if (ObjectUtils.isNotEmpty(fetched.getName())) {
            productDto.setName(fetched.getName());
            productNameField.setValue(fetched.getName());
        }
        if (fetched.getDescription() != null) {
            productDto.setDescription(fetched.getDescription());
            productDescTextArea.setValue(fetched.getDescription());
        }
        if (ObjectUtils.isNotEmpty(fetched.getCategoryDto())) {
            productDto.setCategoryDto(fetched.getCategoryDto());
            if (ObjectUtils.isNotEmpty(fetched.getCategoryDto().getId())) {
                context.categoryDtos().stream()
                        .filter(c -> Objects.equals(c.getId(), fetched.getCategoryDto().getId()))
                        .findFirst()
                        .ifPresent(categoryBox::setValue);
            }
        }
        if (ObjectUtils.isNotEmpty(fetched.getSkuDtos())) {
            productDto.setSkuDtos(fetched.getSkuDtos());
            skuSection.load(fetched.getSkuDtos());
        }
        if (ObjectUtils.isNotEmpty(fetched.getProductImageDto())) {
            productDto.setProductImageDto(fetched.getProductImageDto());
            refreshImage();
        }

        updateButtonStates();
        if (ObjectUtils.isNotEmpty(productDto.getId())) {
            customizationSection.load(productDto.getId());
        }
    }

    /**
     * Shows the product image preview from the loaded product details.
     */
    private void refreshImage() {
        if (ObjectUtils.isNotEmpty(productDto.getProductImageDto()) &&
                ObjectUtils.isNotEmpty(productDto.getProductImageDto().getUrl())) {
            this.productImageUploadView.setImage(productDto.getProductImageDto().getUrl());
        }
    }

    /**
     * Closes the editor tab and returns to the list tab. Called after a successful
     * save as well as on cancel.
     *
     * <p>This is what the save listener reaches after a 201. It used to resolve the tab
     * sheet from this component's own parent, which never worked: a tab sheet does not
     * make its tab content a child of itself, so there was no parent to match and the
     * close was a no-op. A product saved with a 201 therefore left its tab sitting open
     * with nothing to suggest the save had worked. The sheet is now held directly.</p>
     *
     * <p>Closes through {@link TabManager} so the list tab is selected again, matching
     * how the promotion and customization editors close themselves.</p>
     */
    public void removeFromSheet() {
        UiUtil.safeAccess(getUi(), this::closeEditorTab);
    }

    /**
     * The close itself, separated from {@link #removeFromSheet()} so it can be exercised
     * without a UI: {@code safeAccess} returns immediately when there is no session, so
     * anything it guards is untestable as written.
     */
    void closeEditorTab() {
        TabManager tabManager = context.tabManager();
        if (tabManager == null || context.productTab() == null) {
            return;
        }
        tabManager.closeAndSelectFirst(context.productTab());
    }

    /**
     * Binds the category and name validators to the {@link #binder}. The
     * validators are null-safe so a missing selection or value can never crash
     * the validation pass.
     */
    private void addValidation() {
        binder.forField(categoryBox)
                .withValidator(value -> value == null || (value.getId() != null && value.getId() > 0),
                        Messages.get("validation.category.required"))
                .bind(ProductDto::getCategoryDto, ProductDto::setCategoryDto);
        binder.forField(productNameField)
                .withValidator(value -> value != null && value.length() > 2,
                        Messages.get(Messages.Keys.VALIDATION_NAME_MIN_LENGTH))
                .bind(ProductDto::getName, ProductDto::setName);
    }

    /**
     * Subscribes to binder status changes so the save/update buttons mirror the
     * current validation result, and applies the initial button state.
     *
     * <p>
     * The {@link StatusChangeEvent} listener is the single place that reads the
     * validation outcome; it must never call {@code binder.validate()} itself,
     * as that would fire another status change and recurse until a
     * {@link StackOverflowError}.
     * </p>
     */
    private void bindButtonState() {
        binder.addStatusChangeListener(this::updateButtonStates);
        updateButtonStates();
    }

    /**
     * Consumes a {@link StatusChangeEvent} and mirrors its validation outcome
     * onto the save/update buttons.
     *
     * @param event the validation status change, carrying the outcome
     */
    /**
     * Shows Update for a product that already exists and Save for a new one.
     *
     * @param existing whether the product is already stored
     */
    private void applyActionFor(boolean existing) {
        saveButton.setVisible(!existing);
        updateButton.setVisible(existing);
    }

    /**
     * Updates the save/update buttons to reflect the current validation state.
     *
     * <p>
     * This is the only place that reads the validation outcome; it must never
     * call {@code binder.validate()} itself, as that would fire another status
     * change and recurse until a {@link StackOverflowError}.
     * </p>
     *
     * @param event the validation status change, carrying the outcome
     */
    private void updateButtonStates(StatusChangeEvent event) {
        if (saving) {
            return;
        }
        boolean valid = !event.hasValidationErrors();
        saveButton.setEnabled(valid);
        updateButton.setEnabled(valid);
    }

    /**
     * Requests a fresh validation pass. The resulting status change is handled
     * by {@link #updateButtonStates(StatusChangeEvent)}, which performs the
     * actual button state updates.
     */
    private void updateButtonStates() {
        if (saving) {
            return;
        }
        binder.validate();
    }

    /**
     * Assembles the action bar: Update for existing products, Save for new
     * ones, plus the Cancel button.
     *
     * @return the footer toolbar
     */
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

        HorizontalLayout toolbar = new HorizontalLayout(
                (context.isEditingExistingProduct() ? updateButton : saveButton), closeButton);
        toolbar.addClassName(Css.TOOLBAR);
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
            UiUtil.safeAccess(ui, () -> {
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
            UiUtil.safeAccess(ui, () -> {
                saving = false;
                savingBar.stop();
                updateButtonStates();
            });
        }
    }

    /**
     * Closes this editor tab when the cancel button is clicked.
     *
     * @param buttonClickEvent the click event from the close button
     */
    private void onButtonClose(ClickEvent<Button> buttonClickEvent) {
        removeFromSheet();
    }

    @Override
    public Integer getProductId() {
        return productDto != null ? productDto.getId() : null;
    }
}