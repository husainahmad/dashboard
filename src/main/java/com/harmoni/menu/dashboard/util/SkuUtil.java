package com.harmoni.menu.dashboard.util;

import com.harmoni.menu.dashboard.dto.SkuDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Small helpers for working with {@link SkuDto} collections.
 */
public final class SkuUtil {

    private SkuUtil() {
    }

    /**
     * Collects the ids of the given SKUs.
     *
     * @param skus the SKU list
     * @return the ids in the same order
     */
    public static List<Integer> getIdsByList(List<SkuDto> skus)  {
        List<Integer> skuIds = new ArrayList<>();
        for (SkuDto skuDto : skus) {
            skuIds.add(skuDto.getId());
        }
        return skuIds;
    }
}
