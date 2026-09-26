package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.dto.PromotionRuleDto;
import com.harmoni.menu.dashboard.dto.PromotionScheduleDto;
import com.harmoni.menu.dashboard.dto.PromotionSpecialPriceDto;
import com.harmoni.menu.dashboard.dto.PromotionTargetDto;
import com.harmoni.menu.dashboard.event.promotion.PromotionDeleteEventListener;
import com.harmoni.menu.dashboard.event.promotion.PromotionSaveEventListener;
import com.harmoni.menu.dashboard.event.promotion.PromotionStatusEventListener;
import com.harmoni.menu.dashboard.event.promotion.PromotionUpdateEventListener;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.enums.PromotionRuleType;
import com.harmoni.menu.dashboard.layout.enums.PromotionStatus;
import com.harmoni.menu.dashboard.layout.enums.PromotionTargetType;
import com.harmoni.menu.dashboard.layout.enums.PromotionType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientPromotionService;
import com.harmoni.menu.dashboard.util.Messages;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Tab form for creating, editing or re-statusing a {@link PromotionDto}.
 *
 * <p>
 * The aggregate carries four child collections, so the editor is split into an
 * inner {@link TabSheet}: the main form holds the scalar attributes, and the
 * remaining tabs hold editable grids for the weekly schedules, the targets, the
 * rules and the special prices. Every grid is edited in memory and flushed to the
 * bean on save, so a single POST or PUT carries the whole aggregate.
 * </p>
 */
@RequiredArgsConstructor
@Slf4j
public class PromotionForm extends FormLayout {

    Registration broadcasterRegistration;

    @Getter
    private final BeanValidationBinder<PromotionDto> binder = new BeanValidationBinder<>(PromotionDto.class);

    @Getter
    private final TextField codeField = new TextField(Messages.get("label.promotion.code"));

    @Getter
    private final TextField nameField = new TextField(Messages.get(Messages.Keys.LABEL_NAME));

    @Getter
    private final TextArea descriptionArea = new TextArea(Messages.get(Messages.Keys.LABEL_DESCRIPTION));

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

    private final Grid<PromotionScheduleDto> scheduleGrid = new Grid<>(PromotionScheduleDto.class);
    private final Grid<PromotionTargetDto> targetGrid = new Grid<>(PromotionTargetDto.class);
    private final Grid<PromotionRuleDto> ruleGrid = new Grid<>(PromotionRuleDto.class);
    private final Grid<PromotionSpecialPriceDto> specialPriceGrid = new Grid<>(PromotionSpecialPriceDto.class);

    private final Button saveButton = new Button(Messages.get(Messages.Keys.ACTION_SAVE));
    private final Button updateButton = new Button(Messages.get(Messages.Keys.ACTION_UPDATE));
    private final Button closeButton = new Button(Messages.get(Messages.Keys.ACTION_CANCEL));
    private final Button statusButton = new Button(Messages.get("action.applyStatus"));

    @Getter
    private UI ui;

    private final RestClientPromotionService restClientPromotionService;
    private final TabManager tabManager;
    private final Tab currentTab;
    private final FormAction formAction;

    @Getter
    private final transient PromotionDto promotionDto;

    private final List<PromotionScheduleDto> schedules = new ArrayList<>();
    private final List<PromotionTargetDto> targets = new ArrayList<>();
    private final List<PromotionRuleDto> rules = new ArrayList<>();
    private final List<PromotionSpecialPriceDto> specialPrices = new ArrayList<>();

