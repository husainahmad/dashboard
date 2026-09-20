package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * Holds the REST URLs of the product image endpoints.
 *
 * <p>
 * Binds the {@code menu.url.products.images.*} keys for the upload, update and
 * prefix URLs; consumed as the {@code images} subtree of
 * {@link ProductProperties}.
 * </p>
 */

@Data
public class ImageProperties implements Serializable {
    private String upload;
    private String uploadUpdate;
    private String prefix;
}
