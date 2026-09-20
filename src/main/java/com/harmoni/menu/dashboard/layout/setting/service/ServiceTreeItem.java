package com.harmoni.menu.dashboard.layout.setting.service;

import com.harmoni.menu.dashboard.dto.SkuDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Tree node rendered in the {@link ServiceListView} grid: a service row with
 * an optional nested sub-service child. Plain data holder populated from the
 * service REST responses via the {@code @Builder}.
 */
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
