package com.harmoni.menu.dashboard.event.product;

import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.SkuDto;
import com.harmoni.menu.dashboard.dto.SkuTierPriceDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.menu.product.ProductForm;
import com.harmoni.menu.dashboard.layout.menu.product.SkuTreeItem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Setter
@Getter
@RequiredArgsConstructor
@Slf4j
public class ProductEventListener implements
        BroadcastMessageService {

    private ProductForm productForm;

    public ProductDto populatePayload() {
        ProductDto productDto = new ProductDto();
        if (ObjectUtils.isNotEmpty(productForm.getProductDto())) {
            productDto = productForm.getProductDto();
        }
        productDto.setCategoryId(productForm.getCategoryBox().getValue().getId());
        productDto.setName(productForm.getProductNameField().getValue());
        productDto.setDescription(productForm.getProductDescTextArea().getValue());

        List<SkuDto> skuDtos = new ArrayList<>();

        productForm.getSkuDtoGrid().getTreeData().getRootItems().forEach(skuTreeItem ->
                skuDtos.add(getSkuDto(skuTreeItem)));

        productDto.setSkuDtos(skuDtos);
        return productDto;
    }

    private SkuDto getSkuDto(SkuTreeItem skuTreeItem) {
        SkuDto skuDto = new SkuDto();
        skuDto.setId(skuTreeItem.getSkuId());
        skuDto.setName(productForm.getSkuNames().get(skuTreeItem.getId()) != null ?
                productForm.getSkuNames().get(skuTreeItem.getId()) : "");
        skuDto.setActive(true);
        skuDto.setDescription(productForm.getSkuDescs().get(skuTreeItem.getId()) != null ?
                productForm.getSkuDescs().get(skuTreeItem.getId()) : "");
        extractedListSkuTierPrice(skuTreeItem, skuDto);
        return skuDto;
    }

    private void extractedListSkuTierPrice(SkuTreeItem skuTreeItem, SkuDto skuDto) {
        List<SkuTierPriceDto> skuTierPriceDtos = new ArrayList<>();

        //get Root value
        AtomicReference<SkuTierPriceDto> skuTierPriceDto = new AtomicReference<>(new SkuTierPriceDto());
        skuTierPriceDto.get().setSkuId(skuDto.getId());
        skuTierPriceDto.get().setTierId(skuTreeItem.getTierId());
        skuTierPriceDto.get().setPrice(productForm.getSkuTierPrices().get(skuTreeItem.getId()) != null ?
                productForm.getSkuTierPrices().get(skuTreeItem.getId()) : 0.0);
        skuTierPriceDtos.add(skuTierPriceDto.get());

        productForm.getSkuDtoGrid().getTreeData().getChildren(skuTreeItem).forEach(childSkuTreeItem ->
                extractedSkuTierPrice(childSkuTreeItem, skuTierPriceDto, skuTierPriceDtos));

        skuDto.setSkuTierPriceDtos(skuTierPriceDtos);
    }

    private void extractedSkuTierPrice(SkuTreeItem childSkuTreeItem, AtomicReference<SkuTierPriceDto> skuTierPriceDto,
                                       List<SkuTierPriceDto> skuTierPriceDtos) {
        skuTierPriceDto.set(new SkuTierPriceDto());
        skuTierPriceDto.get().setSkuId(childSkuTreeItem.getSkuId());
        skuTierPriceDto.get().setTierId(childSkuTreeItem.getTierId());
        skuTierPriceDto.get().setPrice(productForm.getSkuTierPrices().get(childSkuTreeItem.getId()) != null ?
                productForm.getSkuTierPrices().get(childSkuTreeItem.getId()) : 0.0);
        skuTierPriceDtos.add(skuTierPriceDto.get());
    }

    public void acceptResponse() {
        this.getProductForm().removeFromSheet();
    }
}
