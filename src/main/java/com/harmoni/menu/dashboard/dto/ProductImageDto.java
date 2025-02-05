package com.harmoni.menu.dashboard.dto;

import lombok.Data;

import java.util.Date;

@Data
public class ProductImageDto {
    private Integer id;
    private Integer productId;
    private String fileName;
    private byte[] imageBlob;
    private String mimeType;
    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;
}
