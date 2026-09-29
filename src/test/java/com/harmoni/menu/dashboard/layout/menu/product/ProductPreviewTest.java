package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.CustomizationOptionDto;
import com.harmoni.menu.dashboard.dto.ProductCustomizationDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.ProductImageDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.enums.SelectionType;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards what the product preview shows as the form is edited.
 *
 * <p>The point of the preview is that it reflects the form, not the saved product, so
 * most of these drive {@code updatePreview} directly with unsaved values. That is the
 * only seam available: the form reaches the preview through its own state, and a unit
 * test has no session to attach to.</p>
 *
 * <p>Note that {@code Messages.get} returns the bare key in a unit test - no bundle is
 * loaded - so an assertion built on it pins which key a field uses rather than the
 * wording behind it.</p>
 */
class ProductPreviewTest {

    @Test
    void itShowsTheProductName() {
        ProductPreview preview = new ProductPreview();

        preview.updatePreview(product("Americano"), List.of(), List.of(), List.of());

        assertTrue(text(preview).contains("Americano"));
    }

    @Test
    void itNamesAnUnnamedProductRatherThanShowingNothing() {
        ProductPreview preview = new ProductPreview();

        preview.updatePreview(product("  "), List.of(), List.of(), List.of());

        assertTrue(text(preview).contains(Messages.get("preview.product.unnamed")),
                "a half-typed name should read as a placeholder, not as a blank card");
    }

    @Test
    void itShowsTheCategory() {
        ProductPreview preview = new ProductPreview();
        ProductDto product = product("Americano");
        product.setCategoryDto(category("Drinks"));

        preview.updatePreview(product, List.of(), List.of(), List.of());

        assertTrue(text(preview).contains("Drinks"));
    }

    @Test
    void itHidesTheCategoryLineWhenNoCategoryIsChosen() {
        ProductPreview preview = new ProductPreview();
        ProductDto product = product("Americano");
        product.setCategoryDto(category(null));

        preview.updatePreview(product, List.of(), List.of(), List.of());

        assertFalse(categoryLine(preview).isVisible(),
                "an unset category is not a row worth showing, so the line is dropped");
    }

    @Test
    void itShowsTheCategoryLineOnceACategoryIsChosen() {
        ProductPreview preview = new ProductPreview();
        ProductDto product = product("Americano");
        product.setCategoryDto(category("Drinks"));

        preview.updatePreview(product, List.of(), List.of(), List.of());

        assertTrue(categoryLine(preview).isVisible());
    }

    @Test
    void itShowsTheDescription() {
        ProductPreview preview = new ProductPreview();
        ProductDto product = product("Americano");
        product.setDescription("Shots over ice.");

        preview.updatePreview(product, List.of(), List.of(), List.of());

        assertTrue(text(preview).contains("Shots over ice."));
        assertTrue(descriptionSection(preview).isVisible());
    }

    @Test
    void itHidesTheDescriptionWhenThereIsNone() {
        ProductPreview preview = new ProductPreview();
        ProductDto product = product("Americano");
        product.setDescription("   ");

        preview.updatePreview(product, List.of(), List.of(), List.of());

        assertFalse(descriptionSection(preview).isVisible(),
                "a blank description should leave no section behind at all");
    }

    // ----------------------------------------------------------- price matrix

    @Test
    void itGivesEveryTierItsOwnColumn() {
        ProductPreview preview = new ProductPreview();

        preview.updatePreview(product("Americano"), List.of(variant("Large", 25_000.0, 30_000.0)),
                List.of(tier(1, "Retail"), tier(2, "Whole")), List.of());

        String text = text(preview);
        assertTrue(text.contains("Retail"), "tier names head the columns");
        assertTrue(text.contains("Whole"));
        assertTrue(text.contains(Messages.get("preview.product.variants")),
                "the corner names the row dimension, so a bare number is never ambiguous");
    }

    @Test
    void itGivesEveryVariantItsOwnRow() {
        ProductPreview preview = new ProductPreview();

        preview.updatePreview(product("Americano"),
                List.of(variant("Large", 25_000.0, 30_000.0), variant("Small", 20_000.0, 24_000.0)),
                List.of(tier(1, "Retail"), tier(2, "Whole")), List.of());

        String text = text(preview);
        assertTrue(text.contains("Large"));
        assertTrue(text.contains("Small"));
    }

    @Test
    void itShowsThePriceAtEachVariantsOwnTier() {
        ProductPreview preview = new ProductPreview();

        preview.updatePreview(product("Americano"), List.of(variant("Large", 25_000.0, 30_000.0)),
                List.of(tier(1, "Retail"), tier(2, "Whole")), List.of());

        String text = text(preview);
        assertTrue(text.contains("Rp25.000"), "the retail price for this variant");
        assertTrue(text.contains("Rp30.000"), "and the wholesale one, which is a different cell");
    }

