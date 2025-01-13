package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

@Data
public class TierProperties implements Serializable {
    private String brand;
    private String service;
    private String serviceByBrandId;
    private String menu;
    private String menuByBrandId;
    private TierMenuProperties menus;
    private TierServiceProperties services;

}
