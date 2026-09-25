package com.harmoni.menu.dashboard.event.product;

import com.harmoni.menu.dashboard.dto.ProductCustomizationDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.ProductImageDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.menu.product.ProductForm;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;

/**
 * Shared logic for the product save/update listeners: builds the payload from
 * the current {@link ProductForm} state and closes the editor tab on success.
 * The save vs update behaviour itself lives in the concrete listener classes.
 */
@Setter
@Getter
@RequiredArgsConstructor
@Slf4j
public class ProductEventListener implements
        BroadcastMessageService {

    private ProductForm productForm;

    /**
     * Assembles the {@link ProductDto} that is sent to the product API, reading
     * sku rows and tier prices from {@code productForm.getSkuSection()} and the
     * customization attachment ids from {@code productForm.getCustomizationSection()}.
     *
     * <p>
     * Attachment ids are only included for a not-yet-saved product; a persisted
     * product manages customizations through its own endpoints.
     * </p>
     *
     * @return the payload to persist
     */
    public ProductDto populatePayload() {
        ProductDto productDto = new ProductDto();
        if (ObjectUtils.isNotEmpty(productForm.getProductDto())) {
            productDto = productForm.getProductDto();
        }
        productDto.setCategoryId(productForm.getCategoryBox().getValue().getId());
        productDto.setName(productForm.getProductNameField().getValue());
        productDto.setDescription(productForm.getProductDescTextArea().getValue());

        List<com.harmoni.menu.dashboard.dto.SkuDto> skuDtos = productForm.getSkuSection().toSkuDtos();
        productDto.setSkuDtos(skuDtos);

        // Only a not-yet-saved product carries the customization attachment list;
        // existing products manage the relationship via the customization endpoints
        // so per-product settings are never wiped by a plain product update.
        if (ObjectUtils.isEmpty(productDto.getId())) {
            productDto.setCustomizationIds(productForm.getCustomizationSection().getProductCustomizations().stream()
                    .map(ProductCustomizationDto::getCustomizationId)
                    .toList());
        }
        if (ObjectUtils.isNotEmpty(productForm.getProductImageUploadView())
                && ObjectUtils.isNotEmpty(productForm.getProductImageUploadView().getProductImageDto())) {
            ProductImageDto productImageDto = productForm.getProductImageUploadView().getProductImageDto();
            productDto.setProductImageDto(productImageDto);
        }
        return productDto;
    }

    /**
     * Closes the editor tab after a successful save/update broadcast.
     */
    public void acceptResponse() {
        this.getProductForm().removeFromSheet();
    }
}