    @Test
    void itDoesNotLeakOneVariantsPriceOntoAnothersRow() {
        ProductPreview preview = new ProductPreview();

        preview.updatePreview(product("Americano"),
                List.of(variant("Large", 25_000.0, 30_000.0), variant("Small", 20_000.0, 24_000.0)),
                List.of(tier(1, "Retail"), tier(2, "Whole")), List.of());

        String text = text(preview);
        assertTrue(text.contains("Rp20.000"));
        assertTrue(text.contains("Rp24.000"));
    }

    @Test
    void aVariantWithNoPriceForATierShowsADashNotAZero() {
        ProductPreview preview = new ProductPreview();
        Map<Integer, Double> prices = new HashMap<>();
        prices.put(1, 25_000.0);

        preview.updatePreview(product("Americano"), List.of(variant("Large", prices)),
                List.of(tier(1, "Retail"), tier(2, "Whole")), List.of());

        String text = text(preview);
        assertTrue(text.contains("Rp25.000"));
        assertTrue(text.contains(Messages.get("preview.notSet")),
                "a missing price must not read as a price of zero");
        assertFalse(text.contains("Rp0"), "nothing here should render as a free variant");
    }

    @Test
    void itSaysSoWhenThereAreNoVariantsYet() {
        ProductPreview preview = new ProductPreview();

        preview.updatePreview(product("Americano"), List.of(), List.of(tier(1, "Retail")), List.of());

        assertTrue(text(preview).contains(Messages.get("preview.product.emptySku")));
    }

    @Test
    void itSaysSoWhenTheBrandHasNoPriceTiers() {
        ProductPreview preview = new ProductPreview();

        preview.updatePreview(product("Americano"), List.of(variant("Large", 25_000.0, 30_000.0)),
                List.of(), List.of());

        assertTrue(text(preview).contains(Messages.get("preview.product.emptyPrices")));
    }

    // ------------------------------------------------------------- liveness

    @Test
    void anUnsavedPriceEditShowsUpOnTheNextRepaint() {
        ProductPreview preview = new ProductPreview();
        SkuTreeItem sku = variant("Large", 25_000.0, 30_000.0);
        List<TierDto> tiers = List.of(tier(1, "Retail"), tier(2, "Whole"));

        preview.updatePreview(product("Americano"), List.of(sku), tiers, List.of());
        assertTrue(text(preview).contains("Rp25.000"));

        // The edit the operator is in the middle of making, not a saved one.
        sku.getTierPrices().put(1, 27_500.0);
        preview.updatePreview(product("Americano"), List.of(sku), tiers, List.of());

        assertTrue(text(preview).contains("Rp27.500"),
                "the preview has to follow the row being edited, not a snapshot of it");
    }

    @Test
    void aRepaintWithNothingAtAllDoesNotBlowUp() {
        ProductPreview preview = new ProductPreview();

        preview.updatePreview(null, null, null, List.of());

        assertTrue(text(preview).contains(Messages.get("preview.product.unnamed")));
    }

    // ---------------------------------------------------------- table sizing

    @Test
    void thePriceRowsAreNotPinnedToTheWidthOfThePanel() {
        ProductPreview preview = new ProductPreview();
        preview.updatePreview(product("Americano"),
                List.of(variant("Large", 25_000.0, 30_000.0)),
                List.of(tier(1, "Retail"), tier(2, "Whole"), tier(3, "Wholesale")), List.of());

        List<Component> rows = rows(preview);
        assertFalse(rows.isEmpty(), "a price table was expected");
        for (Component row : rows) {
            assertNull(row.getElement().getStyle().get("width"),
                    "an inline width pins the row to the panel, so a table wider than the "
                            + "panel has nothing to scroll to and its last columns are "
                            + "clipped instead");
        }
    }

    @Test
    void aLongPriceIsNotTruncatedToNothing() {
        ProductPreview preview = new ProductPreview();
        preview.updatePreview(product("Americano"),
                List.of(variant("Large", 1_250_000.0)),
                List.of(tier(1, "Retail")), List.of());

        assertTrue(text(preview).contains("Rp1.250.000"),
                "a wide figure has to be shown in full, even if that makes the column wider");
    }

