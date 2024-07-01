package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProductProperties implements Serializable {
    private String bulk;
    private String sku;
    private String category;
}
