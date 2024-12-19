package com.harmoni.menu.dashboard.event.product;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.SkuDto;
import com.harmoni.menu.dashboard.dto.SkuTierPriceDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.menu.product.ProductForm;
import com.harmoni.menu.dashboard.layout.menu.product.SkuTreeItem;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
@Slf4j
public class ProductSaveEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final transient ProductForm productForm;
    private final RestClientMenuService restClientMenuService;

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (productForm.getBinder().validate().isOk()) {
            restClientMenuService.saveProduct(populatePayload()).subscribe(this::accept);
        }
    }

    private void accept(RestAPIResponse restAPIResponse) {
        if (restAPIResponse.getHttpStatus() == HttpStatus.CREATED.value()) {
            broadcastMessage(BroadcastMessage.PRODUCT_UPDATE_SUCCESS, restAPIResponse);
            this.productForm.removeFromSheet();
        }
    }

    private ProductDto populatePayload() {
        ProductDto productDto = new ProductDto();
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
        skuTierPriceDto.get().setTierId(childSkuTreeItem.getTierId());
        skuTierPriceDto.get().setPrice(productForm.getSkuTierPrices().get(childSkuTreeItem.getId()) != null ?
                productForm.getSkuTierPrices().get(childSkuTreeItem.getId()) : 0.0);
        skuTierPriceDtos.add(skuTierPriceDto.get());
    }
}
