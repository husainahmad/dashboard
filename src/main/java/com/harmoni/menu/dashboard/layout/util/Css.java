package com.harmoni.menu.dashboard.layout.util;

/**
 * CSS class, property and attribute names shared across the layouts, so a name
 * only needs to be changed here rather than at every usage site.
 */
public final class Css {

    private Css() {
        throw new IllegalStateException("Utility class");
    }

    public static final String TOOLBAR = "toolbar";
    public static final String LIST_VIEW = "list-view";
    public static final String PRODUCT_FORM = "product-form";
    public static final String HEALTH_CAPTION = "health-caption";
    public static final String TIER_SAVED_AT = "tier-saved-at";
    public static final String TIER_SAVING = "tier-saving";
    public static final String PRICE_DIRTY = "price-dirty";
    public static final String PRICE_SAVING = "price-saving";
    public static final String PRICE_SAVED = "price-saved";
    public static final String PRICE_ERROR = "price-error";
    public static final String STAT_VALUE_OK = "stat-value--ok";
    public static final String STAT_VALUE_WARN = "stat-value--warn";

    public static final String STAT_CARDS = "stat-cards";

    // Style properties set inline on components, collected here for the same reason as
    // the class names above. A property is only listed once it is set from more than
    // three places; a one- or two-off stays a literal beside its one caller, since a
    // constant with a single use is just indirection.

    public static final String ALIGN_ITEMS = "align-items";
    public static final String BACKGROUND_COLOR = "background-color";
    public static final String BORDER = "border";
    public static final String BORDER_BOTTOM = "border-bottom";
    public static final String BORDER_TOP = "border-top";
    public static final String COLOR = "color";
    public static final String DISPLAY = "display";
    public static final String FLEX = "flex";
    public static final String FLEX_GROW = "flex-grow";
    public static final String FONT_SIZE = "font-size";
    public static final String FONT_WEIGHT = "font-weight";
    public static final String HEIGHT = "height";
    public static final String JUSTIFY_CONTENT = "justify-content";
    public static final String MARGIN = "margin";
    public static final String OVERFLOW_WRAP = "overflow-wrap";
    public static final String PADDING = "padding";
    public static final String WIDTH = "width";

    /**
     * The hairline border the preview and form panels are divided by.
     *
     * <p>Eight call sites drew the same line, and a panel that lost it while its
     * neighbours kept theirs is a layout fault that reads as a rendering bug rather
     * than as a missing rule.</p>
     */
    public static final String HAIRLINE_BORDER = "1px solid var(--lumo-contrast-10pct)";

    /**
     * The width a dialog is given so it is usable without eating the viewport.
     *
     * <p>The product picker, the SKU picker and the preview panels all landed on
     * {@code 600px} independently, because a dialog is wide enough to show a grid and
     * narrow enough to leave the list behind it visible.</p>
     */
    public static final String DIALOG_WIDTH = "600px";

    /**
     * The width of the small fixed fields, the time and price pickers.
     *
     * <p>Short enough to read as a single value rather than a column, wide enough for
     * {@code 00:00:00} and a currency amount to sit on it without wrapping.</p>
     */
    public static final String COMPACT_FIELD_WIDTH = "140px";

    public static final String AUTOCOMPLETE = "autocomplete";
    public static final String TERTIARY_INLINE = "tertiary-inline";
}