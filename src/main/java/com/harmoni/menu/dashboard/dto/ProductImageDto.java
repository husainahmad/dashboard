package com.harmoni.menu.dashboard.dto;

import lombok.Data;

import java.util.Date;

/**
 * Metadata of the image attached to a product.
 *
 * <p>Returned inline in {@link ProductDto#productImageDto}; the raw bytes are
 * served from {@code /api/v1/product/image/{id}}.
 */

@Data
public class ProductImageDto {

    /** The database id, or {@code null} when the image has not been saved yet. */

    private Integer id;

    /** The id of the product the image belongs to. */

    private Integer productId;

    /** The stored file name of the image. */

    private String fileName;

    /** The raw image bytes, present when the image is delivered inline; otherwise {@code null}. */

    private byte[] imageBlob;

    /** The content type of the image, such as {@code image/png}. */

    private String mimeType;

    /** The timestamp the image was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;

    /** Soft-delete timestamp; {@code null} while the image is active. */

    private Date deletedAt;
}
