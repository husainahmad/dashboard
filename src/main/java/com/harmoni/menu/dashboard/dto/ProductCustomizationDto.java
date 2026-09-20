package com.harmoni.menu.dashboard.dto;

import com.harmoni.menu.dashboard.layout.enums.SelectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A customization attached to a product, merged with the master definition.
 *
 * <p>Returned by the product customization read endpoints; the master
 * properties are copied from the customization definition while the
 * {@code *Override} fields reflect the per-product configuration and stay
 * {@code null} when inherited.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCustomizationDto {

    /** The id of the product-customization link, or {@code null} when not saved yet. */

    private Integer id;

    /** The id of the product this customization is attached to. */

    private Integer productId;

    /** The id of the referenced {@link CustomizationDto} master. */

    private Integer customizationId;

    /** The customization name, copied from the master definition. */

    private String name;

    /** The customization description, copied from the master definition. */

    private String description;

    /** How many options may be selected, copied from the master definition. */

    private SelectionType selectionType;

    /** The id of the brand that owns the customization master. */

    private Integer brandId;

    /** Whether a selection is mandatory, copied from the master definition. */

    private Boolean required;

    /** Master minimum selection count; {@code null} when unrestricted. */

    private Integer minSelection;

    /** Master maximum selection count; {@code null} when unrestricted. */

    private Integer maxSelection;

    /** Per-product override of {@link #required}, or {@code null} to inherit. */

    private Boolean requiredOverride;

    /** Per-product override of {@link #minSelection}, or {@code null} to inherit. */

    private Integer minSelectionOverride;

    /** Per-product override of {@link #maxSelection}, or {@code null} to inherit. */

    private Integer maxSelectionOverride;

    /** Sort position of this attachment among the product's customizations. */

    private Integer sortOrder;

    /** The selectable options, each with its per-tier prices. */

    private List<CustomizationOptionDto> options;

}