    private TabSheet childTabs;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        registerBroadcastListener();
        configureFields();
        configureGrids();
        addValidation();
        addFields();
        addChildTabs();
        binder.bindInstanceFields(this);
        restoreBean();
        addFooterButtons();
        restructureButton(formAction);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }

    private void registerBroadcastListener() {
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcast =
                        (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isEmpty(broadcast) || ObjectUtils.isEmpty(broadcast.getType())) {
                    return;
                }
                if (BroadcastMessage.PROMOTION_INSERT_SUCCESS.equals(broadcast.getType())
                        || BroadcastMessage.PROMOTION_UPDATED_SUCCESS.equals(broadcast.getType())) {
                    UiUtil.safeAccess(ui, this::handleSavedElsewhere);
                }
            } catch (JsonProcessingException e) {
                log.error("Broadcast handler error", e);
            }
        });
    }

    /**
     * Closes this tab when the same promotion was saved by another session, so the
     * operator is not left editing a stale copy.
     */
    private void handleSavedElsewhere() {
        UiUtil.success(Messages.get("notification.promotion.saved"));
        close();
    }

    private void configureFields() {
        descriptionArea.setMaxLength(500);
        typeSelect.setItems(PromotionType.values());
        typeSelect.setItemLabelGenerator(PromotionType::getLabel);
        statusSelect.setItems(PromotionStatus.values());
        statusSelect.setItemLabelGenerator(PromotionStatus::getLabel);
        priorityField.setMin(0);
        priorityField.setMax(999);
        priorityField.setValue(0);
        startDatePicker.setPlaceholder("dd/mm/yyyy");
        endDatePicker.setPlaceholder("dd/mm/yyyy");
        Set<Component> typingFields = new HashSet<>(Set.of(codeField, nameField, descriptionArea));
        UiUtil.guardShortcutField(typingFields, codeField);
        UiUtil.guardShortcutField(typingFields, nameField);
        UiUtil.guardShortcutField(typingFields, descriptionArea);

        typeSelect.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                applyTypeSpecificVisibility(change.getValue());
            }
        });
    }

    private void applyTypeSpecificVisibility(PromotionType type) {
        boolean isSpecialPrice = PromotionType.SPECIAL_PRICE == type;
        specialPriceGrid.setVisible(isSpecialPrice);
    }

    private void configureGrids() {
        configureScheduleGrid();
        configureTargetGrid();
        configureRuleGrid();
        configureSpecialPriceGrid();
    }

    /**
     * Renders the weekly windows. Each row owns its own editor, so editing one
     * window never disturbs another.
     */
    private void configureScheduleGrid() {
        scheduleGrid.setSizeFull();
        scheduleGrid.removeAllColumns();
        scheduleGrid.addComponentColumn(this::scheduleRow)
                .setHeader(Messages.get("grid.header.window"));
        scheduleGrid.addComponentColumn(grid -> new HorizontalLayout(
                        newButton(Messages.get("action.addSchedule"), event -> addScheduleRow())))
                .setWidth("130px");
        scheduleGrid.setItems(schedules);
    }

    private Component scheduleRow(PromotionScheduleDto schedule) {
        Select<DayOfWeek> daySelect = new Select<>();
        daySelect.setItems(DayOfWeek.values());
        daySelect.setItemLabelGenerator(day -> Messages.get("promotion.day." + day.name()));
        daySelect.setValue(schedule.getDayOfWeek());
        daySelect.setWidth("140px");

        TimePicker startPicker = new TimePicker();
        startPicker.setValue(schedule.getStartTime());
        startPicker.setStep(Duration.ofMinutes(15));
        startPicker.setWidth("120px");

        TimePicker endPicker = new TimePicker();
        endPicker.setValue(schedule.getEndTime());
        endPicker.setStep(Duration.ofMinutes(15));
        endPicker.setWidth("120px");

        Checkbox enabled = new Checkbox(Messages.get("label.promotion.enabled"));
        enabled.setValue(schedule.getEnabled() == null || schedule.getEnabled());

        Runnable sync = () -> {
            schedule.setDayOfWeek(daySelect.getValue());
            schedule.setStartTime(startPicker.getValue());
            schedule.setEndTime(endPicker.getValue());
            schedule.setEnabled(enabled.getValue());
        };
        daySelect.addValueChangeListener(change -> sync.run());
        startPicker.addValueChangeListener(change -> sync.run());
        endPicker.addValueChangeListener(change -> sync.run());
        enabled.addValueChangeListener(change -> sync.run());

        return new HorizontalLayout(daySelect, startPicker, endPicker, enabled,
                UiUtil.deleteButton(event -> removeRow(schedules, scheduleGrid, schedule)));
    }

    private void addScheduleRow() {
        PromotionScheduleDto schedule = PromotionScheduleDto.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .enabled(true)
                .build();
        schedules.add(schedule);
        scheduleGrid.setItems(schedules);
    }

    /**
     * Renders the products, SKUs and categories the promotion applies to.
     */
    private void configureTargetGrid() {
        targetGrid.setSizeFull();
        targetGrid.removeAllColumns();
        targetGrid.addComponentColumn(this::targetRow)
                .setHeader(Messages.get("grid.header.target"));
        targetGrid.addComponentColumn(grid -> new HorizontalLayout(
                        newButton(Messages.get("action.addTarget"), event -> addTargetRow())))
                .setWidth("130px");
        targetGrid.setItems(targets);
    }

    private Component targetRow(PromotionTargetDto target) {
        Select<PromotionTargetType> typeSelect = new Select<>();
        typeSelect.setItems(PromotionTargetType.values());
        typeSelect.setItemLabelGenerator(PromotionTargetType::getLabel);
        typeSelect.setValue(target.getTargetType());
        typeSelect.setWidth("160px");

        IntegerField referenceField = new IntegerField();
        referenceField.setMin(1);
        referenceField.setWidth("120px");
        referenceField.setValue(existingReferenceId(target));

        Runnable sync = () -> applyReference(target, typeSelect.getValue(), referenceField.getValue());
        typeSelect.addValueChangeListener(change -> sync.run());
        referenceField.addValueChangeListener(change -> sync.run());

        return new HorizontalLayout(typeSelect, referenceField,
                UiUtil.deleteButton(event -> removeRow(targets, targetGrid, target)));
    }

    private void addTargetRow() {
        targets.add(PromotionTargetDto.builder().targetType(PromotionTargetType.SKU).build());
        targetGrid.setItems(targets);
    }

    /**
     * Creates the "add" button that sits in the last column of a child grid.
     *
     * @param label    the button caption
     * @param onClick  what to add
     * @return a tertiary inline plus button
     */
    private static Button newButton(String label,
                                    ComponentEventListener<ClickEvent<Button>> onClick) {
        Button button = new Button(label, VaadinIcon.PLUS.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        button.addClickListener(onClick);
        return button;
    }

    /**
     * Drops a row from an in-memory child collection and refreshes its grid.
     *
     * @param rows  the backing list
     * @param grid  the grid to refresh
     * @param row   the row to remove
     * @param <T>   the row type
     */
    private <T> void removeRow(List<T> rows, Grid<T> grid, T row) {
        if (rows.remove(row)) {
            grid.setItems(rows);
        }
    }

    /**
     * Reads whichever reference id the target's type designates, so a partially
     * filled new row shows a blank box rather than a misleading zero.
     */
    private Integer existingReferenceId(PromotionTargetDto target) {
        Long reference = referenceIdOf(target);
        return reference == null ? null : reference.intValue();
    }

    private Long referenceIdOf(PromotionTargetDto target) {
        if (target == null || target.getTargetType() == null) {
            return null;
        }
        return switch (target.getTargetType()) {
            case PRODUCT -> target.getProductId();
            case SKU -> target.getSkuId();
            case CATEGORY -> target.getCategoryId();
        };
    }

    /**
     * Writes the entered id into the single field the chosen type designates and
     * clears the other two, which is what the menu service validates against.
     */
    private void applyReference(PromotionTargetDto target, PromotionTargetType type, Integer referenceId) {
        target.setTargetType(type);
        target.setProductId(null);
        target.setSkuId(null);
        target.setCategoryId(null);
        if (type == null || referenceId == null) {
            return;
        }
        long reference = referenceId.longValue();
        switch (type) {
            case PRODUCT -> target.setProductId(reference);
            case SKU -> target.setSkuId(reference);
            case CATEGORY -> target.setCategoryId(reference);
        }
    }

    private PromotionTargetDto toTarget(PromotionTargetType targetType, long referenceId) {
        PromotionTargetDto target = PromotionTargetDto.builder().targetType(targetType).build();
        applyReference(target, targetType, (int) referenceId);
        return target;
    }

    /**
     * Renders the constraints a basket must satisfy.
     */
    private void configureRuleGrid() {
        ruleGrid.setSizeFull();
        ruleGrid.removeAllColumns();
        ruleGrid.addComponentColumn(this::ruleRow)
                .setHeader(Messages.get("grid.header.rule"));
        ruleGrid.addComponentColumn(grid -> new HorizontalLayout(
                        newButton(Messages.get("action.addRule"), event -> addRuleRow())))
                .setWidth("130px");
        ruleGrid.setItems(rules);
    }

    private Component ruleRow(PromotionRuleDto rule) {
        Select<PromotionRuleType> typeSelect = new Select<>();
        typeSelect.setItems(PromotionRuleType.values());
        typeSelect.setItemLabelGenerator(PromotionRuleType::getLabel);
        typeSelect.setValue(rule.getRuleType());
        typeSelect.setWidth("200px");

        BigDecimalField valueField = new BigDecimalField();
        valueField.setWidth("160px");
        valueField.setValue(ruleValue(rule));

        Runnable sync = () -> applyRuleValue(rule, typeSelect.getValue(), valueField.getValue());
        typeSelect.addValueChangeListener(change -> sync.run());
        valueField.addValueChangeListener(change -> sync.run());

        return new HorizontalLayout(typeSelect, valueField,
                UiUtil.deleteButton(event -> removeRow(rules, ruleGrid, rule)));
    }

    private void addRuleRow() {
        rules.add(PromotionRuleDto.builder().ruleType(PromotionRuleType.PERCENTAGE).build());
        ruleGrid.setItems(rules);
    }

    private BigDecimal ruleValue(PromotionRuleDto rule) {
        if (rule == null || rule.getRuleType() == null) {
            return null;
        }
        return switch (rule.getRuleType()) {
            case PERCENTAGE, FIXED_AMOUNT -> rule.getDiscountValue();
            case MAX_DISCOUNT_AMOUNT -> rule.getMaxDiscountAmount();
            case MIN_QUANTITY -> rule.getMinQuantity();
            case MIN_AMOUNT -> rule.getMinAmount();
        };
    }

    private void applyRuleValue(PromotionRuleDto rule, PromotionRuleType ruleType, BigDecimal value) {
        rule.setRuleType(ruleType);
        rule.setDiscountValue(null);
        rule.setMaxDiscountAmount(null);
        rule.setMinQuantity(null);
        rule.setMinAmount(null);
        if (ruleType == null || value == null) {
            return;
        }
        switch (ruleType) {
            case PERCENTAGE, FIXED_AMOUNT -> rule.setDiscountValue(value);
            case MAX_DISCOUNT_AMOUNT -> rule.setMaxDiscountAmount(value);
            case MIN_QUANTITY -> rule.setMinQuantity(value);
            case MIN_AMOUNT -> rule.setMinAmount(value);
        }
    }

    private PromotionRuleDto toRule(PromotionRuleType ruleType, BigDecimal value) {
        PromotionRuleDto rule = PromotionRuleDto.builder().ruleType(ruleType).build();
        applyRuleValue(rule, ruleType, value);
        return rule;
    }

    /**
     * Renders the replacement prices of a {@code SPECIAL_PRICE} promotion.
     */
    private void configureSpecialPriceGrid() {
        specialPriceGrid.setSizeFull();
        specialPriceGrid.removeAllColumns();
        specialPriceGrid.addComponentColumn(this::specialPriceRow)
                .setHeader(Messages.get("grid.header.specialPrice"));
        specialPriceGrid.addComponentColumn(grid -> new HorizontalLayout(
                        newButton(Messages.get("action.addSpecialPrice"), event -> addSpecialPriceRow())))
                .setWidth("130px");
        specialPriceGrid.setItems(specialPrices);
    }

    private Component specialPriceRow(PromotionSpecialPriceDto specialPrice) {
        IntegerField skuField = new IntegerField();
        skuField.setMin(1);
        skuField.setWidth("120px");
        skuField.setValue(specialPrice.getSkuId() == null ? null : specialPrice.getSkuId().intValue());

        BigDecimalField priceField = new BigDecimalField();
        priceField.setWidth("160px");
        priceField.setValue(specialPrice.getSpecialPrice());

        Runnable sync = () -> {
            specialPrice.setSkuId(skuField.getValue() == null ? null : skuField.getValue().longValue());
            specialPrice.setSpecialPrice(priceField.getValue());
        };
        skuField.addValueChangeListener(change -> sync.run());
        priceField.addValueChangeListener(change -> sync.run());

        return new HorizontalLayout(skuField, priceField,
                UiUtil.deleteButton(event -> removeRow(specialPrices, specialPriceGrid, specialPrice)));
    }

    private void addSpecialPriceRow() {
        specialPrices.add(PromotionSpecialPriceDto.builder().build());
        specialPriceGrid.setItems(specialPrices);
    }

    private void addValidation() {
        binder.forField(codeField)
                .asRequired(Messages.get("validation.promotion.codeRequired"))
                .withValidator(value -> value == null || value.trim().length() >= 2,
                        Messages.get("validation.promotion.codeMinLength"))
                .withValidator(this::isCodeUniqueFormat,
                        Messages.get("validation.promotion.codeFormat"))
                .bind(PromotionDto::getCode, PromotionDto::setCode);

        binder.forField(nameField)
                .asRequired(Messages.get("validation.promotion.nameRequired"))
                .withValidator(value -> value == null || value.trim().length() >= 2,
                        Messages.get("validation.promotion.nameMinLength"))
                .bind(PromotionDto::getName, PromotionDto::setName);

        binder.forField(descriptionArea)
                .bind(PromotionDto::getDescription, PromotionDto::setDescription);

        binder.forField(typeSelect)
                .asRequired(Messages.get("validation.promotion.typeRequired"))
                .bind(PromotionDto::getPromotionType, PromotionDto::setPromotionType);

        binder.forField(statusSelect)
                .asRequired(Messages.get("validation.promotion.statusRequired"))
                .bind(PromotionDto::getStatus, PromotionDto::setStatus);

        binder.forField(priorityField)
                .asRequired(Messages.get("validation.promotion.priorityRequired"))
                .bind(PromotionDto::getPriority, PromotionDto::setPriority);

        binder.forField(startDatePicker)
                .bind(PromotionDto::getStartDate, PromotionDto::setStartDate);

        binder.forField(endDatePicker)
                .withValidator(this::isDateRangeOrdered,
                        Messages.get("validation.promotion.dateRange"))
                .bind(PromotionDto::getEndDate, PromotionDto::setEndDate);
    }

    private boolean isCodeUniqueFormat(String value) {
        return value == null || value.trim().matches("[A-Za-z0-9_-]+");
    }

    private boolean isDateRangeOrdered(LocalDate endDate) {
        LocalDate startDate = startDatePicker.getValue();
        return startDate == null || endDate == null || !startDate.isAfter(endDate);
    }

    private void addFields() {
        setResponsiveSteps(new ResponsiveStep[]{
                new ResponsiveStep("0", 1),
                new ResponsiveStep("32em", 2)
        });
        add(codeField, nameField, typeSelect, statusSelect, priorityField, startDatePicker, endDatePicker);
        add(descriptionArea);
        descriptionArea.setWidthFull();
    }

    private void addChildTabs() {
        childTabs = new TabSheet();
        childTabs.setSizeFull();
        childTabs.add(Messages.get("tab.promotionSchedules"), wrap(scheduleGrid));
        childTabs.add(Messages.get("tab.promotionTargets"), wrap(targetGrid));
        childTabs.add(Messages.get("tab.promotionRules"), wrap(ruleGrid));
        childTabs.add(Messages.get("tab.promotionSpecialPrices"), wrap(specialPriceGrid));
        applyTypeSpecificVisibility(promotionDto == null ? null : promotionDto.getPromotionType());
        childTabs.setHeight("320px");
        childTabs.setWidthFull();
        add(childTabs);
    }

    private VerticalLayout wrap(Grid<?> grid) {
        VerticalLayout holder = new VerticalLayout(grid);
        holder.setSizeFull();
        holder.setPadding(false);
        return holder;
    }

    private void restoreBean() {
        if (formAction == FormAction.CREATE || ObjectUtils.isEmpty(promotionDto)) {
            statusSelect.setValue(PromotionStatus.DRAFT);
            return;
        }
        PromotionDto source = formAction == FormAction.STATUS
                ? shallowCopy(promotionDto)
                : promotionDto;
        if (ObjectUtils.isNotEmpty(source.getSchedules())) {
            source.getSchedules().forEach(schedule -> schedules.add(
                    PromotionScheduleDto.builder()
                            .id(schedule.getId())
                            .dayOfWeek(schedule.getDayOfWeek())
                            .startTime(schedule.getStartTime())
                            .endTime(schedule.getEndTime())
                            .enabled(schedule.getEnabled())
                            .build()));
        }
        if (ObjectUtils.isNotEmpty(source.getTargets())) {
            targets.addAll(source.getTargets());
        }
        if (ObjectUtils.isNotEmpty(source.getRules())) {
            rules.addAll(source.getRules());
        }
        if (ObjectUtils.isNotEmpty(source.getSpecialPrices())) {
            specialPrices.addAll(source.getSpecialPrices());
        }
        scheduleGrid.setItems(schedules);
        targetGrid.setItems(targets);
        ruleGrid.setItems(rules);
        specialPriceGrid.setItems(specialPrices);
        binder.readBean(source);
    }

    private PromotionDto shallowCopy(PromotionDto source) {
        return PromotionDto.builder()
                .id(source.getId())
                .code(source.getCode())
                .name(source.getName())
                .description(source.getDescription())
                .promotionType(source.getPromotionType())
                .status(source.getStatus())
                .priority(source.getPriority())
                .startDate(source.getStartDate())
                .endDate(source.getEndDate())
                .schedules(source.getSchedules())
                .targets(source.getTargets())
                .rules(source.getRules())
                .specialPrices(source.getSpecialPrices())
                .build();
    }

    private void addFooterButtons() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);
        closeButton.addClickShortcut(Key.ESCAPE);
        saveButton.addClickListener(
                new PromotionSaveEventListener(this, restClientPromotionService));
        updateButton.addClickListener(
                new PromotionUpdateEventListener(this, restClientPromotionService));
        closeButton.addClickListener(event -> close());

        HorizontalLayout footer = new HorizontalLayout(saveButton, updateButton, statusButton, closeButton);
        footer.setWidthFull();
        if (formAction == FormAction.EDIT) {
            footer.add(new PromotionDeleteEventListener(promotionDto, restClientPromotionService)
                    .asDeleteButton());
        }
        add(footer);
    }

    /**
     * Shows the footer buttons that match the mode this form was opened in. In
     * {@code STATUS} mode the scalar fields are read-only and the button applies the
     * chosen state through the dedicated status endpoint.
     *
     * @param formAction the mode the form was opened in
     */
    public void restructureButton(FormAction formAction) {
        boolean creating = Objects.requireNonNull(formAction) == FormAction.CREATE;
        boolean statusOnly = formAction == FormAction.STATUS;
        saveButton.setVisible(creating);
        updateButton.setVisible(!creating && !statusOnly);
        statusButton.setVisible(statusOnly);
        closeButton.setVisible(true);
        if (statusOnly) {
            setFieldsReadOnly(true);
            childTabs.setVisible(false);
        }
        if (statusOnly) {
            statusButton.addClickListener(
                    new PromotionStatusEventListener(this, restClientPromotionService));
        }
    }

    /**
     * Locks the configuration fields so a status change cannot silently alter the
     * promotion itself.
     *
     * @param readOnly whether the fields accept input
     */
    private void setFieldsReadOnly(boolean readOnly) {
        codeField.setReadOnly(readOnly);
        nameField.setReadOnly(readOnly);
        descriptionArea.setReadOnly(readOnly);
        typeSelect.setEnabled(!readOnly);
        priorityField.setReadOnly(readOnly);
        startDatePicker.setEnabled(!readOnly);
        endDatePicker.setEnabled(!readOnly);
    }

    /**
     * Collects the four child grids into a single aggregate, ready to be sent.
     *
     * @return the promotion carrying every collection currently in the form
     */
    public PromotionDto buildAggregate() {
        PromotionDto promotion = new PromotionDto();
        binder.writeBeanIfValid(promotion);
        promotion.setSchedules(new ArrayList<>(schedules));
        promotion.setTargets(new ArrayList<>(targets));
        promotion.setRules(new ArrayList<>(rules));
        promotion.setSpecialPrices(new ArrayList<>(specialPrices));
        return promotion;
    }

    /**
     * Validates the scalar fields and the rules, since a percentage or fixed amount
     * promotion is meaningless without one.
     *
     * @return {@code true} when the aggregate may be sent
     */
    public boolean isAggregateValid() {
        if (binder.validate().hasErrors()) {
            UiUtil.error(Messages.get("validation.promotion.formErrors"));
            return false;
        }
        PromotionDto draft = new PromotionDto();
        binder.writeBeanIfValid(draft);
        boolean needsRule = PromotionType.PERCENTAGE == draft.getPromotionType()
                || PromotionType.FIXED_AMOUNT == draft.getPromotionType();
        if (needsRule && rules.isEmpty()) {
            UiUtil.error(Messages.get("validation.promotion.ruleRequired"));
            return false;
        }
        if (PromotionType.SPECIAL_PRICE == draft.getPromotionType() && specialPrices.isEmpty()) {
            UiUtil.error(Messages.get("validation.promotion.specialPriceRequired"));
            return false;
        }
        return true;
    }

    /**
     * Shows a success toast and closes the tab.
     */
    public void onSaveSuccess() {
        UiUtil.success(Messages.get("notification.promotion.saved"));
        close();
    }

    /**
     * Reports a non-business failure to the operator.
     *
     * @param error the failure that stopped the save
     */
    public void onSaveError(Throwable error) {
        log.error("Promotion save failed", error);
        UiUtil.error(Messages.get("notification.promotion.saveFailed"));
    }

    /**
     * Closes this tab and returns to the list.
     */
    public void close() {
        UiUtil.safeAccess(ui, () -> tabManager.closeAndSelectFirst(currentTab));
    }
}