    @Test
    void manyTiersStillProduceOneCellEach() {
        ProductPreview preview = new ProductPreview();
        List<TierDto> tiers = List.of(tier(1, "Retail"), tier(2, "Whole"), tier(3, "Wholesale"),
                tier(4, "Distributor"), tier(5, "Reseller"));

        preview.updatePreview(product("Americano"), List.of(variant("Large", 25_000.0)),
                tiers, List.of());

        // Five tiers is already past the panel's width, so this is the case that used
        // to overrun: the columns are what overflow, not the rendering.
        assertEquals(5, countCellsWith(preview, "product-preview-price-head"),
                "a tier must not be dropped to make the table fit");
    }

    // ------------------------------------------------------- customizations

    @Test
    void anAttachedCustomizationIsShownWithItsName() {
        ProductPreview preview = new ProductPreview();
        preview.updatePreview(product("Americano"), List.of(), List.of(),
                List.of(customization("Milk", SelectionType.SINGLE, true, 1, 1, "Whole", "Oat")));

        assertTrue(text(preview).contains("Milk"));
    }

    @Test
    void theCustomerFacingFactsOfACustomizationAreShown() {
        ProductPreview preview = new ProductPreview();
        preview.updatePreview(product("Americano"), List.of(), List.of(),
                List.of(customization("Toppings", SelectionType.MULTIPLE, false, 0, 3, "Vanilla")));

        String text = text(preview);
        assertTrue(text.contains(Messages.get(Messages.Keys.LABEL_OPTIONAL)),
                "whether the customer must answer this at all");
        assertTrue(text.contains(Messages.get("label.multi")),
                "whether they may pick more than one value");
        assertTrue(text.contains("min 0 / 3"), "the selection bounds");
        assertTrue(text.contains("Vanilla"), "the options they would be offered");
    }

    @Test
    void aPerProductOverrideWinsOverTheBrandDefault() {
        ProductPreview preview = new ProductPreview();
        ProductCustomizationDto customization =
                customization("Toppings", SelectionType.MULTIPLE, false, 1, 2, "Vanilla");
        customization.setMinSelectionOverride(0);
        customization.setMaxSelectionOverride(4);

        preview.updatePreview(product("Americano"), List.of(), List.of(), List.of(customization));

        assertTrue(text(preview).contains("min 0 / 4"),
                "the card must show the bounds that would actually be enforced for this "
                        + "product, not the brand-wide default of 1 / 2");
    }

    @Test
    void theGroupIsHiddenWhenThereAreNoCustomizations() {
        ProductPreview preview = new ProductPreview();
        preview.updatePreview(product("Americano"), List.of(), List.of(), List.of());

        assertFalse(group(preview, "preview.group.customizations").isVisible(),
                "an empty group is just a heading with nothing under it");
    }

    @Test
    void severalCustomizationsAreAllShown() {
        ProductPreview preview = new ProductPreview();
        preview.updatePreview(product("Americano"), List.of(), List.of(),
                List.of(customization("Milk", SelectionType.SINGLE, true, 1, 1, "Oat"),
                        customization("Size", SelectionType.SINGLE, true, 1, 1, "Large"),
                        customization("Extras", SelectionType.MULTIPLE, false, 0, 9, "Shot")));

        String text = text(preview);
        assertTrue(text.contains("Milk") && text.contains("Size") && text.contains("Extras"),
                "a product can have several customizations and the card must not show only "
                        + "the first");
    }

    @Test
    void aCustomizationWithNoOptionsStillShowsItsName() {
        ProductPreview preview = new ProductPreview();
        preview.updatePreview(product("Americano"), List.of(), List.of(),
                List.of(customization("Gift note", SelectionType.SINGLE, false, 0, 1)));

        assertTrue(text(preview).contains("Gift note"));
    }

    /** The group whose heading is {@code titleKey}, identified by that heading alone. */
    private static Div group(Component root, String titleKey) {
        for (Component child : root.getChildren().toList()) {
            if (!(child instanceof Div div)
                    || !div.getClassNames().contains("product-preview-section")) {
                continue;
            }
            for (Component heading : div.getChildren().toList()) {
                if (heading.getElement().getText().equals(Messages.get(titleKey))) {
                    return div;
                }
            }
        }
        return null;
    }

    private static ProductCustomizationDto customization(String name, SelectionType type,
                                                        boolean required, int min, Integer max,
                                                        String... optionNames) {
        return ProductCustomizationDto.builder()
                .customizationId(1)
                .name(name)
                .selectionType(type)
                .required(required)
                .minSelection(min)
                .maxSelection(max)
                .options(java.util.Arrays.stream(optionNames)
                        .map(optionName -> CustomizationOptionDto.builder().name(optionName).build())
                        .toList())
                .build();
    }

    // ---------------------------------------------------------------- image

