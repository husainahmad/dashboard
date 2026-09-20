package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * Holds the REST URL used to update the menu of a tier.
 *
 * <p>
 * Binds the {@code menu.url.tiers.menus.update} key; consumed as the nested
 * {@code menus} subtree of {@link TierProperties}.
 * </p>
 */

@Data
public class TierMenuProperties implements Serializable {
    private String update;
}
