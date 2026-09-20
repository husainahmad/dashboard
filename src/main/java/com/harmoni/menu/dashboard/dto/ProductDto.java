package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Represents a product within a category, including its SKUs and image.
 *
 * <p>Used both as the create/update payload and as the response body for the
 * {@code /api/v1/product} endpoints. Products support soft deletion, so a
 * non-null {@code deletedAt} means the row is no longer active.
 */

@Data
public class ProductDto {

    /** The database id, or {@code null} when the product has not been saved yet. */

    private Integer id;

    /** The display name of the product. */

    private String name;

    /** Optional free-text description, bound to the {@code description} JSON property. */

    @JsonProperty("description")
    private String description;

    /** The id of the owning category, matching the entity carried by {@link #categoryDto}. */

    private Integer categoryId;

    /** Nested category details, bound to the {@code category} JSON property. */

    @JsonProperty("category")
    private CategoryDto categoryDto;

    /** The product SKUs with their tier prices, bound to the {@code skus} JSON property. */

    @JsonProperty("skus")
    private List<SkuDto> skuDtos;

    /** The image attached to the product, bound to the {@code productImage} JSON property. */

    @JsonProperty("productImage")
    private ProductImageDto productImageDto;

    /** The ids of the customizations attached to the product, bound to the {@code customizationIds} JSON property. */

    @JsonProperty("customizationIds")
    private List<Integer> customizationIds;

    /** The timestamp the product was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;

    /** Soft-delete timestamp; {@code null} while the product is active. */

    private Date deletedAt;
}