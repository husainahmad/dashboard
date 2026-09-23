package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * Holds the REST URL used to fetch the chain a user belongs to.
 *
 * <p>
 * Binds the {@code menu.url.users.chain} key; consumed as the {@code users}
 * subtree of {@link UrlProperties}.
 * </p>
 */

@Data
public class UserProperties implements Serializable {
    private String chain;
    private String chainPage;
    private String byId;
    private String byName;
}
