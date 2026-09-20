package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * Holds the REST URL used to list the categories of a brand.
 *
 * <p>
 * Binds the {@code menu.url.categories.brand} key; consumed as the
 * {@code categories} subtree of {@link UrlProperties}.
 * </p>
 */

@Data
public class CategoryProperties implements Serializable {
    private String brand;
}
