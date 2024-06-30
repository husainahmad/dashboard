package com.harmoni.menu.dashboard.util;

import com.harmoni.menu.dashboard.dto.SkuDto;

import java.util.ArrayList;
import java.util.List;

public final class SkuUtil {

    private SkuUtil() {
    }

    public static List<Integer> getIdsByList(List<SkuDto> skus)  {
        List<Integer> skuIds = new ArrayList<>();
        for (SkuDto skuDto : skus) {
            skuIds.add(skuDto.getId());
        }
        return skuIds;
    }
}
