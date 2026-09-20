package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * Holds the REST URL used to update the service of a tier.
 *
 * <p>
 * Binds the {@code menu.url.tiers.services.update} key; consumed as the nested
 * {@code services} subtree of {@link TierProperties}.
 * </p>
 */

@Data
public class TierServiceProperties implements Serializable {
    private String update;
}
