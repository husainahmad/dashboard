package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.dto.PromotionScheduleDto;
import com.harmoni.menu.dashboard.dto.PromotionTargetDto;
import com.harmoni.menu.dashboard.layout.enums.PromotionScopeType;
import com.harmoni.menu.dashboard.layout.enums.PromotionTargetType;
import com.harmoni.menu.dashboard.layout.enums.PromotionType;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Live read-only mirror of {@link PromotionForm}.
 *
 * <p>Every field the form edits is rendered here as a label/value row, so an
 * operator can confirm the whole payload before saving. The hero area keeps the
 * name and the headline benefit, while the detail rows cover the remaining
 * fields. Rows that have no value yet fall back to {@code preview.notSet}.</p>
 */
public class PromotionPreview extends Div {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String ROW_CODE = "code";
    private static final String ROW_DESCRIPTION = "description";
    private static final String ROW_TYPE = "type";
    private static final String ROW_PRIORITY = "priority";
    private static final String ROW_STATUS = "status";
    private static final String ROW_STACKABLE = "stackable";
    private static final String ROW_START_DATE = "startDate";
    private static final String ROW_END_DATE = "endDate";
    private static final String ROW_DAYS = "days";
    private static final String ROW_START_TIME = "startTime";
    private static final String ROW_END_TIME = "endTime";
    private static final String ROW_DISCOUNT = "discount";
    private static final String ROW_SCOPE = "scope";
    private static final String ROW_APPLY_TO = "applyTo";
    private static final String ROW_TARGET = "target";
    private static final String ROW_SPECIAL_PRICE = "specialPrice";

    private final VerticalLayout content = new VerticalLayout();
    private final VerticalLayout detailRows = new VerticalLayout();
    private final Map<String, Span> values = new LinkedHashMap<>();
    private final Map<String, List<String>> groupRowKeys = new LinkedHashMap<>();
    private final Map<String, Div> groupContainers = new LinkedHashMap<>();
    private VerticalLayout currentGroupRows;
    private String currentGroupKey;

    private final Span nameLabel = new Span();
    private final Span benefitLabel = new Span();
    private final Span statusLabel = new Span();

    public PromotionPreview() {
        addClassName("promotion-preview");
        setWidthFull();

        content.setWidthFull();
        content.setPadding(false);
        content.setSpacing(false);
        // Stretch, so the hero block and the rows below fill whatever width the
        // preview panel gives them. Starting them off shrink-wrapped instead makes
        // the panel's width irrelevant, and the operator's name - the one value in
        // here with no length limit - decides how wide the preview grows.
        content.setAlignItems(FlexComponent.Alignment.STRETCH);

        nameLabel.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD);
        nameLabel.getStyle().set(Css.COLOR, "var(--app-text)");
        // anywhere, not break-word: it is the variant that also shrinks the element's
        // min-content width, so an unbroken name wraps instead of pushing the panel wide.
        nameLabel.getStyle().set(Css.OVERFLOW_WRAP, "anywhere");

