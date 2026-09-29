package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.dto.PromotionScheduleDto;
import com.harmoni.menu.dashboard.dto.PromotionSpecialPriceDto;
import com.harmoni.menu.dashboard.dto.PromotionTargetDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.enums.PromotionScopeType;
import com.harmoni.menu.dashboard.layout.enums.PromotionStatus;
import com.harmoni.menu.dashboard.layout.enums.PromotionTargetType;
import com.harmoni.menu.dashboard.layout.enums.PromotionType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientPromotionService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Slf4j
public class PromotionForm extends FormLayout {

    /**
     * The days a new promotion starts on when the operator has not chosen any. Used
     * as the form default only; a saved promotion always overwrites it in
     * {@link #restoreBean()}.
     */
    private static final Set<DayOfWeek> DEFAULT_DAYS = EnumSet.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );

    Registration broadcasterRegistration;

    @Getter
    private final BeanValidationBinder<PromotionDto> binder = new BeanValidationBinder<>(PromotionDto.class);

    @Getter
    private final TextField nameField = new TextField(Messages.get("label.name"));

    @Getter
    private final TextField codeField = new TextField(Messages.get("label.promotion.code"));

    @Getter
    private final TextField descriptionField = new TextField(Messages.get("label.promotion.description"));

    @Getter
    private final Select<PromotionType> typeSelect = new Select<>();

    @Getter
    private final Select<PromotionStatus> statusSelect = new Select<>();

    @Getter
    private final IntegerField priorityField = new IntegerField(Messages.get("label.promotion.priority"));

    @Getter
    private final DatePicker startDatePicker = new DatePicker(Messages.get("label.promotion.startDate"));

    @Getter
    private final DatePicker endDatePicker = new DatePicker(Messages.get("label.promotion.endDate"));

    @Getter
    private final Checkbox stackableCheckbox = new Checkbox(Messages.get("label.promotion.stackable"));

    @Getter
    private final BigDecimalField discountValueField = new BigDecimalField();

    private final Span discountUnitLabel = new Span();

    @Getter
    private final HorizontalLayout discountLayout = new HorizontalLayout(discountValueField, discountUnitLabel);

    @Getter
    private final HorizontalLayout daysLayout = new HorizontalLayout();

    @Getter
    private final TimePicker startTimePicker = new TimePicker();

    @Getter
    private final TimePicker endTimePicker = new TimePicker();

    @Getter
    private final HorizontalLayout timeLayout = new HorizontalLayout(startTimePicker, new Span(" → "), endTimePicker);

    @Getter
    private final HorizontalLayout scheduleTimingLayout = new HorizontalLayout(daysLayout, timeLayout);

    @Getter
    private final Select<PromotionScopeType> scopeSelect = new Select<>();

    /**
     * The organization level the promotion is limited to. Its choices cascade: a chain
     * belongs to a brand and a store to a chain, so narrowing the scope narrows the
     * fields the operator has to fill in rather than adding to them.
     */
    @Getter
    private final Select<BrandDto> brandSelect = new Select<>();

    @Getter
    private final Select<ChainDto> chainSelect = new Select<>();

    @Getter
    private final MultiSelectComboBox<StoreDto> storeSelect = new MultiSelectComboBox<>();

    /**
     * The scope fields the chosen level actually needs, laid out side by side so the
     * narrowing selector and the field it narrows read as one decision.
     */
    @Getter
    private final HorizontalLayout scopeFieldsLayout = new HorizontalLayout();

    @Getter
    private final Select<PromotionTargetType> applyToSelect = new Select<>();

    @Getter
    private final Button targetSelectButton = new Button();

    @Getter
    private final Checkbox activeCheckbox = new Checkbox(Messages.get("label.active"));

    private final Grid<PromotionSpecialPriceDto> specialPriceGrid = new Grid<>(PromotionSpecialPriceDto.class);

    private final Button addSpecialPriceButton = new Button();

    private final VerticalLayout specialPriceLayout = new VerticalLayout();

    @Getter
    private UI ui;

    private final RestClientPromotionService restClientPromotionService;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AccessService accessService;

    /**
     * The tab, action and promotion this editor was opened with, from which the four
     * fields below are taken. The fields are kept individually because the class reads
     * them in several places, and a reader should not have to remember that every one
     * of them is really a component of the same object.
     */
    private final transient PromotionEditorContext context;

    /** The scope the field held before the store picker opened, restored if it is cancelled. */
    private PromotionScopeType scopeBeforeStoreDialog;

    private final transient TabManager tabManager;
    private final Tab currentTab;
    private final FormAction formAction;

    @Getter
    private final transient PromotionDto promotionDto;

    /**
     * Opens a form for the given editor context.
     *
     * <p>Written out rather than left to {@code @RequiredArgsConstructor}, which would
     * have produced a signature with the four context components inlined and the two
     * orderings of them that the editor used to reconcile by hand.</p>
     *
     * @param restClientPromotionService       promotion reads and writes
     * @param asyncRestClientMenuService       async menu lookups
     * @param asyncRestClientOrganizationService async brand, chain and store lookups
     * @param accessService                    current user's permissions
     * @param context                          the tab, action and promotion being edited
     */
    public PromotionForm(RestClientPromotionService restClientPromotionService,
                         AsyncRestClientMenuService asyncRestClientMenuService,
                         AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                         AccessService accessService,
                         PromotionEditorContext context) {
        this.restClientPromotionService = restClientPromotionService;
        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.accessService = accessService;
        this.context = context;
        this.tabManager = context.tabManager();
        this.currentTab = context.currentTab();
        this.formAction = context.formAction();
        this.promotionDto = context.promotionDto();
    }

    private final List<PromotionSpecialPriceDto> specialPrices = new ArrayList<>();

    /**
     * The promotion types an operator can pick.
     *
     * <p>{@code SPECIAL_PRICE} is withheld on purpose. Its rows are built outside the
     * binder, so nothing validated them: a row with no SKU and no price saved cleanly and
     * was then dropped from the preview, and changing the type away from it left the rows
     * on the record, because an omitted list means "leave them alone" to the backend.
     * The grid stays in place and the type is reinstated for a promotion that already uses
     * it, so opening one cannot quietly change its type. It comes back to this list once
     * those two faults are fixed.</p>
     */
    private static final List<PromotionType> SELECTABLE_TYPES = List.of(
            PromotionType.PERCENTAGE, PromotionType.FIXED_AMOUNT);

    /**
     * The scopes an operator can pick.
     *
     * <p>{@code ALL_STORES} is withheld on purpose: a promotion is now always limited to
     * at least a brand, so the backend reads a promotion with no brand as an operator who
     * had not finished filling the section in. The value stays on the enum so a promotion
     * already saved that way still restores - it is folded into the default instead, by
     * {@link #restorableScope}.</p>
     */
    private static final List<PromotionScopeType> SELECTABLE_SCOPES = List.of(
            PromotionScopeType.BRAND, PromotionScopeType.CHAIN, PromotionScopeType.STORE);

    /**
     * The things a promotion can be applied to.
     *
     * <p>{@code SKU} is withheld on purpose: it was never a real choice, because
     * picking SKUs meant picking products first and the dialog then expanded whatever
     * SKUs those products happened to carry, so a promotion could silently land on
     * SKUs the operator never saw or agreed to. It stays on the enum, and a promotion
     * already saved against SKUs still restores and stays editable, by
     * {@link #restorableTargetTypes(List)}.</p>
     */
    private static final List<PromotionTargetType> SELECTABLE_TARGET_TYPES = List.of(
            PromotionTargetType.CATEGORY, PromotionTargetType.PRODUCT);

    /**
     * The scope a promotion that names no organization of its own is read as.
     *
     * <p>Falls back to the narrowest level that is always meaningful, rather than to
     * {@code ALL_STORES}, which is no longer offered.</p>
     */
    private static final PromotionScopeType DEFAULT_SCOPE = PromotionScopeType.BRAND;

    /**
     * How many stores to ask for in one page.
     *
     * <p>The store endpoint is paginated, and a promotion is narrowed by picking stores
     * out of a list rather than paging through it, so this is the whole chain's stores
     * rather than a page an operator would have to page through.</p>
     */
    private static final int STORE_PAGE_SIZE = 1000;

    private final List<DayOfWeek> selectedDays = new ArrayList<>();

    /**
     * The day checkboxes keyed by their day. State is read back through this map
     * rather than by re-parsing checkbox labels, so translating a label can never
     * silently clear the schedule of a promotion that is being edited.
     */
    private final Map<DayOfWeek, Checkbox> dayCheckboxes = new EnumMap<>(DayOfWeek.class);

    // Selected targets
    private final List<CategoryDto> selectedCategories = new ArrayList<>();
    private final List<ProductDto> selectedProducts = new ArrayList<>();
    private final List<Long> selectedSkuIds = new ArrayList<>();
    /**
     * Display names looked up for targets restored from a saved promotion, keyed by
     * {@link PromotionPreview#targetKey}. Kept apart from the selection lists because
     * those hold id-only stubs for restored targets, and a name fetched here says
     * nothing about what the operator has picked.
     */
    private final Map<String, String> restoredTargetNames = new LinkedHashMap<>();
    private Long pendingBrandId;
    private Long pendingChainId;
    private final Set<Long> pendingStoreIds = new LinkedHashSet<>();

    @Setter
    private BiConsumer<PromotionDto, Map<String, String>> previewUpdater;

    /**
     * The target types to offer, given what a saved promotion already uses.
     *
     * <p>A Select only holds a value that is one of its own options, so dropping
     * {@code SKU} from the list would leave a promotion saved against SKUs showing
     * something else, and saving it would quietly rewrite its targets. The type is
     * reinstated for that promotion alone; every other form offers the list it has
     * always offered.</p>
     *
     * @param saved the types the saved promotion's targets use
     * @return the types to put in the field
     */
    private static List<PromotionTargetType> restorableTargetTypes(List<PromotionTargetDto> targets) {
        if (targets == null
                || targets.stream().noneMatch(target -> target != null
                && PromotionTargetType.SKU == target.getTargetType())) {
            return SELECTABLE_TARGET_TYPES;
        }
        List<PromotionTargetType> all = new ArrayList<>(SELECTABLE_TARGET_TYPES);
        all.add(PromotionTargetType.SKU);
        return all;
    }

    /**
     * The scope to offer, given what a saved promotion already uses.
     *
     * <p>A Select only holds a value that is one of its own options, so dropping
     * {@code ALL_STORES} from the list would leave a promotion saved against no
     * organization showing something else, and saving it would quietly rewrite its
     * scope. The value is reinstated for that promotion alone; every other form offers
     * the list it has always offered.</p>
     *
     * @param saved the scope the saved promotion uses
     * @return the scope to put in the field
     */
    private static List<PromotionType> withSpecialPrice(List<PromotionType> types) {
        List<PromotionType> all = new ArrayList<>(types);
        all.add(PromotionType.SPECIAL_PRICE);
        return all;
    }


    /**
     * Fires the preview updater with the current state of the form, so the preview
     * can show what the promotion will look like if saved.
     */
    public void firePreviewUpdate() {
        if (previewUpdater != null) {
            previewUpdater.accept(buildAggregate(), buildTargetNames());
        }
    }

    /**
     * Collects the display names of the currently selected targets so the preview
     * can show names rather than bare ids. Only the ids are sent to the backend.
     *
     * @return target display names keyed by {@link PromotionPreview#targetKey}
     */
    private Map<String, String> buildTargetNames() {
        Map<String, String> names = new HashMap<>();
        selectedCategories.stream()
                .filter(category -> category.getId() != null)
                .forEach(category -> names.put(
                        PromotionPreview.targetKey(PromotionTargetType.CATEGORY, category.getId().longValue()),
                        category.getName()));
        selectedProducts.stream()
                .filter(product -> product.getId() != null)
                .forEach(product -> names.put(
                        PromotionPreview.targetKey(PromotionTargetType.PRODUCT, product.getId().longValue()),
                        product.getName()));
        selectedSkuIds.forEach(skuId -> names.put(
                PromotionPreview.targetKey(PromotionTargetType.SKU, skuId), "SKU ".concat(skuId.toString())));

        // A restored target contributes a stub with no name, so anything already
        // resolved for it is put in behind the list, never over it: a name the operator
        // picked through the dialog always wins over one fetched at restore time.
        restoredTargetNames.forEach(names::putIfAbsent);
        addScopeNames(names);
        return names;
    }

    /**
     * Adds the organizations the section is narrowed to, so the preview can name them.
     *
     * <p>Each level is recorded as it currently stands and the preview reads back the
     * one matching the chosen scope, so a brand left showing above a chosen chain cannot
     * be mistaken for the answer. The two can hold values at once only while the
     * narrower one has not been chosen from yet, which is why the scope decides.</p>
     *
     * @param names the map the preview reads its display names from
     */
    private void addScopeNames(Map<String, String> names) {
        BrandDto brand = brandSelect.getValue();
        if (brand != null) {
            names.put(PromotionPreview.BRAND_KEY, brand.getName());
        }
        ChainDto chain = chainSelect.getValue();
        if (chain != null) {
            names.put(PromotionPreview.CHAIN_KEY, chain.getName());
        }
        String stores = storeSelect.getValue().stream()
                .map(StoreDto::getName)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.joining(", "));
        if (!stores.isEmpty()) {
            names.put(PromotionPreview.STORE_KEY, stores);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        configureFields();
        configureSpecialPriceGrid();
        addValidation();
        addFields();
        restoreBean();
        // Read the type from the select, not from the dto: restoreBean has already
        // applied the create-time default (PERCENTAGE) to the select, and the
        // typeSelect listener is guarded by isFromClient() so it does not run for
        // that programmatic change. Passing promotionDto.getPromotionType() here
        // would pass null for a new promotion and hide the Discount field even
        // though the default type needs it.
        applyTypeSpecificVisibility(typeSelect.getValue());
        // Brands are the root of the section's cascade and are fetched, so this is what
        // the brand field - and everything below it - waits on. This is the one place
        // the fetch is started for an untouched form: restoreBean only sets what the
        // field should show and parks the saved narrowing for the callbacks to re-apply,
        // so asking for the brands from both would double the request on every new
        // promotion.
        loadBrands();
        firePreviewUpdate();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }

    /**
     * Configures the fields' labels, widths, and listeners so the form can react to
     * changes and update the preview.
     */
    private void configureFields() {
        nameField.setRequired(true);
        nameField.setWidthFull();
        nameField.addValueChangeListener(e -> firePreviewUpdate());

        codeField.setRequired(true);
        codeField.setWidthFull();
        codeField.setMaxLength(50);
        codeField.addValueChangeListener(e -> firePreviewUpdate());

        descriptionField.setWidthFull();
        descriptionField.setMaxLength(500);
        descriptionField.addValueChangeListener(e -> firePreviewUpdate());

        priorityField.setMin(1);
        priorityField.setMax(99);
        priorityField.setValue(1);
        priorityField.setWidth("100px");
        priorityField.addValueChangeListener(e -> firePreviewUpdate());

        startDatePicker.setWidthFull();
        startDatePicker.addValueChangeListener(e -> firePreviewUpdate());

        endDatePicker.setWidthFull();
        endDatePicker.addValueChangeListener(e -> firePreviewUpdate());

        stackableCheckbox.setValue(false);
        stackableCheckbox.addValueChangeListener(e -> firePreviewUpdate());

        typeSelect.setLabel(Messages.get("label.promotion.type"));
        typeSelect.setItems(SELECTABLE_TYPES);
        typeSelect.setItemLabelGenerator(PromotionType::getLabel);
        typeSelect.setWidthFull();
        typeSelect.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                applyTypeSpecificVisibility(change.getValue());
                firePreviewUpdate();
            }
        });

        statusSelect.setLabel(Messages.get("label.promotion.status"));
        statusSelect.setItems(PromotionStatus.values());
        statusSelect.setItemLabelGenerator(PromotionStatus::getLabel);
        statusSelect.setWidthFull();
        statusSelect.setVisible(false);

        discountValueField.setLabel(Messages.get("label.promotion.discount"));
        discountValueField.setWidth("160px");
        discountValueField.addValueChangeListener(e -> firePreviewUpdate());

        discountUnitLabel.getStyle()
                .set(Css.FONT_WEIGHT, "600")
                .set(Css.COLOR, "var(--app-text-secondary)");
        discountUnitLabel.setText("%");

        discountLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        discountLayout.setSpacing(true);

        configureDaysSelector();

        startTimePicker.setLabel(Messages.get("label.promotion.startTime"));
        startTimePicker.setStep(java.time.Duration.ofMinutes(15));
        startTimePicker.setWidth(Css.COMPACT_FIELD_WIDTH);
        startTimePicker.addValueChangeListener(e -> firePreviewUpdate());

        endTimePicker.setLabel(Messages.get("label.promotion.endTime"));
        endTimePicker.setStep(java.time.Duration.ofMinutes(15));
        endTimePicker.setWidth(Css.COMPACT_FIELD_WIDTH);
        endTimePicker.addValueChangeListener(e -> firePreviewUpdate());

        timeLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        timeLayout.setSpacing(true);

        scheduleTimingLayout.setWidthFull();
        scheduleTimingLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        scheduleTimingLayout.setSpacing(true);
        scheduleTimingLayout.getStyle().set("flex-wrap", "wrap");
        scheduleTimingLayout.setFlexGrow(1, timeLayout);

        applyToSelect.setLabel(Messages.get("label.promotion.applyTo"));
        applyToSelect.setItems(SELECTABLE_TARGET_TYPES);
        applyToSelect.setItemLabelGenerator(PromotionTargetType::getLabel);
        applyToSelect.setWidthFull();
        applyToSelect.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                updateTargetSelect(change.getValue());
                firePreviewUpdate();
            }
        });

        configureScopeFields();

        targetSelectButton.setText(Messages.get("label.promotion.target"));
        targetSelectButton.setWidthFull();
        targetSelectButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        targetSelectButton.addClickListener(e -> openTargetSelectionDialog());
        targetSelectButton.setVisible(false);

        activeCheckbox.setValue(true);
        activeCheckbox.addValueChangeListener(e -> {
            syncStatusWithActiveFlag();
            firePreviewUpdate();
        });
    }

    /**
     * Wires the "Scope To" narrowing selector and the organization fields it drives.
     *
     * <p>The fields are built once and kept, rather than rebuilt whenever the scope
     * changes. Rebuilding them meant the operator's choice lived on a component that
     * had already been thrown away, so the selected brand, chain and stores could never
     * be read back - not for the preview, not for the payload, and not to restore a
     * saved promotion.</p>
     */
    private void configureScopeFields() {
        scopeSelect.setLabel(Messages.get("label.promotion.scope"));
        scopeSelect.setItems(SELECTABLE_SCOPES);
        scopeSelect.setItemLabelGenerator(PromotionScopeType::getLabel);
        scopeSelect.setWidthFull();
        scopeSelect.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                onScopeChosen(change.getValue(), change.getOldValue());
            }
        });

        configureScopeField(brandSelect, Messages.Keys.LABEL_BRAND, Messages.Keys.PLACEHOLDER_SELECT_BRAND, BrandDto::getName);
        brandSelect.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                onBrandChanged();
            }
        });

        configureScopeField(chainSelect, Messages.Keys.LABEL_CHAIN, Messages.Keys.PLACEHOLDER_SELECT_CHAIN, ChainDto::getName);
        chainSelect.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                onChainChanged();
            }
        });

        storeSelect.setLabel(Messages.get(Messages.Keys.LABEL_STORE));
        storeSelect.setPlaceholder(Messages.get("placeholder.selectStore"));
        storeSelect.setItemLabelGenerator(StoreDto::getName);
        storeSelect.setWidthFull();
        storeSelect.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                firePreviewUpdate();
            }
        });

        scopeFieldsLayout.add(brandSelect, chainSelect, storeSelect);
        scopeFieldsLayout.setWidthFull();
        scopeFieldsLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        scopeFieldsLayout.setFlexGrow(1, brandSelect);
        scopeFieldsLayout.setFlexGrow(1, chainSelect);
        scopeFieldsLayout.setFlexGrow(1, storeSelect);
    }

    /**
     * Applies the settings every organization field shares, so each one only has to
     * declare its label and its own name generator.
     *
     * @param field          the field being configured
     * @param labelKey       message key for its label
     * @param placeholderKey message key shown while nothing is chosen
     * @param nameGenerator  reads the display name off an option
     * @param <T>            the option type
     */
    private <T> void configureScopeField(Select<T> field, String labelKey, String placeholderKey,
                                         ItemLabelGenerator<T> nameGenerator) {
        field.setLabel(Messages.get(labelKey));
        field.setEmptySelectionCaption(Messages.get(placeholderKey));
        field.setItemLabelGenerator(nameGenerator);
        field.setWidthFull();
    }

    /**
     * Whether this form is authoring a promotion that does not exist yet, and so owns
     * its lifecycle state.
     *
     * <p>Matches the rule the list view uses to decide between the create form and the
     * edit form: an action of {@code CREATE}, or a row that turned out to have no id.</p>
     */
    private boolean isNewPromotion() {
        return formAction == FormAction.CREATE || ObjectUtils.isEmpty(promotionDto);
    }

    /**
     * Authors the lifecycle state of a new promotion from its Active checkbox.
     *
     * <p>The checkbox and {@code status} are one decision expressed twice, and
     * {@code status} is the one the menu service stores. Hard-coding {@code DRAFT}
     * while the checkbox was ticked meant a promotion the operator had switched on was
     * saved - and previewed - as a draft it was never actually live in, so the preview
     * showed "Active" and "Draft" side by side for the same promotion.</p>
     *
     * <p>Deliberately a no-op on an existing promotion: its status is owned by the
     * status endpoint, and quietly reactivating a paused or cancelled promotion because
     * the operator was editing an unrelated field would be far worse than the
     * inconsistency this fixes.</p>
     */
    private void syncStatusWithActiveFlag() {
        if (!isNewPromotion()) {
            return;
        }
        statusSelect.setValue(Boolean.TRUE.equals(activeCheckbox.getValue())
                ? PromotionStatus.ACTIVE
                : PromotionStatus.DRAFT);
    }

    /**
     * Configures the checkboxes for the days of the week, so the operator can pick
     * which days the promotion is active on.
     *
     * <p>The checkboxes are keyed by their day rather than by their label, so a
     * translation cannot silently clear a promotion's schedule. The backing list of
     * selected days is kept in sync with the checkboxes, because {@code Checkbox.setValue}
     * does not fire a value change event when the new value equals the current one.</p>
     */
    private void configureDaysSelector() {
        daysLayout.removeAll();
        dayCheckboxes.clear();
        daysLayout.setSpacing(true);
        daysLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        Span daysLabel = new Span(Messages.get("label.promotion.days"));
        daysLabel.addClassName("field-label");
        daysLabel.setWidth("56px");
        daysLayout.add(daysLabel);

        List<DayOfWeek> dayOrder = Arrays.asList(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        );

        for (DayOfWeek day : dayOrder) {
            Checkbox dayCheckbox = new Checkbox(dayName(day));
            dayCheckbox.addClassName("day-checkbox");
            dayCheckbox.addValueChangeListener(event -> {
                if (event.getValue()) {
                    if (!selectedDays.contains(day)) {
                        selectedDays.add(day);
                    }
                } else {
                    selectedDays.remove(day);
                }
                firePreviewUpdate();
            });
            daysLayout.add(dayCheckbox);
            dayCheckboxes.put(day, dayCheckbox);
        }

        setSelectedDays(DEFAULT_DAYS);
    }

    /**
     * Writes the day selection to both halves of the state: the checkboxes the operator
     * sees and the list that is submitted.
     *
     * <p>Both are written because {@code Checkbox.setValue} does not fire a value change
     * event when the new value equals the current one. Restoring a promotion through
     * {@code setValue} alone therefore only reaches the backing list for the days whose
     * value actually flips, and the create-time default already has Monday to Friday
     * ticked. A promotion scheduled on those days would then submit an empty day list -
     * and be rejected with "select at least one day of the week" - while every checkbox
     * still looks correctly ticked.</p>
     *
     * @param days the days that should end up selected
     */
    private void setSelectedDays(Set<DayOfWeek> days) {
        dayCheckboxes.forEach((day, checkbox) -> checkbox.setValue(days.contains(day)));
        selectedDays.clear();
        dayCheckboxes.keySet().stream().filter(days::contains).forEach(selectedDays::add);
    }

    private String dayName(DayOfWeek day) {
        return Messages.get("promotion.day." + day.name());
    }

    /**
     * Configures the grid that holds the special price rows, and the button that adds
     * a new row to it.
     */
    private void configureSpecialPriceGrid() {
        specialPriceGrid.setSizeFull();
        specialPriceGrid.removeAllColumns();
        specialPriceGrid.addComponentColumn(this::specialPriceRow)
                .setHeader(Messages.get("grid.header.specialPrice"))
                .setFlexGrow(1);
        specialPriceGrid.setItems(specialPrices);
        specialPriceGrid.setHeight("200px");

        addSpecialPriceButton.setText(Messages.get("action.addSpecialPrice"));
        addSpecialPriceButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        addSpecialPriceButton.addClickListener(event -> addSpecialPriceRow());

        specialPriceLayout.setWidthFull();
        specialPriceLayout.setPadding(false);
        specialPriceLayout.setSpacing(false);
        specialPriceLayout.add(specialPriceGrid, addSpecialPriceButton);
        specialPriceLayout.setVisible(false);
    }

    /**
     * Builds a row for the special price grid, with fields for the SKU and the price.
     *
     * <p>The fields are not bound to the bean because they are not validated, so a
     * row with no SKU and no price saved cleanly and was then dropped from the preview.
     * The fields are kept in sync with the backing list of special prices, and any
     * change fires a preview update.</p>
     *
     * @param specialPrice the special price to build a row for
     * @return a component that holds the row's fields
     */
    private Component specialPriceRow(PromotionSpecialPriceDto specialPrice) {
        IntegerField skuField = new IntegerField();
        skuField.setMin(1);
        skuField.setWidth("100px");
        skuField.setValue(specialPrice.getSkuId() == null ? null : specialPrice.getSkuId().intValue());

        BigDecimalField priceField = new BigDecimalField();
        priceField.setWidth(Css.COMPACT_FIELD_WIDTH);
        priceField.setValue(specialPrice.getSpecialPrice());

        Runnable sync = () -> {
            specialPrice.setSkuId(skuField.getValue() == null ? null : skuField.getValue().longValue());
            specialPrice.setSpecialPrice(priceField.getValue());
            firePreviewUpdate();
        };
        skuField.addValueChangeListener(change -> sync.run());
        priceField.addValueChangeListener(change -> sync.run());

        HorizontalLayout row = new HorizontalLayout(skuField, priceField);
        row.setAlignItems(FlexComponent.Alignment.BASELINE);
        row.setSpacing(true);
        row.add(UiUtil.deleteButton(event -> removeRow(specialPrices, specialPriceGrid, specialPrice)));
        return row;
    }

    /**
     * Adds a new, empty special price row to the grid.
     *
     * <p>The row is not validated, so a row with no SKU and no price saved cleanly and
     * was then dropped from the preview. The operator can delete it before saving, or
     * fill it in and save it.</p>
     */
    private void addSpecialPriceRow() {
        specialPrices.add(PromotionSpecialPriceDto.builder().build());
        specialPriceGrid.setItems(specialPrices);
        firePreviewUpdate();
    }

    /**
     * Removes a row from the grid and its backing list.
     *
     * <p>The row is not validated, so a row with no SKU and no price saved cleanly and
     * was then dropped from the preview. The operator can delete it before saving, or
     * fill it in and save it.</p>
     *
     * @param rows the backing list of rows
     * @param grid the grid that shows the rows
     * @param row  the row to remove
     * @param <T>  the type of the rows
     */
    private <T> void removeRow(List<T> rows, Grid<T> grid, T row) {
        if (rows.remove(row)) {
            grid.setItems(rows);
            firePreviewUpdate();
        }
    }

    /**
     * Shows or hides the fields that are relevant to the chosen promotion type.
     *
     * <p>{@code SPECIAL_PRICE} is withheld on purpose. Its rows are built outside the
     * binder, so nothing validated them: a row with no SKU and no price saved cleanly and
     * was then dropped from the preview, and changing the type away from it left the rows
     * on the record, because an omitted list means "leave them alone" to the backend.
     * The grid stays in place and the type is reinstated for a promotion that already uses
     * it, so opening one cannot quietly change its type. It comes back to this list once
     * those two faults are fixed.</p>
     *
     * @param type the promotion type that was chosen
     */
    private void applyTypeSpecificVisibility(PromotionType type) {
        boolean isPercentageOrFixed = type == PromotionType.PERCENTAGE || type == PromotionType.FIXED_AMOUNT;
        boolean isSpecialPrice = type == PromotionType.SPECIAL_PRICE;

        discountLayout.setVisible(isPercentageOrFixed);
        daysLayout.setVisible(true);
        timeLayout.setVisible(true);
        // Every selectable type targets something, so the target section is always
        // shown. The scope fields inside it stay under the scope's own control.
        applyToSelect.setVisible(true);
        setSpecialPriceVisible(isSpecialPrice);

        if (isPercentageOrFixed) {
            discountUnitLabel.setText(type == PromotionType.PERCENTAGE ? "%" : "Rp");
        }
    }

    private void setSpecialPriceVisible(boolean visible) {
        specialPriceLayout.setVisible(visible);
    }

    /**
     * Shows or hides the button that opens the target selection dialog, and sets its
     * label and icon to match the chosen target type.
     *
     * <p>{@code SKU} is withheld on purpose: it was never a real choice, because
     * picking SKUs meant picking products first and the dialog then expanded whatever
     * SKUs those products happened to carry, so a promotion could silently land on
     * SKUs the operator never saw or agreed to. It stays on the enum, and a promotion
     * already saved against SKUs still restores and stays editable, by
     * {@link #restorableTargetTypes(List)}.</p>
     *
     * @param targetType the target type that was chosen
     */
    private void updateTargetSelect(PromotionTargetType targetType) {
        if (targetType == null) {
            targetSelectButton.setVisible(false);
            targetSelectButton.setText(Messages.get("label.promotion.target"));
            return;
        }

        targetSelectButton.setVisible(true);
        switch (targetType) {
            case CATEGORY -> {
                targetSelectButton.setText(Messages.get("button.selectCategory"));
                targetSelectButton.setIcon(VaadinIcon.FOLDER.create());
            }
            case PRODUCT -> {
                targetSelectButton.setText(Messages.get("button.selectProducts"));
                targetSelectButton.setIcon(VaadinIcon.PACKAGE.create());
            }
            case SKU -> {
                targetSelectButton.setText(Messages.get("button.selectSkus"));
                targetSelectButton.setIcon(VaadinIcon.BARCODE.create());
            }
        }
        updateTargetButtonText(targetType);
    }

    /**
     * Updates the target selection button's text to reflect how many targets are
     * currently selected.
     *
     * @param targetType the target type that was chosen
     */
    private void updateTargetButtonText(PromotionTargetType targetType) {
        if (targetType == null) {
            // A saved row can carry no type, and updateTargetSelect has already put the
            // generic label on the button by then. Switching here would throw and take
            // the whole restore down, so leave what it set.
            return;
        }

        String baseText = switch (targetType) {
            case CATEGORY -> selectedCategories.isEmpty() ? Messages.get("button.selectCategory")
                    : Messages.get("button.categorySelected", selectedCategories.size());
            case PRODUCT -> selectedProducts.isEmpty() ? Messages.get("button.selectProducts")
                    : Messages.get("button.productSelected", selectedProducts.size());
            case SKU -> selectedSkuIds.isEmpty() ? Messages.get("button.selectSkus")
                    : Messages.get("button.skuSelected", selectedSkuIds.size());
        };
        targetSelectButton.setText(baseText);
    }

    /**
     * Reacts to a change of the "Scope To" level.
     *
     * @param scope the newly chosen level, the default when cleared
     */
    private void updateScopeSelect(PromotionScopeType scope) {
        PromotionScopeType level = ObjectUtils.defaultIfNull(scope, DEFAULT_SCOPE);
        applyScopeFieldVisibility(level);
        loadOptionsFor(level);
        firePreviewUpdate();
    }

    /**
     * Acts on a scope the operator picked.
     *
     * <p>{@code Store} opens the store picker rather than showing a store field: a
     * store-scoped promotion needs a brand and a chain above it to mean anything, and
     * there is no room for all three in the section's own row. The scope is put back
     * until the operator settles on some, so the form never sits on {@code Store} with
     * nothing behind it.</p>
     *
     * @param chosen  the scope now shown in the field
     * @param previous the scope the field held before, restored if the picker is cancelled
     */
    private void onScopeChosen(PromotionScopeType chosen, PromotionScopeType previous) {
        if (PromotionScopeType.STORE == chosen) {
            scopeBeforeStoreDialog = previous;
            openStoreSelectionDialog();
            return;
        }
        updateScopeSelect(chosen);
    }

    /**
     * Asks for the stores, and adopts whatever the operator settles on.
     *
     * <p>Seeded from the form's own brand, chain and stores, so a promotion already
     * narrowed to a chain does not make the operator pick that chain again. Cancelling
     * puts the scope field back where it was and changes nothing else.</p>
     *
     * <p>Package-private so a test can stand in for it: opening a dialog needs a live UI,
     * and what is worth pinning down is the decision to open one, not Vaadin's opening.</p>
     */
    void openStoreSelectionDialog() {
        StoreSelectionDialog dialog = new StoreSelectionDialog(
                asyncRestClientOrganizationService,
                brandSelect.getValue(),
                chainSelect.getValue(),
                new ArrayList<>(storeSelect.getValue()),
                this::onStoresChosen);
        dialog.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                revertStoreScope();
            }
        });
        dialog.open();
    }

    /**
     * Adopts the stores the operator settled on and switches the section over to them.
     *
     * <p>The form's own fields are filled straight from the dialog rather than refetched:
     * the dialog has just read the same three endpoints, and a second round of requests
     * could come back with a different chain underneath the same selection.</p>
     */
    private void onStoresChosen(StoreSelectionDialog.Selection selection) {
        scopeSelect.setValue(PromotionScopeType.STORE);
        applyScopeFieldVisibility(PromotionScopeType.STORE);
        adoptBrand(selection.brand());
        adoptChain(selection.chain());
        pendingStoreIds.clear();
        storeSelect.setItems(selection.stores());
        storeSelect.setValue(selection.stores().stream()
                .filter(store -> store != null && store.getId() != null)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        scopeBeforeStoreDialog = null;
        firePreviewUpdate();
    }

    /**
     * Puts the chosen brand in the field without throwing away the other brands.
     *
     * <p>Carried over by id, because the dialog and the form hold separate objects for
     * the same brand and a field only keeps a value it is currently offering. The
     * one-brand fallback is for a brand the form has not listed, which happens when the
     * picker was reached before the form's own load came back.</p>
     */
    private void adoptBrand(BrandDto brand) {
        if (selectBrand(idOf(brand.getId()))) {
            return;
        }
        brandSelect.setItems(List.of(brand));
        brandSelect.setValue(brand);
    }

    /** Puts the chosen chain in the field without throwing away the other chains. */
    private void adoptChain(ChainDto chain) {
        if (selectChain(idOf(chain.getId()))) {
            return;
        }
        chainSelect.setItems(List.of(chain));
        chainSelect.setValue(chain);
    }

    /** Puts the scope field back after the store picker was cancelled. */
    private void revertStoreScope() {
        if (scopeBeforeStoreDialog == null) {
            return;
        }
        PromotionScopeType previous = scopeBeforeStoreDialog;
        scopeBeforeStoreDialog = null;
        scopeSelect.setValue(previous);
        updateScopeSelect(previous);
    }

    /**
     * Shows the organization fields the given scope narrows by, and hides the rest.
     *
     * <p>Brand sits above chain, and chain above store, so a scope needs everything it
     * sits above: a chain is only meaningful within a brand, and a store only within a
     * chain.</p>
     *
     * @param scope the scope currently chosen
     */
    private void applyScopeFieldVisibility(PromotionScopeType scope) {
        brandSelect.setVisible(scope.narrowsTo(PromotionScopeType.BRAND));
        chainSelect.setVisible(scope.narrowsTo(PromotionScopeType.CHAIN));
        storeSelect.setVisible(scope.narrowsTo(PromotionScopeType.STORE));
        scopeFieldsLayout.setVisible(scope.narrowsTo(PromotionScopeType.BRAND));
    }

    /**
     * Loads the options the given scope needs, skipping the levels it does not reach.
     *
     * <p>Chains and stores are requested from the brand and chain above them, so asking
     * for them at a scope that hides the field they depend on would fetch a list the
     * operator cannot see or use.</p>
     *
     * @param scope the scope currently chosen
     */
    private void loadOptionsFor(PromotionScopeType scope) {
        if (brandSelect.getValue() == null) {
            loadBrands();
            return;
        }
        loadChains();
        if (scope == PromotionScopeType.STORE) {
            loadStores();
        }
    }

    /**
     * Narrows the chain and store options to the newly chosen brand.
     *
     * <p>Both dependent fields are cleared rather than kept: a chain that survived a
     * brand change would submit a brand and a chain that contradict each other. The
     * chains are then fetched again, because a cleared field left empty would take the
     * whole chain and store level out of reach until the promotion was reopened.</p>
     *
     * <p>The store level is left to {@link #applyChains(List)}, which fetches it once
     * the chain actually in hand is known.</p>
     */
    private void onBrandChanged() {
        chainSelect.clear();
        storeSelect.clear();
        loadChains();
        firePreviewUpdate();
    }

    /**
     * Narrows the store options to the newly chosen chain.
     *
     * <p>As with a brand change, clearing the field is only half the job: the stores of
     * the new chain are fetched so the level stays reachable.</p>
     */
    private void onChainChanged() {
        storeSelect.clear();
        if (scopeSelect.getValue() == PromotionScopeType.STORE) {
            loadStores();
        }
        firePreviewUpdate();
    }

    /**
     * Fetches the brands the promotion can be limited to.
     *
     * <p>Brands are the root of the cascade, so this is also what the section waits on
     * before it can offer any other level.</p>
     */
    private void loadBrands() {
        if (asyncRestClientOrganizationService == null) {
            return;
        }
        asyncRestClientOrganizationService.getAllBrandAsync(
                brands -> UiUtil.safeAccess(ui, () -> applyBrands(brands)),
                error -> UiUtil.safeAccess(ui, () -> UiUtil.errorWithRetry(
                        Messages.get("notification.brand.loadFailed"), this::loadBrands)));
    }

    /**
     * Puts the fetched brands in the field and carries on down the cascade.
     *
     * <p>Whichever brand is chosen - the one already in the field, or the one a saved
     * promotion was narrowed by - is put back by id. The id has to be carried across the
     * reload by hand: the field only keeps a brand that is one of the objects it is
     * currently holding, and a refetch hands it fresh ones, so a selection the operator
     * had already made would otherwise be dropped and quietly reset to the first
     * brand.</p>
     *
     * <p>Failing both, the first brand is chosen, so the section is never left asking for
     * a brand it is able to name.</p>
     *
     * @param brands the brands the backend returned
     */
    private void applyBrands(List<BrandDto> brands) {
        Long wanted = firstNonNull(selectedBrandId(), pendingBrandId);
        pendingBrandId = null;
        brandSelect.setItems(brands == null ? List.of() : brands);
        if (!selectBrand(wanted)) {
            selectFirstBrand();
        }
        loadChains();
        firePreviewUpdate();
    }

    /**
     * Chooses a brand by id.
     *
     * @param brandId the brand to choose, may be {@code null}
     * @return {@code true} when that brand is among the options and was chosen
     */
    private boolean selectBrand(Long brandId) {
        return brandSelect.getListDataView().getItems()
                .filter(brand -> brandId != null && brandId.equals(idOf(brand.getId())))
                .findFirst()
                .map(brand -> {
                    brandSelect.setValue(brand);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Fetches the chains of the chosen brand.
     */
    private void loadChains() {
        BrandDto brand = brandSelect.getValue();
        if (brand == null || brand.getId() == null || asyncRestClientOrganizationService == null) {
            return;
        }
        asyncRestClientOrganizationService.getAllChainByBrandIdAsync(
                chains -> UiUtil.safeAccess(ui, () -> applyChains(chains)),
                error -> UiUtil.safeAccess(ui, () -> UiUtil.errorWithRetry(
                        Messages.get(Messages.Keys.NOTIFICATION_CHAIN_LOAD_FAILED), this::loadChains)),
                brand.getId());
    }

    /**
     * Puts the fetched chains in the field, and fetches the stores they narrow.
     *
     * <p>The chosen chain is carried across the reload by id, for the same reason the
     * brand's is. Failing that the first is chosen, because the store level is reached
     * through the chain: a brand with no chain chosen has nothing to offer underneath
     * it.</p>
     *
     * @param chains the chains of the chosen brand
     */
    private void applyChains(List<ChainDto> chains) {
        Long wanted = firstNonNull(selectedChainId(), pendingChainId);
        pendingChainId = null;
        chainSelect.setItems(chains == null ? List.of() : chains);
        if (!selectChain(wanted)) {
            selectFirstChain();
        }
        if (scopeSelect.getValue() == PromotionScopeType.STORE) {
            loadStores();
        }
        firePreviewUpdate();
    }

    /**
     * Chooses a chain by id.
     *
     * @param chainId the chain to choose, may be {@code null}
     * @return {@code true} when that chain is among the options and was chosen
     */
    private boolean selectChain(Long chainId) {
        return chainSelect.getListDataView().getItems()
                .filter(chain -> chainId != null && chainId.equals(idOf(chain.getId())))
                .findFirst()
                .map(chain -> {
                    chainSelect.setValue(chain);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Fetches the stores of the chosen chain.
     */
    private void loadStores() {
        ChainDto chain = chainSelect.getValue();
        if (chain == null || chain.getId() == null || asyncRestClientOrganizationService == null) {
            return;
        }
        asyncRestClientOrganizationService.getAllStoreAsync(
                page -> UiUtil.safeAccess(ui, () -> applyStores(extractStores(page))),
                error -> UiUtil.safeAccess(ui, () -> UiUtil.errorWithRetry(
                        Messages.get(Messages.Keys.NOTIFICATION_STORE_LOAD_FAILED), this::loadStores)),
                chain.getId(), 1, STORE_PAGE_SIZE, "");
    }

    /**
     * Puts the fetched stores in the field.
     *
     * <p>No store is chosen by default, unlike the levels above. This is the one field
     * that takes several values, and pre-ticking one would quietly narrow a promotion to
     * a single store the operator never picked.</p>
     *
     * @param stores the stores of the chosen chain
     */
    private void applyStores(List<StoreDto> stores) {
        storeSelect.setItems(stores == null ? List.of() : stores);
        restorePendingStores();
        firePreviewUpdate();
    }

    /**
     * Reads the store list out of the endpoint's page envelope.
     *
     * <p>The store endpoint answers with a page rather than a plain list, so the rows sit
     * under a {@code data} key and are untyped maps until they are converted.</p>
     *
     * @param page the endpoint response
     * @return the stores it carried, empty when it carried none
     */
    private static List<StoreDto> extractStores(Map<String, Object> page) {
        List<StoreDto> stores = new ArrayList<>();
        if (page == null || !(page.get("data") instanceof List<?> rows)) {
            return stores;
        }
        for (Object row : rows) {
            StoreDto store = ObjectUtil.convertValueToObject(row, StoreDto.class);
            if (store != null) {
                stores.add(store);
            }
        }
        return stores;
    }

    /**
     * Chooses the first brand offered, if there is one.
     */
    private void selectFirstBrand() {
        brandSelect.getListDataView().getItems()
                .findFirst()
                .ifPresent(brandSelect::setValue);
    }

    /**
     * Chooses the first chain offered, if there is one.
     */
    private void selectFirstChain() {
        chainSelect.getListDataView().getItems()
                .findFirst()
                .ifPresent(chainSelect::setValue);
    }

    /**
     * The brand the promotion is limited to.
     *
     * @return the selected brand id, {@code null} when the scope is not brand-based or
     *         nothing was chosen
     */
    private Long selectedBrandId() {
        return idOf(brandSelect.getValue() == null ? null : brandSelect.getValue().getId());
    }

    /**
     * The chain the promotion is limited to.
     *
     * @return the selected chain id, {@code null} when the scope is not chain-based or
     *         nothing was chosen
     */
    private Long selectedChainId() {
        return idOf(chainSelect.getValue() == null ? null : chainSelect.getValue().getId());
    }

    /**
     * The stores the promotion is limited to.
     *
     * @return the selected store ids, empty when the scope is not store-based
     */
    private List<Long> selectedStoreIds() {
        if (storeSelect.getValue() == null) {
            return List.of();
        }
        return storeSelect.getValue().stream()
                .map(store -> idOf(store.getId()))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Widens a form-level id to the type the payload carries.
     *
     * <p>The organization DTOs use {@code Integer} ids while the promotion payload uses
     * {@code Long}. Converting in one place keeps a widening cast out of the places that
     * read a selection, where it is easy to miss one of the three.</p>
     *
     * @param id the id read off a component, may be {@code null}
     * @return the same id as a {@code Long}, or {@code null} when there was none
     */
    private static Long idOf(Integer id) {
        return id == null ? null : id.longValue();
    }

    /**
     * Opens the dialog that lets the operator pick the targets the promotion applies to.
     *
     * <p>The dialog is chosen by the target type, and seeded with whatever targets are
     * already selected. The operator can change them, and the selection is written back
     * to the form.</p>
     */
    private void openTargetSelectionDialog() {
        PromotionTargetType targetType = applyToSelect.getValue();
        if (targetType == null) {
            return;
        }

        Integer brandId = targetBrandId();
        if (brandId == null) {
            UiUtil.error(Messages.get("error.brandNotSelected"));
            return;
        }

        switch (targetType) {
            case CATEGORY -> openCategorySelectionDialog(brandId);
            case PRODUCT -> openProductSelectionDialog(brandId);
            case SKU -> openSkuSelectionDialog(brandId);
        }
    }

    /**
     * Opens the dialog that lets the operator pick categories.
     *
     * <p>The dialog is seeded with whatever categories are already selected. The
     * operator can change them, and the selection is written back to the form.</p>
     *
     * @param brandId the brand whose categories to list
     */
    private void openCategorySelectionDialog(Integer brandId) {
        CategorySelectionDialog dialog = new CategorySelectionDialog(
                asyncRestClientMenuService,
                brandId,
                new ArrayList<>(selectedCategories),
                selected -> {
                    selectedCategories.clear();
                    selectedCategories.addAll(selected);
                    updateTargetButtonText(PromotionTargetType.CATEGORY);
                    firePreviewUpdate();
                }
        );
        dialog.open();
    }

    /**
     * Opens the dialog that lets the operator pick products.
     *
     * <p>The dialog is seeded with whatever products are already selected. The
     * operator can change them, and the selection is written back to the form.</p>
     *
     * @param brandId the brand whose products to list
     */
    private void openProductSelectionDialog(Integer brandId) {
        ProductSelectionDialog dialog = new ProductSelectionDialog(
                asyncRestClientMenuService,
                brandId,
                new ArrayList<>(selectedProducts),
                selected -> {
                    selectedProducts.clear();
                    selectedProducts.addAll(selected);
                    updateTargetButtonText(PromotionTargetType.PRODUCT);
                    firePreviewUpdate();
                }
        );
        dialog.open();
    }

    /**
     * Opens the dialog that lets the operator pick SKUs.
     *
     * <p>The dialog is seeded with whatever SKUs are already selected. The
     * operator can change them, and the selection is written back to the form.</p>
     *
     * @param brandId the brand whose SKUs to list
     */
    private void openSkuSelectionDialog(Integer brandId) {
        // For SKU, we can reuse ProductSelectionDialog and extract SKUs from products
        ProductSelectionDialog dialog = new ProductSelectionDialog(
                asyncRestClientMenuService,
                brandId,
                new ArrayList<>(selectedProducts),
                selected -> {
                    selectedProducts.clear();
                    selectedProducts.addAll(selected);
                    // Extract unique SKU IDs from selected products
                    selectedSkuIds.clear();
                    for (ProductDto product : selectedProducts) {
                        if (product.getSkuDtos() != null) {
                            for (var sku : product.getSkuDtos()) {
                                if (sku.getId() != null) {
                                    selectedSkuIds.add(sku.getId().longValue());
                                }
                            }
                        }
                    }
                    updateTargetButtonText(PromotionTargetType.SKU);
                    firePreviewUpdate();
                }
        );
        dialog.open();
    }

    /**
     * The brand whose categories and products the target pickers list.
     *
     * <p>This is the brand the operator narrowed the promotion to, not the brand of the
     * store they happen to be logged into. Picking targets from their own brand while the
     * promotion is scoped to another would offer them categories and products that do not
     * exist in the brand the promotion runs in.</p>
     *
     * <p>Falls back to the session's own brand, which only comes into play if the brands
     * failed to load - the field would otherwise be empty and the operator left unable to
     * pick any target at all.</p>
     *
     * @return the brand to list targets from, {@code null} when neither is available
     */
    private Integer targetBrandId() {
        BrandDto brand = brandSelect.getValue();
        if (brand != null && brand.getId() != null) {
            return brand.getId();
        }
        return sessionBrandId();
    }

    /**
     * The brand the logged-in operator belongs to.
     *
     * @return their brand id, {@code null} when the session does not carry one
     */
    private Integer sessionBrandId() {
        if (accessService == null) {
            return null;
        }
        UserDto user = accessService.getUserDetail();
        if (user == null || user.getStoreDto() == null || user.getStoreDto().getChainDto() == null) {
            return null;
        }
        return user.getStoreDto().getChainDto().getBrandId();
    }

    private void addValidation() {
        binder.forField(nameField)
                .asRequired(Messages.get("validation.promotion.nameRequired"))
                .withValidator(value -> value != null && value.trim().length() >= 2,
                        Messages.get("validation.promotion.nameMinLength"))
                .bind(PromotionDto::getName, PromotionDto::setName);

        binder.forField(codeField)
                .asRequired(Messages.get("validation.promotion.codeRequired"))
                .withValidator(value -> value != null && value.trim().length() >= 2,
                        Messages.get("validation.promotion.codeMinLength"))
                .bind(PromotionDto::getCode, PromotionDto::setCode);

        binder.forField(descriptionField)
                .bind(PromotionDto::getDescription, PromotionDto::setDescription);

        binder.forField(priorityField)
                .asRequired(Messages.get("validation.promotion.priorityRequired"))
                .bind(PromotionDto::getPriority, PromotionDto::setPriority);

        binder.forField(startDatePicker)
                .bind(PromotionDto::getStartDate, PromotionDto::setStartDate);

        binder.forField(endDatePicker)
                .bind(PromotionDto::getEndDate, PromotionDto::setEndDate);

        binder.forField(stackableCheckbox)
                .bind(PromotionDto::getStackable, PromotionDto::setStackable);

        binder.forField(typeSelect)
                .asRequired(Messages.get("validation.promotion.typeRequired"))
                .bind(PromotionDto::getPromotionType, PromotionDto::setPromotionType);

        binder.forField(statusSelect)
                .bind(PromotionDto::getStatus, PromotionDto::setStatus);

        // No validator here on purpose: the discount only applies to a percentage or
        // fixed amount promotion, and this field is hidden for every other type, so a
        // required-value validator would fail on a field the operator never sees.
        // isAggregateValid() enforces the rule per type and reports it by name.
        binder.forField(discountValueField)
                .bind(PromotionDto::getDiscountValue, PromotionDto::setDiscountValue);

        binder.forField(applyToSelect)
                .bind(PromotionDto::getApplyToType, PromotionDto::setApplyToType);

        // targetSelectButton is not bound to binder - handled manually

        binder.forField(startTimePicker)
                .bind(PromotionDto::getStartTime, PromotionDto::setStartTime);

        binder.forField(endTimePicker)
                .bind(PromotionDto::getEndTime, PromotionDto::setEndTime);

        binder.forField(activeCheckbox)
                .bind(PromotionDto::getActive, PromotionDto::setActive);
    }

    private void addFields() {
        setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep(Css.DIALOG_WIDTH, 2)
        );

        // GENERAL SECTION - Card
        FormLayout generalForm = new FormLayout();
        generalForm.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep(Css.DIALOG_WIDTH, 2)
        );
        // Name and code share a row rather than each taking a full one. Both are short,
        // single-line values - a code is a handful of characters - so giving each a row of
        // its own spent two rows on two short fields. They still stack on a narrow form,
        // because the section's responsive steps drop to one column below 600px.
        generalForm.add(nameField, codeField);
        generalForm.setColspan(descriptionField, 2);
        generalForm.add(descriptionField);
        generalForm.add(typeSelect);
        // The discount is what the promotion gives, not when it runs, so it belongs with
        // the other general properties. It sits next to the type because its unit and
        // visibility both follow the type, and it is hidden for SPECIAL_PRICE.
        generalForm.add(discountLayout);
        generalForm.add(priorityField);
        generalForm.add(startDatePicker);
        generalForm.add(endDatePicker);
        generalForm.add(stackableCheckbox);
        generalForm.add(activeCheckbox);
        generalForm.add(statusSelect);

        Div generalCard = createSectionCard(Messages.get("promotion.section.general"), generalForm);
        setColspan(generalCard, 2);
        add(generalCard);

        // SCHEDULE SECTION - Card
        FormLayout scheduleForm = new FormLayout();
        scheduleForm.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep(Css.DIALOG_WIDTH, 2)
        );
        scheduleForm.add(scheduleTimingLayout);
        scheduleForm.setColspan(scheduleTimingLayout, 2);

        Div scheduleCard = createSectionCard(Messages.get("promotion.section.schedule"), scheduleForm);
        setColspan(scheduleCard, 2);
        add(scheduleCard);

        // TARGET SECTION - Card
        FormLayout targetForm = new FormLayout();
        targetForm.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep(Css.DIALOG_WIDTH, 2)
        );
        // "Scope To" and the organization it narrows to are one decision, so the
        // selector and the fields it drives share a row rather than stacking into two.
        // The fields collapse to nothing at All Stores, leaving the selector on its own.
        targetForm.add(scopeSelect, scopeFieldsLayout);
        targetForm.setColspan(scopeSelect, 1);
        targetForm.setColspan(scopeFieldsLayout, 1);

        // "Target To" and the picker it opens are likewise one decision.
        targetForm.add(applyToSelect);
        targetForm.setColspan(applyToSelect, 1);
        targetForm.add(targetSelectButton);
        targetForm.setColspan(targetSelectButton, 1);

        targetForm.add(specialPriceLayout);
        targetForm.setColspan(specialPriceLayout, 2);

        Div targetCard = createSectionCard(Messages.get("promotion.section.target"), targetForm);
        setColspan(targetCard, 2);
        add(targetCard);
    }

    /**
     * Wraps a form layout in a card with a title.
     *
     * <p>The card is styled to look like a card, and the title is styled to look like a
     * section heading. The form layout is added below the title.</p>
     *
     * @param title   the title of the section
     * @param content the form layout to wrap
     * @return a div containing the title and form layout
     */
    private Div createSectionCard(String title, FormLayout content) {
        Div card = new Div();
        card.addClassName("form-section-card");
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", Css.HAIRLINE_BORDER)
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set(Css.PADDING, "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-m)");

        H3 titleLabel = new H3(title);
        titleLabel.getStyle()
                .set(Css.MARGIN, "0 0 var(--lumo-space-m) 0")
                .set(Css.FONT_SIZE, "var(--lumo-font-size-m)")
                .set(Css.FONT_WEIGHT, "600")
                .set(Css.COLOR, "var(--lumo-primary-text-color)")
                .set("padding-bottom", "var(--lumo-space-s)")
                .set(Css.BORDER_BOTTOM, Css.HAIRLINE_BORDER);

        card.add(titleLabel, content);
        return card;
    }

    private void restoreBean() {
        if (isNewPromotion()) {
            typeSelect.setValue(PromotionType.PERCENTAGE);
            activeCheckbox.setValue(true);
            syncStatusWithActiveFlag();
            startTimePicker.setValue(LocalTime.of(15, 0));
            endTimePicker.setValue(LocalTime.of(17, 0));
            applyToSelect.setValue(PromotionTargetType.CATEGORY);
            updateTargetSelect(PromotionTargetType.CATEGORY);
            scopeSelect.setValue(DEFAULT_SCOPE);
            applyScopeFieldVisibility(DEFAULT_SCOPE);
            return;
        }

        PromotionDto source = promotionDto;
        if (PromotionType.SPECIAL_PRICE == source.getPromotionType()) {
            typeSelect.setItems(withSpecialPrice(SELECTABLE_TYPES));
        }
        binder.readBean(source);
        resetSelections();

        // Restore schedules
        restoreSchedules(source);

        // Restore targets
        restoreTargets(source);

        // Restore special prices
        if (ObjectUtils.isNotEmpty(source.getSpecialPrices())) {
            specialPrices.addAll(source.getSpecialPrices());
            specialPriceGrid.setItems(specialPrices);
        }
    }

    private void restoreSchedules(PromotionDto source) {
        if (ObjectUtils.isNotEmpty(source.getSchedules())) {
            setSelectedDays(scheduledDays(source.getSchedules()));

            PromotionScheduleDto firstSchedule = source.getSchedules().stream()
                    .filter(PromotionForm::isActiveSchedule)
                    .findFirst().orElse(null);
            if (firstSchedule != null) {
                startTimePicker.setValue(firstSchedule.getStartTime());
                endTimePicker.setValue(firstSchedule.getEndTime());
            }
        }
    }

    private void restoreTargets(PromotionDto source) {
        restoreScope(source);
        if (ObjectUtils.isEmpty(source.getTargets())) {
            return;
        }
        PromotionTargetDto firstTarget = source.getTargets().getFirst();
        if (firstTarget == null) {
            return;
        }
        applyToSelect.setItems(restorableTargetTypes(source.getTargets()));
        applyToSelect.setValue(firstTarget.getTargetType());
        updateTargetSelect(firstTarget.getTargetType());
        source.getTargets().forEach(this::selectTarget);
        updateTargetButtonText(firstTarget.getTargetType());
        resolveRestoredTargetNames(source);
    }

    /**
     * Looks up display names for targets restored from a saved promotion.
     *
     * <p>A restored target is rebuilt from its id alone, because that is all the saved
     * row carries, so without this the preview fell back to printing the id: opening a
     * promotion to edit it showed "Category #12" where the same promotion picked
     * through the dialogs showed a name. The names are fetched rather than read from
     * the selection lists because those hold the stubs.</p>
     *
     * <p>Categories come from one listing of the brand; products are fetched one at a
     * time, as the by-brand product listing can only be queried one category at a time
     * and a restored target does not say which category its product is in. A promotion
     * points at a handful of targets, so the one request per product is a fair trade
     * for the names.</p>
     *
     * <p>Names arrive on a background thread, so each is applied on the UI thread and
     * the preview is repainted once the last one lands. A failed lookup is left out
     * rather than reported: the preview degrades to showing the id, which is what it
     * did before, and a name is not worth interrupting an edit for.</p>
     *
     * @param source the saved promotion being restored
     */
    private void resolveRestoredTargetNames(PromotionDto source) {
        if (asyncRestClientMenuService == null) {
            return;
        }
        List<Long> categoryIds = new ArrayList<>();
        List<Long> productIds = new ArrayList<>();
        for (PromotionTargetDto target : source.getTargets()) {
            if (target == null) {
                continue;
            }
            PromotionTargetType targetType = target.getTargetType();
            if (targetType == null) {
                continue;
            }
            switch (targetType) {
                case CATEGORY -> addIfAbsent(categoryIds, target.getCategoryId());
                case PRODUCT -> addIfAbsent(productIds, target.getProductId());
                case SKU -> {
                    // A SKU is already named from its own id by buildTargetNames.
                }
            }
        }

        if (!categoryIds.isEmpty()) {
            Integer brandId = targetBrandId();
            if (brandId != null) {
                asyncRestClientMenuService.getAllCategoryAsync(
                        categories -> UiUtil.safeAccess(ui, () ->
                                applyRestoredNames(PromotionTargetType.CATEGORY, categories, categoryIds)),
                        error -> log.debug("Target category names unavailable for promotion {}",
                                source.getId(), error), brandId);
            }
        }

        for (Long productId : productIds) {
            asyncRestClientMenuService.getProductAsync(
                    product -> UiUtil.safeAccess(ui, () ->
                            applyRestoredName(PromotionTargetType.PRODUCT, productId, product.getName())),
                    error -> log.debug("Target product name unavailable for promotion {}",
                            source.getId(), error), productId.intValue());
        }
    }

    private static void addIfAbsent(List<Long> ids, Long id) {
        if (id != null && !ids.contains(id)) {
            ids.add(id);
        }
    }

    /**
     * Applies the names of restored targets to the preview.
     *
     * <p>Only the targets whose ids were restored are updated, because the others are
     * already named from the selection lists.</p>
     *
     * @param type      the type of target being named
     * @param categories the categories fetched for the brand
     * @param wantedIds  the ids of targets that were restored from a saved promotion
     */
    private void applyRestoredNames(PromotionTargetType type, List<CategoryDto> categories,
                                    List<Long> wantedIds) {
        if (categories == null) {
            return;
        }
        for (CategoryDto category : categories) {
            if (category != null && category.getId() != null
                    && wantedIds.contains(category.getId().longValue())) {
                applyRestoredName(type, category.getId().longValue(), category.getName());
            }
        }
        firePreviewUpdate();
    }

    private void applyRestoredName(PromotionTargetType type, Long refId, String name) {
        if (refId == null || name == null || name.isBlank()) {
            return;
        }
        restoredTargetNames.put(PromotionPreview.targetKey(type, refId), name);
        firePreviewUpdate();
    }

    /**
     * Restores the scope the promotion was saved with, and parks the organization it was
     * narrowed by.
     *
     * <p>The scope is read off the promotion, not off its targets. It used to be taken
     * from each target in turn, so a promotion saved without it on the rows came back as
     * {@code ALL_STORES}, and one that had it came back only if every target agreed on
     * the same value.</p>
     *
     * <p>The brand, chain and store ids are only parked, not put into the fields: the
     * fields can hold a value that is one of their own options, and those are still being
     * fetched. {@link #loadBrands()} and the callbacks behind it put them back.</p>
     *
     * @param source the saved promotion
     */
    private void restoreScope(PromotionDto source) {
        PromotionScopeType scope = restorableScope(source);
        parkPendingScope(source);
        scopeSelect.setValue(scope);
        applyScopeFieldVisibility(scope);
    }

    /**
     * The scope to show for a saved promotion.
     *
     * <p>Falls back to the default for the two cases the field cannot show as saved: a
     * promotion with no scope, and one saved as {@code ALL_STORES} before that scope was
     * withdrawn from the selector.</p>
     *
     * @param source the saved promotion
     * @return the scope to restore
     */
    private static PromotionScopeType restorableScope(PromotionDto source) {
        PromotionScopeType saved = source.getScope();
        if (saved == null || !SELECTABLE_SCOPES.contains(saved)) {
            return DEFAULT_SCOPE;
        }
        return saved;
    }

    /**
     * Remembers the organization a saved promotion was narrowed by.
     *
     * <p>The ids ride on the target rows rather than on the promotion, and every row of a
     * promotion carries the same ones. They are read off the first row that has them, so
     * a promotion written elsewhere - which may have left some rows bare - still restores
     * the narrowing it was saved with.</p>
     *
     * @param source the saved promotion
     */
    private void parkPendingScope(PromotionDto source) {
        pendingBrandId = null;
        pendingChainId = null;
        pendingStoreIds.clear();
        if (ObjectUtils.isEmpty(source.getTargets())) {
            return;
        }
        source.getTargets().stream()
                .filter(Objects::nonNull)
                .forEach(target -> {
                    pendingBrandId = firstNonNull(pendingBrandId, target.getBrandId());
                    pendingChainId = firstNonNull(pendingChainId, target.getChainId());
                    if (target.getStoreIds() != null) {
                        target.getStoreIds().stream()
                                .filter(Objects::nonNull)
                                .forEach(pendingStoreIds::add);
                    }
                });
    }

    /**
     * Re-applies the parked stores once the chain's stores have loaded.
     *
     * <p>Stores the chain no longer has are dropped rather than restored: a field cannot
     * hold a store that is not among its own options, and a deleted store must not leave
     * a promotion pointing at nothing.</p>
     */
    private void restorePendingStores() {
        if (pendingStoreIds.isEmpty()) {
            return;
        }
        Set<StoreDto> available = storeSelect.getListDataView().getItems()
                .filter(store -> store.getId() != null
                        && pendingStoreIds.contains(store.getId().longValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!available.isEmpty()) {
            storeSelect.setValue(available);
        }
        pendingStoreIds.clear();
    }

    /**
     * Keeps the value already parked in preference to a later one.
     *
     * @param current the value parked so far
     * @param candidate the value just offered
     * @return whichever of the two is set
     */
    private static Long firstNonNull(Long current, Long candidate) {
        return current != null ? current : candidate;
    }

    /**
     * Turns one saved target row back into the selection it came from.
     *
     * <p>Only the id is restored, because that is all the payload carried; the names
     * shown beside the target button are refilled by the picker.</p>
     *
     * @param target the saved row
     */
    private void selectTarget(PromotionTargetDto target) {
        if (target == null) {
            return;
        }
        switch (target.getTargetType()) {
            case CATEGORY -> addSelectedCategory(target.getCategoryId());
            case PRODUCT -> addSelectedProduct(target.getProductId());
            case SKU -> addSelectedSku(target.getSkuId());
            case null -> {
                // A row with no type names nothing, so there is nothing to restore.
            }
        }
    }

    private void addSelectedCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        CategoryDto category = new CategoryDto();
        category.setId(categoryId.intValue());
        selectedCategories.add(category);
    }

    private void addSelectedProduct(Long productId) {
        if (productId == null) {
            return;
        }
        ProductDto product = new ProductDto();
        product.setId(productId.intValue());
        selectedProducts.add(product);
    }

    private void addSelectedSku(Long skuId) {
        if (skuId != null) {
            selectedSkuIds.add(skuId);
        }
    }

    /**
     * Empties every child collection the form owns. Called before {@link #restoreBean()}
     * rebuilds them, because {@code onAttach} runs again on every re-attach and the
     * restore would otherwise append to the previous state.
     */
    private void resetSelections() {
        selectedDays.clear();
        selectedCategories.clear();
        selectedProducts.clear();
        selectedSkuIds.clear();
        restoredTargetNames.clear();
        specialPrices.clear();
        specialPriceGrid.setItems(specialPrices);
    }

    /**
     * Reads the days a saved promotion is scheduled on.
     *
     * @param schedules the schedule rows returned for the promotion
     * @return the live days, empty when every row is explicitly disabled
     */
    private static Set<DayOfWeek> scheduledDays(List<PromotionScheduleDto> schedules) {
        return schedules.stream()
                .filter(PromotionForm::isActiveSchedule)
                .map(PromotionScheduleDto::getDayOfWeek)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }

    /**
     * Whether a schedule row counts towards the promotion.
     *
     * <p>A row counts unless it explicitly says it is disabled. The flag documents a
     * default of {@code true} and this form only ever writes {@code true}, so a row
     * that arrives without the flag is a live window rather than a disabled one -
     * reading it as disabled would blank the whole schedule of a promotion that
     * plainly has one.</p>
     */
    private static boolean isActiveSchedule(PromotionScheduleDto schedule) {
        return !Boolean.FALSE.equals(schedule.getEnabled());
    }

    /**
     * Applies the form-level restrictions of the given action. The action buttons
     * themselves live in {@link PromotionFormWithPreview}, so only the editable
     * state of the fields is handled here.
     *
     * <p>In {@code STATUS} mode everything is locked except the lifecycle selector,
     * which is the single field the operator is allowed to change; the footer then
     * submits it through {@code PromotionStatusEventListener}.</p>
     *
     * @param formAction the action the form was opened for
     */
    public void restructureButton(FormAction formAction) {
        if (formAction == FormAction.STATUS) {
            setFieldsReadOnly(true);
            setSpecialPriceVisible(false);
            statusSelect.setVisible(true);
            statusSelect.setEnabled(true);
        } else {
            statusSelect.setVisible(false);
        }
    }

    /**
     * Makes the form fields read-only or editable.
     *
     * <p>Does not affect the action buttons, which live in {@link PromotionFormWithPreview}.</p>
     *
     * @param readOnly whether to make the fields read-only
     */
    public void setFieldsReadOnly(boolean readOnly) {
        nameField.setReadOnly(readOnly);
        codeField.setReadOnly(readOnly);
        descriptionField.setReadOnly(readOnly);
        typeSelect.setEnabled(!readOnly);
        priorityField.setReadOnly(readOnly);
        startDatePicker.setEnabled(!readOnly);
        endDatePicker.setEnabled(!readOnly);
        stackableCheckbox.setReadOnly(readOnly);
        discountValueField.setReadOnly(readOnly);
        addSpecialPriceButton.setVisible(!readOnly);
        dayCheckboxes.values().forEach(checkbox -> checkbox.setReadOnly(readOnly));
        startTimePicker.setEnabled(!readOnly);
        endTimePicker.setEnabled(!readOnly);
        applyToSelect.setEnabled(!readOnly);
        scopeSelect.setEnabled(!readOnly);
        brandSelect.setEnabled(!readOnly);
        chainSelect.setEnabled(!readOnly);
        storeSelect.setEnabled(!readOnly);
        targetSelectButton.setEnabled(!readOnly);
        activeCheckbox.setReadOnly(readOnly);
    }

    /**
     * Builds the promotion payload from the form fields.
     *
     * <p>The payload is a single object, but the form has multiple child collections
     * that are not bound to the bean. This method reads those and adds them to the
     * payload.</p>
     *
     * @return a promotion DTO with all fields and child collections filled in
     */
    public PromotionDto buildAggregate() {
        PromotionDto promotion = new PromotionDto();
        binder.writeBeanIfValid(promotion);

        if (promotionDto != null) {
            // The id identifies the row to replace, and the rules are owned by a
            // separate endpoint the form does not edit, so both are carried over
            // rather than resubmitted as null.
            promotion.setId(promotionDto.getId());
            promotion.setRules(promotionDto.getRules());
        }

        promotion.setName(nameField.getValue());
        promotion.setCode(codeField.getValue());
        promotion.setDescription(descriptionField.getValue());
        promotion.setPriority(priorityField.getValue());
        promotion.setStartDate(startDatePicker.getValue());
        promotion.setEndDate(endDatePicker.getValue());
        promotion.setStackable(stackableCheckbox.getValue());
        promotion.setPromotionType(typeSelect.getValue());
        promotion.setStatus(statusSelect.getValue());
        promotion.setDiscountValue(discountValueField.getValue());
        promotion.setActive(activeCheckbox.getValue());
        promotion.setStartTime(startTimePicker.getValue());
        promotion.setEndTime(endTimePicker.getValue());
        promotion.setApplyToType(applyToSelect.getValue());
        promotion.setScope(scopeSelect.getValue());

        List<PromotionScheduleDto> scheduleList = selectedDays.stream()
                .map(day -> PromotionScheduleDto.builder()
                        .dayOfWeek(day)
                        .startTime(startTimePicker.getValue())
                        .endTime(endTimePicker.getValue())
                        .enabled(true)
                        .build())
                .collect(Collectors.toList());
        promotion.setSchedules(scheduleList);

        // Build targets from selected categories/products/skus. The list is always
        // submitted, because the backend reads an empty list as "clear these rows"
        // and an omitted list as "leave them alone" - so skipping it here would make
        // it impossible for an operator to detach a promotion from its targets.
        promotion.setTargets(buildTargets(applyToSelect.getValue()));

        if (PromotionType.SPECIAL_PRICE == promotion.getPromotionType()) {
            promotion.setSpecialPrices(new ArrayList<>(specialPrices));
        }

        return promotion;
    }

    /**
     * Turns the current target selection into the list sent to the backend.
     *
     * @param applyTo which kind of target the form is currently selecting
     * @return one target per selected category, product or SKU, empty when nothing is
     *         selected
     */
    private List<PromotionTargetDto> buildTargets(PromotionTargetType applyTo) {
        if (applyTo == null) {
            return new ArrayList<>();
        }
        return switch (applyTo) {
            case CATEGORY -> selectedCategories.stream()
                    .filter(category -> category.getId() != null)
                    .map(category -> scopedTarget(applyTo).categoryId(category.getId().longValue()).build())
                    .collect(Collectors.toCollection(ArrayList::new));
            case PRODUCT -> selectedProducts.stream()
                    .filter(product -> product.getId() != null)
                    .map(product -> scopedTarget(applyTo).productId(product.getId().longValue()).build())
                    .collect(Collectors.toCollection(ArrayList::new));
            case SKU -> selectedSkuIds.stream()
                    .map(skuId -> scopedTarget(applyTo).skuId(skuId).build())
                    .collect(Collectors.toCollection(ArrayList::new));
        };
    }

    /**
     * Starts a target row with the scope narrowing on it.
     *
     * <p>Every target of a promotion shares one scope, so it is stamped here rather than
     * repeated per branch. Each branch then only supplies the one id that makes its row
     * a category, a product or a SKU.</p>
     *
     * @param applyTo the kind of target being built
     * @return a builder pre-filled with the target type and the scope narrowing
     */
    private PromotionTargetDto.PromotionTargetDtoBuilder scopedTarget(PromotionTargetType applyTo) {
        return PromotionTargetDto.builder()
                .targetType(applyTo)
                .scope(scopeSelect.getValue())
                .brandId(selectedBrandId())
                .chainId(selectedChainId())
                .storeIds(selectedStoreIds());
    }

    public boolean isAggregateValid() {
        if (binder.validate().hasErrors()) {
            UiUtil.error(Messages.get("validation.promotion.formErrors"));
            return false;
        }
        PromotionDto draft = new PromotionDto();
        binder.writeBeanIfValid(draft);

        boolean needsDiscount = PromotionType.PERCENTAGE == draft.getPromotionType()
                || PromotionType.FIXED_AMOUNT == draft.getPromotionType();
        if (needsDiscount && (draft.getDiscountValue() == null || draft.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0)) {
            UiUtil.error(Messages.get("validation.promotion.discountRequired"));
            return false;
        }

        if (PromotionType.SPECIAL_PRICE == draft.getPromotionType() && specialPrices.isEmpty()) {
            UiUtil.error(Messages.get("validation.promotion.specialPriceRequired"));
            return false;
        }

        if (startTimePicker.getValue() == null || endTimePicker.getValue() == null) {
            UiUtil.error(Messages.get("validation.promotion.timeRequired"));
            return false;
        }

        if (selectedDays.isEmpty()) {
            UiUtil.error(Messages.get("validation.promotion.daysRequired"));
            return false;
        }

        return true;
    }

    /**
     * Shows the success toast and closes the tab. Called from the REST subscribe
     * callback, which runs on a background thread, so both updates are pushed to
     * the UI thread: a Notification needs a current UI to attach to.
     */
    public void onSaveSuccess() {
        UiUtil.safeAccess(ui, () -> {
            UiUtil.success(Messages.get("notification.promotion.saved"));
            closeTab();
        });
    }

    /**
     * Reports a failed save. Called from the REST subscribe callback, so the toast
     * is pushed to the UI thread.
     *
     * @param error the failure reported by the REST client
     */
    public void onSaveError(Throwable error) {
        log.error("Promotion save failed", error);
        UiUtil.safeAccess(ui, () -> UiUtil.error(Messages.get("notification.promotion.saveFailed")));
    }

    public void close() {
        UiUtil.safeAccess(ui, this::closeTab);
    }

    /**
     * Closes the hosting tab. Runs on the UI thread, so callers that came from a
     * background thread must wrap it themselves.
     */
    private void closeTab() {
        tabManager.closeAndSelectFirst(currentTab);
    }
}
