package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * Store device endpoints.
 */
@Data
public class DeviceProperties implements Serializable {
    private String list;
    private String byId;
}