        benefitLabel.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD);
        benefitLabel.getStyle().set(Css.COLOR, "var(--lumo-primary-color)");
        benefitLabel.getStyle().set(Css.OVERFLOW_WRAP, "anywhere");

        statusLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.MEDIUM);
        // The one hero element that should hug its text: it is a pill, and stretching
        // it across the panel would leave the badge floating in a wide empty bar.
        // A Span is not a FlexComponent, so align-self goes on the element.
        statusLabel.getElement().getStyle().set("align-self", "start");

        detailRows.setWidthFull();
        detailRows.setPadding(false);
        detailRows.setSpacing(false);

        beginGroup("preview.group.identity");
        addDetailRow(ROW_CODE, "label.promotion.code");
        addDetailRow(ROW_DESCRIPTION, "label.promotion.description");
        addDetailRow(ROW_TYPE, "label.promotion.type");
        addDetailRow(ROW_PRIORITY, "label.promotion.priority");
        addDetailRow(ROW_STATUS, "label.promotion.status");
        addDetailRow(ROW_STACKABLE, "label.promotion.stackable");

        beginGroup("preview.group.schedule");
        addDetailRow(ROW_START_DATE, "label.promotion.startDate");
        addDetailRow(ROW_END_DATE, "label.promotion.endDate");
        addDetailRow(ROW_DAYS, "label.promotion.days");
        addDetailRow(ROW_START_TIME, "label.promotion.startTime");
        addDetailRow(ROW_END_TIME, "label.promotion.endTime");

        beginGroup("preview.group.benefit");
        addDetailRow(ROW_DISCOUNT, "label.promotion.discount");
        addDetailRow(ROW_SPECIAL_PRICE, "grid.header.specialPrice");

        beginGroup("preview.group.targeting");
        addDetailRow(ROW_SCOPE, "label.promotion.scope");
        addDetailRow(ROW_APPLY_TO, "label.promotion.applyTo");
        addDetailRow(ROW_TARGET, "label.promotion.target");

        Div divider = new Div();
        // No width of its own: content stretches it across the panel, the same as
        // every other row in the block.
        divider.getStyle()
                .set("border-top", Css.HAIRLINE_BORDER)
                .set(Css.MARGIN, "var(--lumo-space-s) 0")
                .set("flex-shrink", "0");

        content.add(nameLabel, benefitLabel, statusLabel, divider, detailRows);
        add(content);

        clearPreview();
    }

    /**
     * Starts a titled block of detail rows. Blocks whose rows all end up empty are
     * hidden, so the preview only shows what the operator has actually filled in.
     *
     * @param titleKey message key of the block heading
     */
    private void beginGroup(String titleKey) {
        Div group = new Div();

        Div title = new Div(Messages.get(titleKey));
        title.getStyle()
                .set(Css.FONT_SIZE, "var(--lumo-font-size-xs)")
                .set(Css.FONT_WEIGHT, "600")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.04em")
                .set(Css.COLOR, "var(--lumo-tertiary-text-color)")
                .set(Css.MARGIN, "0 0 var(--lumo-space-xs) 0");

        currentGroupRows = new VerticalLayout();
        currentGroupRows.setWidthFull();
        currentGroupRows.setPadding(false);
        currentGroupRows.setSpacing(false);

        group.add(title, currentGroupRows);
        detailRows.add(group);

        groupRowKeys.put(titleKey, new ArrayList<>());
        groupContainers.put(titleKey, group);
        currentGroupKey = titleKey;
    }

    private void addDetailRow(String key, String labelKey) {
        Span label = new Span(Messages.get(labelKey));
        label.addClassName("preview-detail-label");
        // Sized to its own text, so a short label stops reserving 42% of the row. Most
        // labels here are one or two words - "Type", "Priority", "Active" - and a fixed
        // share made every one of them hold back a quarter of the panel from the value
        // beside it. The cap is what keeps a long localised label from taking the row:
        // it can still grow, but never past the share the value used to be guaranteed.
        label.getStyle()
                .set(Css.FONT_SIZE, "var(--lumo-font-size-s)")
                .set(Css.COLOR, "var(--app-text-secondary)")
                .set(Css.FLEX, "0 1 auto")
                .set("max-width", "42%")
                .set("min-width", "0");

        Span value = new Span();
        value.addClassName("preview-detail-value");
        // Basis 0 with grow 1, so the value takes exactly what the label leaves over
        // instead of its own content width deciding the split. min-width 0 lifts the
        // flex default that would otherwise pin it to min-content.
        //
        // break-word rather than anywhere, and the distinction is the whole bug: anywhere
        // lets a single word be split mid-word ("Percenta" / "ge") as soon as the column
        // is a shade narrow, and it lowers the value's min-content size so the flex
        // algorithm is happy to hand it that narrow column in the first place.
        // break-word only breaks a word once it genuinely cannot fit the line, so a value
        // that fits stays on one line and a long target list still wraps in place.
        value.getStyle()
                .set(Css.FONT_SIZE, "var(--lumo-font-size-s)")
                .set(Css.COLOR, "var(--app-text)")
                .set(Css.FLEX, "1 1 0")
                .set("min-width", "0")
                .set(Css.OVERFLOW_WRAP, "break-word");

        HorizontalLayout row = new HorizontalLayout(label, value);
        row.setWidthFull();
        row.setPadding(false);
        row.setSpacing(true);
        row.setAlignItems(FlexComponent.Alignment.START);
        row.getStyle()
                .set(Css.PADDING, "2px 0")
                .set(Css.BORDER_BOTTOM, "1px solid var(--lumo-contrast-5pct)");

        currentGroupRows.add(row);
        values.put(key, value);
        groupRowKeys.get(currentGroupKey).add(key);
    }

    /**
     * Refreshes every row from the current form aggregate.
     *
     * @param promotion   the aggregate built by {@link PromotionForm#buildAggregate()}
     * @param targetNames display names of the selected targets, keyed by
     *                    {@link #targetKey(PromotionTargetType, Long)}
     */
    public void updatePreview(PromotionDto promotion, Map<String, String> targetNames) {
        if (promotion == null) {
            clearPreview();
            return;
        }

        nameLabel.setText(promotion.getName() != null ? promotion.getName() : Messages.get("preview.unnamed"));
        benefitLabel.setText(buildBenefitText(promotion));
        detailRows.setVisible(true);

        boolean active = Boolean.TRUE.equals(promotion.getActive());
        statusLabel.setText(active ? Messages.get("label.active") : Messages.get("label.inactive"));
        statusLabel.removeClassNames("status-active", "status-inactive");
        statusLabel.addClassName(active ? "status-active" : "status-inactive");

        set(ROW_CODE, promotion.getCode());
        set(ROW_DESCRIPTION, promotion.getDescription());
        set(ROW_TYPE, promotion.getPromotionType() == null
                ? null : promotion.getPromotionType().getLabel());
        set(ROW_PRIORITY, promotion.getPriority() == null
                ? null : String.valueOf(promotion.getPriority()));
        set(ROW_STATUS, promotion.getStatus() == null
                ? null : promotion.getStatus().getLabel());
        set(ROW_STACKABLE, promotion.getStackable() == null
                ? Messages.get(Messages.Keys.PREVIEW_NOT_SET)
                : Messages.get(promotion.getStackable() ? "label.yes" : "label.no"));
        set(ROW_START_DATE, promotion.getStartDate() == null
                ? null : DATE_FORMAT.format(promotion.getStartDate()));
        set(ROW_END_DATE, promotion.getEndDate() == null
                ? null : DATE_FORMAT.format(promotion.getEndDate()));
        set(ROW_DAYS, buildDaysText(promotion.getSchedules()));
        set(ROW_START_TIME, formatTime(promotion.getStartTime()));
        set(ROW_END_TIME, formatTime(promotion.getEndTime()));
        set(ROW_DISCOUNT, buildDiscountText(promotion));
        set(ROW_SCOPE, buildScopeText(promotion, targetNames));
        set(ROW_APPLY_TO, promotion.getApplyToType() == null
                ? null : promotion.getApplyToType().getLabel());
        set(ROW_TARGET, buildTargetText(promotion, targetNames));
        set(ROW_SPECIAL_PRICE, buildSpecialPriceText(promotion));

        hideEmptyGroups();
    }

    /**
     * Hides every block whose rows are all still unset, keeping the preview short
     * while the form is only partly filled in.
     */
    private void hideEmptyGroups() {
        String notSet = Messages.get(Messages.Keys.PREVIEW_NOT_SET);
        groupRowKeys.forEach((groupKey, rowKeys) -> {
            boolean anyFilled = rowKeys.stream()
                    .map(values::get)
                    .anyMatch(span -> !notSet.equals(span.getText()));
            groupContainers.get(groupKey).setVisible(anyFilled);
        });
    }

    private void set(String key, String value) {
        values.get(key).setText(value == null || value.isBlank()
                ? Messages.get(Messages.Keys.PREVIEW_NOT_SET) : value);
    }

    private String buildDaysText(List<PromotionScheduleDto> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return null;
        }
        String days = schedules.stream()
                .filter(s -> Boolean.TRUE.equals(s.getEnabled()) && s.getDayOfWeek() != null)
                .map(s -> dayName(s.getDayOfWeek()))
                .distinct()
                .collect(Collectors.joining(", "));
        return days.isEmpty() ? null : days;
    }

    private String formatTime(LocalTime time) {
        return time == null ? null : TIME_FORMAT.format(time);
    }

    private String buildDiscountText(PromotionDto promotion) {
        if (PromotionType.SPECIAL_PRICE == promotion.getPromotionType()) {
            return null;
        }
        BigDecimal discount = promotion.getDiscountValue();
        if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        String unit = PromotionType.PERCENTAGE == promotion.getPromotionType() ? "%" : "Rp";
        String amount = "%".equals(unit)
                ? discount.stripTrailingZeros().toPlainString()
                : formatRupiah(discount);
        return amount.concat(" ").concat(unit).concat(" ").concat(Messages.get("label.off"));
    }

    /**
     * Name of the brand the promotion is limited to.
     *
     * <p>Keys into the display-name map {@link PromotionForm} fills and this preview
     * reads, so a change to either name has to be made in both places. They are
     * constants here rather than a method each so that the set is visible in one block
     * and a reader can see they are the only three.</p>
     */
    static final String BRAND_KEY = "org:brand";

    /** Name of the chain the promotion is limited to. */
    static final String CHAIN_KEY = "org:chain";

    /** Names of the stores the promotion is limited to, comma separated. */
    static final String STORE_KEY = "org:stores";

    /**
     * The organization the promotion is limited to, named.
     *
     * <p>Names the level alongside it, so "Kopi Harmoni" under a chain scope cannot be
     * misread as a brand. Falls back to the bare level when the names have not arrived
     * yet, rather than leaving the row blank and looking like nothing was chosen.</p>
     *
     * @param promotion the promotion being previewed
     * @param names     display names, as the form collected them
     * @return the scope text, or {@code null} when no scope is chosen
     */
    String buildScopeText(PromotionDto promotion, Map<String, String> names) {
        PromotionScopeType scope = promotion.getScope();
        if (scope == null) {
            return null;
        }
        String organization = names.get(scopeNameKey(scope));
        if (organization == null || organization.isBlank()) {
            return scope.getLabel();
        }
        return scope.getLabel().concat(": ").concat(organization);
    }

    /**
     * The names entry naming the organization a scope narrows by.
     *
     * @param scope the scope chosen
     * @return the key to read, or {@code null} for a scope that narrows by nothing
     */
    private static String scopeNameKey(PromotionScopeType scope) {
        return switch (scope) {
            case BRAND -> BRAND_KEY;
            case CHAIN -> CHAIN_KEY;
            case STORE -> STORE_KEY;
            case ALL_STORES -> null;
        };
    }

    /**
     * The lookup key a target's display name is stored under.
     *
     * <p>A null {@code refId} is rendered as the text {@code null} rather than rejected.
     * {@code concat} would throw on one, and the target rows already carry a null ref id
     * when a saved promotion names a target the backend returned without one: the row
     * falls back to its message template in that case, and it can only do that if the
     * lookup for a name runs first and misses.</p>
     *
     * @param type  the target type
     * @param refId the referenced id, which may be {@code null}
     * @return the shared lookup key
     */
    static String targetKey(PromotionTargetType type, Long refId) {
        return type.toString().concat(":").concat(String.valueOf(refId));
    }

    /**
     * Names the targets, package-private so the target row can be tested the same way the
     * scope row is.
     */
    String buildTargetText(PromotionDto promotion, Map<String, String> targetNames) {
        if (promotion.getTargets() == null || promotion.getTargets().isEmpty()) {
            return null;
        }
        String listed = promotion.getTargets().stream()
                .filter(target -> target != null && target.getTargetType() != null)
                .map(target -> describeTarget(target, targetNames))
                .collect(Collectors.joining(", "));
        if (listed.isEmpty()) {
            return null;
        }
        return listed.concat(" (").concat(String.valueOf(promotion.getTargets().size())).concat(")");
    }

    /**
     * Names a single target, package-private so the target row can be tested the same way the
     * scope row is.
     */
    private String describeTarget(PromotionTargetDto target, Map<String, String> targetNames) {
        Long refId = switch (target.getTargetType()) {
            case CATEGORY -> target.getCategoryId();
            case PRODUCT -> target.getProductId();
            case SKU -> target.getSkuId();
        };
        String name = targetNames == null ? null : targetNames.get(targetKey(target.getTargetType(), refId));
        if (name != null && !name.isBlank()) {
            return refId == null ? name : name.concat(" (").concat(refId.toString()).concat(")");
        }
        return switch (target.getTargetType()) {
            case CATEGORY -> Messages.get("preview.target.category", refId);
            case PRODUCT -> Messages.get("preview.target.product", refId);
            case SKU -> Messages.get("preview.target.sku", refId);
        };
    }

    /**
     * Lists the special prices, package-private so the special price row can be tested
     * the same way the discount row is.
     */
    private String buildSpecialPriceText(PromotionDto promotion) {
        if (PromotionType.SPECIAL_PRICE != promotion.getPromotionType()
                || promotion.getSpecialPrices() == null
                || promotion.getSpecialPrices().isEmpty()) {
            return null;
        }
        String listed = promotion.getSpecialPrices().stream()
                .filter(specialPrice -> specialPrice != null && specialPrice.getSkuId() != null)
                .map(specialPrice -> "SKU ".concat(specialPrice.getSkuId().toString()).concat(": ")
                        .concat(formatRupiah(specialPrice.getSpecialPrice())))
                .collect(Collectors.joining(", "));
        return listed.isEmpty() ? null : listed;
    }

    private String dayName(DayOfWeek day) {
        return Messages.get("promotion.day." + day.name());
    }

    /**
     * Builds the headline benefit text, package-private so it can be tested the same way
     * the discount row is.
     */
    private String buildBenefitText(PromotionDto promotion) {
        PromotionType type = promotion.getPromotionType();
        if (type == null) {
            return "";
        }
        if (type == PromotionType.SPECIAL_PRICE) {
            return Messages.get("preview.specialPrice.title");
        }
        BigDecimal discount = promotion.getDiscountValue();
        if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0) {
            return "";
        }
        if (type == PromotionType.PERCENTAGE) {
            return discount.stripTrailingZeros().toPlainString().concat("% ").concat(Messages.get("label.off"));
        }
        if (type == PromotionType.FIXED_AMOUNT) {
            return formatRupiah(discount).concat(" ").concat(Messages.get("label.off"));
        }
        return "";
    }

    /**
     * Formats a {@link BigDecimal} as Indonesian Rupiah, package-private so it can be
     * tested the same way the discount row is.
     */
    private String formatRupiah(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,##0",
                java.text.DecimalFormatSymbols.getInstance(new Locale("id", "ID")));
        return "Rp".concat(formatter.format(value.doubleValue()));
    }

    /**
     * Resets the preview to its initial empty state, with no name, no benefit, and no
     * detail rows.
     */
    private void clearPreview() {
        nameLabel.setText(Messages.get("preview.emptyTitle"));
        benefitLabel.setText("");
        statusLabel.setText("");
        statusLabel.removeClassNames("status-active", "status-inactive");
        detailRows.setVisible(false);
    }
}