    @Test
    void itShowsTheHostedImageOnceThereIsOne() {
        ProductPreview preview = new ProductPreview();
        ProductDto product = product("Americano");
        ProductImageDto image = new ProductImageDto();
        image.setUrl("https://cdn.example.com/americano.jpg");
        product.setProductImageDto(image);

        preview.updatePreview(product, List.of(), List.of(), List.of());

        assertEquals("https://cdn.example.com/americano.jpg", previewImage(preview).getSrc());
    }

    @Test
    void itFallsBackToAPlaceholderWithoutAnImage() {
        ProductPreview preview = new ProductPreview();

        preview.updatePreview(product("Americano"), List.of(), List.of(), List.of());

        assertTrue(text(preview).contains(Messages.get("preview.product.noImage")),
                "with the image now shown only in this panel, it has to say when there "
                        + "is not one yet");
    }

    @Test
    void aBlankImageUrlCountsAsNoImage() {
        ProductPreview preview = new ProductPreview();
        ProductDto product = product("Americano");
        ProductImageDto image = new ProductImageDto();
        image.setUrl("   ");
        product.setProductImageDto(image);

        preview.updatePreview(product, List.of(), List.of(), List.of());

        assertTrue(text(preview).contains(Messages.get("preview.product.noImage")),
                "an all-whitespace url is not a picture, and Image.setSrc rejects it outright");
    }

    // ---------------------------------------------------------------- helpers

    private static List<Component> rows(Component root) {
        List<Component> rows = new java.util.ArrayList<>();
        collect(root, "product-preview-price-row", rows);
        collect(root, "product-preview-price-row-head", rows);
        return rows;
    }

    private static int countCellsWith(Component root, String className) {
        List<Component> found = new java.util.ArrayList<>();
        collect(root, className, found);
        return found.size();
    }

    private static void collect(Component root, String className, List<Component> found) {
        for (Component child : root.getChildren().toList()) {
            if (child.getClassNames().contains(className)) {
                found.add(child);
            }
            collect(child, className, found);
        }
    }

    private static com.vaadin.flow.component.html.Image previewImage(Component root) {
        return (com.vaadin.flow.component.html.Image) findByClass(root, "product-preview-image");
    }

    /**
     * All the text in a component, descendants included.
     *
     * <p>{@code Element.getText()} reads only an element's own text nodes, so on its own
     * it would return the empty string for a preview whose text sits several levels
     * down in nested layouts.</p>
     */
    private static String text(Component root) {
        StringBuilder text = new StringBuilder(root.getElement().getText());
        for (Component child : root.getChildren().toList()) {
            text.append(' ').append(text(child));
        }
        return text.toString();
    }

    private static com.vaadin.flow.component.html.Span categoryLine(Component root) {
        return (com.vaadin.flow.component.html.Span) findByClass(root, "text-s", "font-medium");
    }

    /**
     * The section wrapping the description.
     *
     * <p>The span itself is only ever hidden by its section, and a hidden parent does
     * not make a child report itself hidden, so the assertion has to be made where the
     * visibility is actually set.</p>
     */
    private static Component descriptionSection(Component root) {
        com.vaadin.flow.component.html.Span span =
                (com.vaadin.flow.component.html.Span) findByClass(root, "product-preview-description");
        return span.getParent().orElseThrow();
    }

    /** Depth-first search for a component carrying every one of {@code classNames}. */
    private static Component findByClass(Component root, String... classNames) {
        for (Component child : root.getChildren().toList()) {
            boolean matches = java.util.Arrays.stream(classNames)
                    .allMatch(className -> child.getClassNames().contains(className));
            if (matches) {
                return child;
            }
            try {
                return findByClass(child, classNames);
            } catch (AssertionError absent) {
                // keep looking
            }
        }
        throw new AssertionError("no component with classes " + String.join(" ", classNames));
    }

    private static ProductDto product(String name) {
        ProductDto product = new ProductDto();
        product.setName(name);
        return product;
    }

    private static CategoryDto category(String name) {
        CategoryDto category = new CategoryDto();
        category.setName(name);
        return category;
    }

    private static TierDto tier(Integer id, String name) {
        TierDto tier = new TierDto();
        tier.setId(id);
        tier.setName(name);
        return tier;
    }

    private static SkuTreeItem variant(String name, Double... pricesByTier) {
        Map<Integer, Double> prices = new HashMap<>();
        for (int i = 0; i < pricesByTier.length; i++) {
            prices.put(i + 1, pricesByTier[i]);
        }
        return SkuTreeItem.builder().skuName(name).tierPrices(prices).build();
    }

    private static SkuTreeItem variant(String name, Map<Integer, Double> prices) {
        return SkuTreeItem.builder().skuName(name).tierPrices(prices).build();
    }
}
