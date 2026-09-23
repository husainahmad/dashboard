package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * Holds the REST URLs of the setting endpoints.
 *
 * <p>
 * Binds the {@code setting.url.service} and {@code setting.url.table} keys;
 * consumed as the {@code url} subtree of {@link SettingProperties}.
 * </p>
 */

@Data
public class SettingUrlProperties implements Serializable {
    private String service;
    private String table;
    private String tableByStore;
}
