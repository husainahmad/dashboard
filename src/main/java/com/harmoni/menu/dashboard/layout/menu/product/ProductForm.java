package com.harmoni.menu.dashboard.layout.menu.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.event.product.ProductPersistEventListener;
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
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Objects;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Orchestrator for the product editor tab: wires the binder, category/name
 * fields, image upload and action bar, and delegates editing to
 * {@link SkuSection} (SKU rows and tier prices) and
 * {@link CustomizationSection} (attachments and overrides).
 *
 * <p>Persistence is handled by {@link ProductPersistEventListener}. For an
 * existing product {@link #fetchProduct()} loads the details; for a new one a
 * single empty SKU row is shown.</p>
 */
@RequiredArgsConstructor
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

    Button submitButton = new Button();
    Button closeButton = new Button(Messages.get(Messages.Keys.ACTION_CANCEL));
    private final Span dirtyLabel = new Span(Messages.get("product.unsaved"));
    private final LoadingBar savingBar = new LoadingBar();
    private boolean saving;
    private boolean layoutBuilt;
    private boolean dirty;
    private boolean loading;
    private int loadGeneration;

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
        markDirty();
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
        if (layoutBuilt) {
            return;
        }
        layoutBuilt = true;
        loading = true;

        if (context != null && context.tabManager() != null) {
            context.tabManager().setCloseGuard(context.productTab(), tab -> {
                if (!dirty) {
                    return true;
                }
                requestClose();
                return false;
            });
        }

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
        productImageUploadView.setOnImageChanged(this::onImageChanged);

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

        productNameField.addValueChangeListener(change -> {
            firePreviewUpdate();
            markDirty();
        });
        productDescTextArea.addValueChangeListener(change -> {
            firePreviewUpdate();
            markDirty();
        });
        categoryBox.addValueChangeListener(change -> {
            firePreviewUpdate();
            markDirty();
        });
        binder.addValueChangeListener(event -> markDirty());

        addValidation();
        bindButtonState();
        loading = false;
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
        ProductDto target = ensureProductDto();
        target.setId(productTreeItem.getProductId());
        target.setCategoryId(productTreeItem.getCategoryId());
        target.setName(productTreeItem.getName());
        target.setSkuDtos(productTreeItem.getSkus());
        repaint();
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
        ProductDto target = ensureProductDto();

        if (ObjectUtils.isNotEmpty(fetched.getId())) {
            target.setId(fetched.getId());
        }
        if (ObjectUtils.isNotEmpty(fetched.getCategoryId())) {
            target.setCategoryId(fetched.getCategoryId());
        }
        if (ObjectUtils.isNotEmpty(fetched.getCategoryDto())) {
            target.setCategoryDto(fetched.getCategoryDto());
        }
        if (ObjectUtils.isNotEmpty(fetched.getName())) {
            target.setName(fetched.getName());
        }
        if (fetched.getDescription() != null) {
            target.setDescription(fetched.getDescription());
        }
        if (ObjectUtils.isNotEmpty(fetched.getSkuDtos())) {
            target.setSkuDtos(fetched.getSkuDtos());
        }
        if (ObjectUtils.isNotEmpty(fetched.getProductImageDto())) {
            target.setProductImageDto(fetched.getProductImageDto());
        }
        repaint();
    }

    /**
     * Returns the product bean, creating it on first use and handing it to the
     * binder so name and category edits are written through to it.
     */
    private ProductDto ensureProductDto() {
        if (productDto == null) {
            productDto = new ProductDto();
        }
        binder.setBean(productDto);
        return productDto;
    }

    /**
     * Pushes the product bean onto the widgets and repaints the preview. Both
     * the tree seed and the detail response end here, so the two load paths
     * cannot drift apart.
     */
    private void repaint() {
        int generation = ++loadGeneration;
        loading = true;
        resolveCategoryDto();
        binder.readBean(productDto);
        productDescTextArea.setValue(productDto.getDescription() == null ? "" : productDto.getDescription());
        skuSection.load(productDto.getSkuDtos());
        refreshImage();
        firePreviewUpdate();
        validateForm();
        if (ObjectUtils.isNotEmpty(productDto.getId())) {
            customizationSection.load(productDto.getId(), () -> {
                if (generation == loadGeneration) {
                    loading = false;
                }
            });
        } else {
            loading = false;
        }
    }

    /**
     * Makes sure the bean's category matches the category the widget will show.
     * The tree seed only carries a category id, so the display object is looked
     * up from the categories handed over by the list.
     */
    private void resolveCategoryDto() {
        if (productDto.getCategoryId() == null) {
            return;
        }
        if (productDto.getCategoryDto() != null
                && Objects.equals(productDto.getCategoryDto().getId(), productDto.getCategoryId())) {
            return;
        }
        context.categoryDtos().stream()
                .filter(category -> Objects.equals(category.getId(), productDto.getCategoryId()))
                .findFirst()
                .ifPresent(productDto::setCategoryDto);
    }

    /**
     * Shows the product image preview from the loaded product details.
     */
    private void refreshImage() {
        if (ObjectUtils.isNotEmpty(productDto.getProductImageDto()) &&
                ObjectUtils.isNotEmpty(productDto.getProductImageDto().getUrl())) {
            this.productImageUploadView.setImage(productDto.getProductImageDto());
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
     * Subscribes to binder status changes so the action buttons mirror the
     * current validation result, and applies the initial state.
     */
    private void bindButtonState() {
        binder.addStatusChangeListener(this::onBinderStatusChange);
        validateForm();
    }

    /**
     * Turns the single submit button into either Update (existing product) or
     * Save (new product). The two actions answer different endpoints and treat
     * each other's status as a failure, so only one may ever be wired.
     *
     * @param existing whether the product is already stored
     */
    private void applyActionFor(boolean existing) {
        submitButton.setText(Messages.get(existing ? Messages.Keys.ACTION_UPDATE : Messages.Keys.ACTION_SAVE));
        submitButton.setIcon(new Icon(existing ? VaadinIcon.REFRESH : VaadinIcon.CHECK));
        submitButton.addClickListener(new ProductPersistEventListener(this, restClientMenuService, existing));
    }

    private void onBinderStatusChange(StatusChangeEvent event) {
        applyValidation(event.hasValidationErrors());
    }

    /**
     * Runs a validation pass and mirrors the result onto the buttons. Unlike the
     * status listener this is safe to call directly: it does not itself listen
     * for the status change it triggers.
     */
    private void validateForm() {
        applyValidation(binder.validate().hasErrors());
    }

    private void applyValidation(boolean hasErrors) {
        if (saving) {
            return;
        }
        submitButton.setEnabled(!hasErrors && dirty);
    }

    private void markDirty() {
        if (loading || dirty) {
            return;
        }
        dirty = true;
        dirtyLabel.setVisible(true);
        validateForm();
    }

    private void onImageChanged() {
        firePreviewUpdate();
        markDirty();
    }

    private void requestClose() {
        if (!dirty) {
            removeFromSheet();
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(Messages.get("dialog.unsaved.title"));
        dialog.setText(Messages.get("dialog.unsaved.text"));
        dialog.setCancelable(true);
        dialog.setConfirmText(Messages.get("action.discard"));
        dialog.setCancelText(Messages.get(Messages.Keys.ACTION_CANCEL));
        dialog.addConfirmListener(event -> removeFromSheet());
        dialog.open();
    }

    /**
     * Assembles the action bar: Update for existing products, Save for new
     * ones, plus the Cancel button.
     *
     * @return the footer toolbar
     */
    private HorizontalLayout getButtonBar() {
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.setIcon(new Icon(VaadinIcon.CLOSE_SMALL));

        submitButton.addClickShortcut(Key.ENTER);
        closeButton.addClickShortcut(Key.ESCAPE);

        closeButton.addClickListener(this::onButtonClose);

        dirtyLabel.addClassName("product-dirty-label");
        dirtyLabel.setVisible(false);

        HorizontalLayout toolbar = new HorizontalLayout(dirtyLabel, submitButton, closeButton);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.addClassName("form-actions");
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbar.setFlexGrow(1, dirtyLabel);

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
                submitButton.setEnabled(false);
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
        saving = false;
        UI ui = getUi();
        if (ui != null) {
            UiUtil.safeAccess(ui, () -> {
                savingBar.stop();
                validateForm();
            });
        }
    }

    /**
     * Closes this editor tab when the cancel button is clicked.
     *
     * @param buttonClickEvent the click event from the close button
     */
    private void onButtonClose(ClickEvent<Button> buttonClickEvent) {
        requestClose();
    }

    @Override
    public Integer getProductId() {
        return productDto != null ? productDto.getId() : null;
    }
}