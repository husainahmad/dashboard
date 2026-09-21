package com.harmoni.menu.dashboard.layout.menu.product.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Payload for submitting the product edit form.
 *
 * <p>Carries the product identity, name, owning category and the edited SKU
 * rows. Transient view data; converted into {@link com.harmoni.menu.dashboard.dto.ProductDto}
 * for the REST layer.
 */
@Builder
@Data
public class ProductFormDto {
    /** The product id, or {@code null} for a new product. */
    private Integer id;
    /** The product display name. */
    private String name;
    /** The id of the owning category. */
    private Integer categoryId;
    /** The edited SKU rows of the product. */
    private List<ProductSkuFormDto> skus;
}
