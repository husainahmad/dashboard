package com.harmoni.menu.dashboard.layout.setting.service;

import com.harmoni.menu.dashboard.dto.SkuDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ServiceTreeItem {
    private String id;
    private String name;
    private String serviceName;
    private String subServiceName;
    private boolean active;

    private List<SkuDto> skus;
}
