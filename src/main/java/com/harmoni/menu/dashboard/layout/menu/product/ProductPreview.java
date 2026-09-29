package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.CustomizationOptionDto;
import com.harmoni.menu.dashboard.dto.ProductCustomizationDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.enums.SelectionType;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A customer-facing rendering of the product being edited, kept in step with the form.
 *
 * <p>Shaped like the card a customer would meet rather than like a summary of the record.
 * The promotion editor's preview is a field-by-field echo of its form, which is the right
 * shape when the thing being described is a set of rules; a product is the thing that
 * actually gets shown to someone, so this leads with the picture and the name, and puts
 * the description last where prose belongs.</p>
 *
 * <p>The picture is the one thing shown only here: the form's tile keeps the upload,
 * crop and remove controls but no longer repeats the image, so this card is the single
 * place the operator looks to see what they picked.</p>
 *
 * <p>A product has no single price. {@code ProductDto} and {@code SkuDto} both lack a
 * price field: it hangs off {@code SkuDto.skuTierPriceDtos} as one entry per price tier,
 * so the real figure is a matrix of variants against tiers. Showing any single number
 * would misreport a product that has more than one row or column, so the whole matrix is
 * shown instead.</p>
 */
public class ProductPreview extends Div {

    private static final String GROUP_PRICES = "preview.group.prices";
    private static final String GROUP_CUSTOMIZATIONS = "preview.group.customizations";

    private final Div imageFrame = new Div();
    private final Image image = new Image();
    private final Span noImageLabel = new Span();
    private final Span nameLabel = new Span();
    private final Span categoryLabel = new Span();
    private final Div priceSection = new Div();
    private final Div priceHost = new Div();
    private final Div customizationSection = new Div();
    private final Div customizationHost = new Div();
    private final Div descriptionSection = new Div();
    private final Span descriptionLabel = new Span();

    public ProductPreview() {
        addClassName("product-preview");
        setWidthFull();
        renderImage();
        renderHeader();
        renderPrices();
        renderCustomizations();
        renderDescription();
    }

    /**
     * Renders the product image, or a placeholder if none is set.
     *
     * <p>The image is uploaded and cropped before the product is ever saved, so the
     * hosted URL is already available while the operator is still typing the name.
     * That makes the picture the one part of the preview that can lead.</p>
     */
    private void renderImage() {
        image.setClassName("product-preview-image");
        // The image is uploaded and cropped before the product is ever saved, so the
        // hosted URL is already available while the operator is still typing the name.
        // That makes the picture the one part of the preview that can lead.
        image.getElement().getStyle()
                .set("object-fit", "cover")
                .set(Css.WIDTH, "100%")
                .set(Css.HEIGHT, "100%");

        noImageLabel.setText(Messages.get("preview.product.noImage"));
        noImageLabel.addClassName("product-preview-no-image");
        noImageLabel.getStyle()
                .set(Css.DISPLAY, "flex")
                .set(Css.ALIGN_ITEMS, "center")
                .set(Css.JUSTIFY_CONTENT, "center")
                .set(Css.COLOR, "var(--app-text-muted)")
                .set(Css.FONT_SIZE, "var(--lumo-font-size-s)");

        imageFrame.addClassName("product-preview-image-frame");
        imageFrame.add(image, noImageLabel);
        noImageLabel.setVisible(false);
        add(imageFrame);
    }

    /**
     * Renders the product name and category, which are the two fields that identify
     * a product to a customer.
     *
     * <p>The name is the only field that is required, so it is always shown. The
     * category is optional, so it is hidden when not set.</p>
     */
    private void renderHeader() {
        nameLabel.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD);
        // anywhere, not break-word: it is the variant that also shrinks min-content
        // width, so a long unbroken name wraps instead of widening the whole panel.
        nameLabel.getStyle()
                .set(Css.COLOR, "var(--app-text)")
                .set(Css.OVERFLOW_WRAP, "anywhere");

        categoryLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.MEDIUM);
        categoryLabel.getStyle()
                .set(Css.COLOR, "var(--app-text-muted)")
                .set(Css.OVERFLOW_WRAP, "anywhere");

        VerticalLayout header = new VerticalLayout(nameLabel, categoryLabel);
        header.addClassName("product-preview-section");
        header.setPadding(false);
        header.setSpacing(false);
        header.setAlignItems(FlexComponent.Alignment.STRETCH);
        header.addClassName("product-preview-header");
        add(header);
    }

    // --------------------------------------------------------------- prices

    private void renderPrices() {
        priceSection.addClassName("product-preview-section");
        priceHost.addClassName("product-preview-price-host");
        priceSection.add(groupTitle(GROUP_PRICES), priceHost);
        add(priceSection);
    }

    private void renderCustomizations() {
        customizationSection.addClassName("product-preview-section");
        customizationSection.add(groupTitle(GROUP_CUSTOMIZATIONS), customizationHost);
        add(customizationSection);
    }

    /**
     * One group heading for every group on the card.
     *
     * <p>Styled inline, as PromotionPreview styles its group titles, so the two
     * panels' section headings stay identical without a shared class. Shared here
     * because the card has three groups and the rules are long enough that copying
     * them three times would be how they drift apart.</p>
     */
    private static Div groupTitle(String key) {
        Div title = new Div(Messages.get(key));
        title.getStyle()
                .set(Css.FONT_SIZE, "var(--lumo-font-size-xs)")
                .set(Css.FONT_WEIGHT, "600")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.04em")
                .set(Css.COLOR, "var(--lumo-tertiary-text-color)")
                .set(Css.MARGIN, "0 0 var(--lumo-space-xs) 0");
        return title;
    }

    /**
     * Rebuilds the variant-by-tier price matrix.
     *
     * <p>Tiers are the columns and variants the rows, in the order the brand defines
     * them, because a price tier's position carries meaning to the operator - "the one
     * next to Retail" is not something worth reordering away from the list they set up.
     * A variant with nothing entered for a tier shows a dash rather than a blank, so a
     * missing price never reads as a price of zero.</p>
     *
     * @param skus  live variants, including edits not yet saved
     * @param tiers tiers of the brand, as columns
     */
    private void buildPriceMatrix(List<SkuTreeItem> skus, List<TierDto> tiers) {
        priceHost.removeAll();

        if (skus == null || skus.isEmpty()) {
            priceHost.add(muted(Messages.get("preview.product.emptySku")));
            return;
        }
        if (tiers == null || tiers.isEmpty()) {
            priceHost.add(muted(Messages.get("preview.product.emptyPrices")));
            return;
        }

        // One scroll container for the whole table so the header row and the body
        // scroll together; a per-row overflow would leave the tiers stranded off-screen
        // while the operator reads the numbers beneath them.
        Div scroller = new Div();
        scroller.setClassName("product-preview-price-scroll");

        scroller.add(matrixRow(headerCells(tiers), true));
        for (SkuTreeItem sku : skus) {
            scroller.add(matrixRow(cellsFor(sku, tiers), false));
        }
        priceHost.add(scroller);
    }

    private List<Span> headerCells(List<TierDto> tiers) {
        java.util.List<Span> cells = new java.util.ArrayList<>();
        Span corner = new Span(Messages.get("preview.product.variants"));
        corner.addClassName("product-preview-price-corner");
        cells.add(corner);
        for (TierDto tier : tiers) {
            Span cell = new Span(orDash(tier == null ? null : tier.getName()));
            cell.addClassName("product-preview-price-head");
            cells.add(cell);
        }
        return cells;
    }

    private List<Span> cellsFor(SkuTreeItem sku, List<TierDto> tiers) {
        java.util.List<Span> cells = new java.util.ArrayList<>();
        Span name = new Span(orPlaceholder(sku == null ? null : sku.getSkuName()));
        name.addClassName("product-preview-price-variant");
        cells.add(name);
        Map<Integer, Double> prices = sku == null ? null : sku.getTierPrices();
        for (TierDto tier : tiers) {
            Double price = prices == null || tier == null ? null : prices.get(tier.getId());
            Span cell = new Span(UiUtil.rupiah(price));
            cell.addClassName("product-preview-price-cell");
            cells.add(cell);
        }
        return cells;
    }

    /**
     * Renders the customizations attached to this product.
     *
     * <p>Each one shows what a customer would be asked for: the name, whether it must
     * be answered, whether one or several values are allowed, the selection bounds, and
     * the option names.</p>
     *
     * <p>The bounds are read as <em>override first, master second</em>. The
     * customization carries the brand-wide default in {@code minSelection} and
     * {@code maxSelection}, and a per-product departure in {@code minSelectionOverride}
     * and {@code maxSelectionOverride}; a non-null override wins, and the card shows
     * the value that would actually be enforced. Note the customization grid in the
     * left-hand form reads only the master values, so the two can disagree while an
     * override is in place.</p>
     */
    private void buildCustomizations(List<ProductCustomizationDto> customizations) {
        customizationHost.removeAll();

        if (ObjectUtils.isEmpty(customizations)) {
            customizationSection.setVisible(false);
            return;
        }
        customizationSection.setVisible(true);

        for (ProductCustomizationDto customization : customizations) {
            customizationHost.add(customizationCard(customization));
        }
    }

    private Div customizationCard(ProductCustomizationDto customization) {
        Div card = new Div();
        card.addClassName("product-preview-customization");

        Span name = new Span(orDash(customization.getName()));
        name.addClassName("product-preview-customization-name");
        card.add(name);

        card.add(muted(customizationMeta(customization)));

        String options = optionNames(customization);
        if (!options.isEmpty()) {
            card.add(muted(options));
        }
        return card;
    }

    /** "Required - Single - 1 / 1", skipping the parts that carry no information. */
    private String customizationMeta(ProductCustomizationDto customization) {
        List<String> parts = new ArrayList<>();
        parts.add(Boolean.TRUE.equals(customization.getRequired())
                ? Messages.get(Messages.Keys.LABEL_REQUIRED)
                : Messages.get(Messages.Keys.LABEL_OPTIONAL));

        if (customization.getSelectionType() != null) {
            parts.add(customization.getSelectionType() == SelectionType.MULTIPLE
                    ? Messages.get("label.multi")
                    : Messages.get("label.single"));
        }

        Integer min = customization.getMinSelectionOverride() != null
                ? customization.getMinSelectionOverride()
                : customization.getMinSelection();
        Integer max = customization.getMaxSelectionOverride() != null
                ? customization.getMaxSelectionOverride()
                : customization.getMaxSelection();
        parts.add("min " + (min == null ? "0" : min) + " / " + (max == null ? "n" : max));
        return String.join(" - ", parts);
    }

    private static String optionNames(ProductCustomizationDto customization) {
        if (ObjectUtils.isEmpty(customization.getOptions())) {
            return "";
        }
        return customization.getOptions().stream()
                .map(CustomizationOptionDto::getName)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(", "));
    }

    private HorizontalLayout matrixRow(List<Span> cells, boolean header) {
        HorizontalLayout row = new HorizontalLayout();
        row.setPadding(false);
        row.setSpacing(false);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.addClassName(header ? "product-preview-price-row-head" : "product-preview-price-row");
        cells.forEach(row::add);
        // Deliberately not setWidthFull(). An inline width:100% pins every row to the
        // width of the scroller, so the table's real width never exceeds its container
        // and there is nothing for overflow-x to scroll: a brand with several price
        // tiers simply had its last columns overrun and clipped. The rows size to
        // their cells instead, and the stylesheet gives them min-width:100% so a short
        // table still fills the panel.
        return row;
    }

    // ---------------------------------------------------------- description

    private void renderDescription() {
        descriptionLabel.addClassName("product-preview-description");
        // Plain text from a TextArea, so it is set as text and never as HTML, and it is
        // clamped: a product description is free text of no fixed length, and one long
        // paragraph would otherwise push the whole card past the fold.
        descriptionLabel.getStyle()
                .set(Css.COLOR, "var(--app-text-secondary)")
                .set(Css.OVERFLOW_WRAP, "break-word")
                .set(Css.DISPLAY, "-webkit-box")
                .set("-webkit-line-clamp", "6")
                .set("-webkit-box-orient", "vertical")
                .set("overflow", "hidden");

        descriptionSection.add(descriptionLabel);
        add(descriptionSection);
    }

    // --------------------------------------------------------------- update

    /**
     * Repaints the card from the form's current state.
     *
     * <p>Called on every relevant edit, so it must tolerate a product that is still
     * nearly empty: a new product has no name, no category, no image, no variants and no
     * prices for several seconds, and the card has to say so rather than look broken.</p>
     *
     * @param product the product being edited, may be {@code null}
     * @param skus    live variants, including edits not yet saved
     * @param tiers   tiers of the brand, as price columns
     * @param customizations the product's attached customizations, may be {@code null}
     */
    public void updatePreview(ProductDto product, List<SkuTreeItem> skus, List<TierDto> tiers,
                              List<ProductCustomizationDto> customizations) {
        String name = product == null ? null : product.getName();
        nameLabel.setText(orPlaceholder(name));

        CategoryDto category = product == null ? null : product.getCategoryDto();
        String categoryName = category == null ? null : category.getName();
        categoryLabel.setText(orDash(categoryName));
        categoryLabel.setVisible(categoryName != null && !categoryName.isBlank());

        String url = imageUrl(product);
        // Never setSrc(null): Image rejects it outright, and a product with no picture
        // yet is the ordinary case for the first few seconds of a new one.
        image.setSrc(url == null ? "" : url);
        image.setVisible(url != null);
        noImageLabel.setVisible(url == null);

        String description = product == null ? null : product.getDescription();
        descriptionLabel.setText(orDash(description));
        descriptionSection.setVisible(description != null && !description.isBlank());

        buildPriceMatrix(skus, tiers);
        buildCustomizations(customizations);
    }

    private static String orPlaceholder(String value) {
        return value == null || value.isBlank()
                ? Messages.get("preview.product.unnamed")
                : value;
    }

    private static String imageUrl(ProductDto product) {
        if (product == null || product.getProductImageDto() == null) {
            return null;
        }
        String url = product.getProductImageDto().getUrl();
        return url == null || url.isBlank() ? null : url;
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? Messages.get(Messages.Keys.PREVIEW_NOT_SET) : value;
    }

    private static Span muted(String text) {
        Span span = new Span(text);
        span.addClassName("product-preview-muted");
        span.getStyle().set(Css.COLOR, "var(--app-text-muted)")
                .set(Css.FONT_SIZE, "var(--lumo-font-size-s)");
        return span;
    }
}
