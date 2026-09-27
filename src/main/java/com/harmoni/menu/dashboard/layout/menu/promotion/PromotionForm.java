package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.dto.PromotionScheduleDto;
import com.harmoni.menu.dashboard.dto.PromotionSpecialPriceDto;
import com.harmoni.menu.dashboard.dto.PromotionTargetDto;
import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.enums.PromotionStatus;
import com.harmoni.menu.dashboard.layout.enums.PromotionTargetType;
import com.harmoni.menu.dashboard.layout.enums.PromotionType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientPromotionService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
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
    private final TabManager tabManager;
    private final Tab currentTab;
    private final FormAction formAction;
    private final AccessService accessService;

    @Getter
    private final transient PromotionDto promotionDto;

    private final List<PromotionScheduleDto> schedules = new ArrayList<>();
    private final List<PromotionTargetDto> targets = new ArrayList<>();
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
     * The selectable types plus the withdrawn one, for a promotion that is already using it.
     *
     * @param types the types an operator may pick
     * @return those types with {@code SPECIAL_PRICE} added, in that order
     */
    private static List<PromotionType> withSpecialPrice(List<PromotionType> types) {
        List<PromotionType> all = new ArrayList<>(types);
        all.add(PromotionType.SPECIAL_PRICE);
        return all;
    }

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

    private BiConsumer<PromotionDto, Map<String, String>> previewUpdater;

    public PromotionForm(RestClientPromotionService restClientPromotionService,
                         AsyncRestClientMenuService asyncRestClientMenuService,
                         TabManager tabManager,
                         Tab currentTab,
                         FormAction formAction,
                         PromotionDto promotionDto,
                         AccessService accessService) {
        this.restClientPromotionService = restClientPromotionService;
        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.tabManager = tabManager;
        this.currentTab = currentTab;
        this.formAction = formAction;
        this.promotionDto = promotionDto;
        this.accessService = accessService;
    }

    public void setPreviewUpdater(BiConsumer<PromotionDto, Map<String, String>> previewUpdater) {
        this.previewUpdater = previewUpdater;
    }

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
                PromotionPreview.targetKey(PromotionTargetType.SKU, skuId), "SKU " + skuId));
        return names;
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
        firePreviewUpdate();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }

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
                .set("font-weight", "600")
                .set("color", "var(--app-text-secondary)");
        discountUnitLabel.setText("%");

        discountLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        discountLayout.setSpacing(true);

        configureDaysSelector();

        startTimePicker.setLabel(Messages.get("label.promotion.startTime"));
        startTimePicker.setStep(java.time.Duration.ofMinutes(15));
        startTimePicker.setWidth("140px");
        startTimePicker.addValueChangeListener(e -> firePreviewUpdate());

        endTimePicker.setLabel(Messages.get("label.promotion.endTime"));
        endTimePicker.setStep(java.time.Duration.ofMinutes(15));
        endTimePicker.setWidth("140px");
        endTimePicker.addValueChangeListener(e -> firePreviewUpdate());

        timeLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        timeLayout.setSpacing(true);

        scheduleTimingLayout.setWidthFull();
        scheduleTimingLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        scheduleTimingLayout.setSpacing(true);
        scheduleTimingLayout.getStyle().set("flex-wrap", "wrap");
        scheduleTimingLayout.setFlexGrow(1, timeLayout);

        applyToSelect.setLabel(Messages.get("label.promotion.applyTo"));
        applyToSelect.setItems(PromotionTargetType.CATEGORY, PromotionTargetType.PRODUCT, PromotionTargetType.SKU);
        applyToSelect.setItemLabelGenerator(PromotionTargetType::getLabel);
        applyToSelect.setWidthFull();
        applyToSelect.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                updateTargetSelect(change.getValue());
                firePreviewUpdate();
            }
        });

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

    private Component specialPriceRow(PromotionSpecialPriceDto specialPrice) {
        IntegerField skuField = new IntegerField();
        skuField.setMin(1);
        skuField.setWidth("100px");
        skuField.setValue(specialPrice.getSkuId() == null ? null : specialPrice.getSkuId().intValue());

        BigDecimalField priceField = new BigDecimalField();
        priceField.setWidth("140px");
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

    private void addSpecialPriceRow() {
        specialPrices.add(PromotionSpecialPriceDto.builder().build());
        specialPriceGrid.setItems(specialPrices);
        firePreviewUpdate();
    }

    private <T> void removeRow(List<T> rows, Grid<T> grid, T row) {
        if (rows.remove(row)) {
            grid.setItems(rows);
            firePreviewUpdate();
        }
    }

    private void applyTypeSpecificVisibility(PromotionType type) {
        boolean isPercentageOrFixed = type == PromotionType.PERCENTAGE || type == PromotionType.FIXED_AMOUNT;
        boolean isSpecialPrice = type == PromotionType.SPECIAL_PRICE;

        discountLayout.setVisible(isPercentageOrFixed);
        daysLayout.setVisible(true);
        timeLayout.setVisible(true);
        applyToSelect.setVisible(true);
        targetSelectButton.setVisible(applyToSelect.getValue() != null);
        setSpecialPriceVisible(isSpecialPrice);

        if (isPercentageOrFixed) {
            discountUnitLabel.setText(type == PromotionType.PERCENTAGE ? "%" : "Rp");
        }
    }

    private void setSpecialPriceVisible(boolean visible) {
        specialPriceLayout.setVisible(visible);
    }

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

    private void updateTargetButtonText(PromotionTargetType targetType) {
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

    private void openTargetSelectionDialog() {
        PromotionTargetType targetType = applyToSelect.getValue();
        if (targetType == null) {
            return;
        }

        Integer brandId = getBrandId();
        if (brandId == null) {
            UiUtil.error(Messages.get("error.brandNotSelected"));
            return;
        }

        switch (targetType) {
            case CATEGORY -> {
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
            case PRODUCT -> {
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
            case SKU -> {
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
        }
    }

    private Integer getBrandId() {
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
                new ResponsiveStep("600px", 2)
        );

        // GENERAL SECTION - Card
        FormLayout generalForm = new FormLayout();
        generalForm.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("600px", 2)
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
                new ResponsiveStep("600px", 2)
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
                new ResponsiveStep("600px", 2)
        );
        targetForm.add(applyToSelect);
        targetForm.add(targetSelectButton);
        targetForm.add(specialPriceLayout);
        targetForm.setColspan(specialPriceLayout, 2);

        Div targetCard = createSectionCard(Messages.get("promotion.section.target"), targetForm);
        setColspan(targetCard, 2);
        add(targetCard);
    }

    private Div createSectionCard(String title, FormLayout content) {
        Div card = new Div();
        card.addClassName("form-section-card");
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-m)");

        H3 titleLabel = new H3(title);
        titleLabel.getStyle()
                .set("margin", "0 0 var(--lumo-space-m) 0")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("font-weight", "600")
                .set("color", "var(--lumo-primary-text-color)")
                .set("padding-bottom", "var(--lumo-space-s)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        card.add(titleLabel, content);
        return card;
    }

    private void restoreBean() {
        if (isNewPromotion()) {
            typeSelect.setValue(PromotionType.PERCENTAGE);
            activeCheckbox.setValue(true);
            // Set the checkbox first: syncStatusWithActiveFlag reads it, and the value
            // is already true from configureFields, so setting it again fires nothing.
            syncStatusWithActiveFlag();
            startTimePicker.setValue(LocalTime.of(15, 0));
            endTimePicker.setValue(LocalTime.of(17, 0));
            applyToSelect.setValue(PromotionTargetType.CATEGORY);
            updateTargetSelect(PromotionTargetType.CATEGORY);
            return;
        }

        PromotionDto source = promotionDto;
        // A saved SPECIAL_PRICE promotion still has to show its own type, and the binder
        // reads the type straight into the select. A value the select does not offer is
        // not a value it can show, so the option is put back before the bean is read -
        // otherwise opening such a promotion and saving it would change its type.
        if (PromotionType.SPECIAL_PRICE == source.getPromotionType()) {
            typeSelect.setItems(withSpecialPrice(SELECTABLE_TYPES));
        }
        binder.readBean(source);

        // onAttach runs again whenever the tab is detached and re-attached, so the
        // selections are reset before being rebuilt from the bean.
        resetSelections();

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

        if (ObjectUtils.isNotEmpty(source.getTargets())) {
            PromotionTargetDto firstTarget = source.getTargets().getFirst();
            if (firstTarget != null) {
                applyToSelect.setValue(firstTarget.getTargetType());
                updateTargetSelect(firstTarget.getTargetType());

                // Populate selected targets from DTO
                PromotionTargetType targetType = firstTarget.getTargetType();
                for (PromotionTargetDto target : source.getTargets()) {
                    switch (targetType) {
                        case CATEGORY -> {
                            if (target.getCategoryId() != null) {
                                CategoryDto category = new CategoryDto();
                                category.setId(target.getCategoryId().intValue());
                                selectedCategories.add(category);
                            }
                        }
                        case PRODUCT -> {
                            if (target.getProductId() != null) {
                                ProductDto product = new ProductDto();
                                product.setId(target.getProductId().intValue());
                                selectedProducts.add(product);
                            }
                        }
                        case SKU -> {
                            if (target.getSkuId() != null) {
                                selectedSkuIds.add(target.getSkuId());
                            }
                        }
                    }
                }
                updateTargetButtonText(targetType);
            }
        }

        if (ObjectUtils.isNotEmpty(source.getSpecialPrices())) {
            specialPrices.addAll(source.getSpecialPrices());
            specialPriceGrid.setItems(specialPrices);
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
        targetSelectButton.setEnabled(!readOnly);
        activeCheckbox.setReadOnly(readOnly);
    }

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
     * @return one target per selected category, product or SKU, empty when nothing
     *         is selected
     */
    private List<PromotionTargetDto> buildTargets(PromotionTargetType applyTo) {
        List<PromotionTargetDto> targets = new ArrayList<>();
        if (applyTo == null) {
            return targets;
        }
        switch (applyTo) {
            case CATEGORY -> selectedCategories.stream()
                    .filter(category -> category.getId() != null)
                    .map(category -> PromotionTargetDto.builder()
                            .targetType(PromotionTargetType.CATEGORY)
                            .categoryId(category.getId().longValue())
                            .build())
                    .forEach(targets::add);
            case PRODUCT -> selectedProducts.stream()
                    .filter(product -> product.getId() != null)
                    .map(product -> PromotionTargetDto.builder()
                            .targetType(PromotionTargetType.PRODUCT)
                            .productId(product.getId().longValue())
                            .build())
                    .forEach(targets::add);
            case SKU -> selectedSkuIds.stream()
                    .map(skuId -> PromotionTargetDto.builder()
                            .targetType(PromotionTargetType.SKU)
                            .skuId(skuId)
                            .build())
                    .forEach(targets::add);
        }
        return targets;
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
