package com.harmoni.menu.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;
import java.io.Serializable;

@Data
@Builder
public class ImageDto implements Serializable  {
    private Integer id;
    private transient InputStream fileStream;
    private String fileName;
    private String mimeType;
}
