package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

@Data
public class ImageProperties implements Serializable {
    private String upload;
    private String uploadUpdate;
    private String prefix;
}
