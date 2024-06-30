package com.harmoni.menu.dashboard.event.product;

import com.harmoni.menu.dashboard.layout.menu.product.*;
import com.harmoni.menu.dashboard.layout.menu.product.binder.ProductBinderBean;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Slf4j
public class ProductUpdateEventListener implements ComponentEventListener<ClickEvent<Button>> {

    private final ProductDialogEdit productDialogEdit;
    private final RestClientMenuService restClientMenuService;

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        boolean[] isAllValid = new boolean[productDialogEdit.getBinders().size()];

        int i = 0;
        for (Binder<ProductBinderBean> productBinderBeanBinder : productDialogEdit.getBinders()) {
            BinderValidationStatus<ProductBinderBean> binderBeanBinderValidationStatus =
                    productBinderBeanBinder.validate();
            if (binderBeanBinderValidationStatus.isOk()
                    && isSkuNameEqualsTo(productBinderBeanBinder, i, productDialogEdit.getBinders())) {
                isAllValid[i] = true;
            }
            i++;
        }

        for (i = 0; i < isAllValid.length; i++) {
            if (!isAllValid[i]) {
                return;
            }
        }

        List<ProductSkuFormDto> skuFormDtos = new ArrayList<>();
        productDialogEdit.getBinders().forEach(productBeanBinder -> skuFormDtos.add(ProductSkuFormDto.builder()
                .id(productBeanBinder.getBean().getSkuId())
                .name(productBeanBinder.getBean().getSkuName())
                .tierPrice(ProductSkuTierPriceFormDto.builder()
                        .id(productBeanBinder.getBean().getTierId())
                        .price(productBeanBinder.getBean().getPrice())
                        .build())
                .build()));

        final ProductFormDto productFormDto = ProductFormDto.builder()
                .id(productDialogEdit.getProductTreeItem().getProductId())
                .name(productDialogEdit.getProductNameField().getValue())
                .categoryId(productDialogEdit.getCategoryBox().getValue().getId())
                .skus(skuFormDtos)
                .build();

        restClientMenuService.saveProductBulk(productFormDto).subscribe(restAPIResponse -> {
            if (restAPIResponse.getHttpStatus() == HttpStatus.OK.value()) {
                productDialogEdit.getUi().access(productDialogEdit::close);
            }
        });
    }

    private boolean isSkuNameEqualsTo(Binder<ProductBinderBean> productBinderBean, int index,
                                      List<Binder<ProductBinderBean>> currentBinder) {
        int i = 0;
        for (Binder<ProductBinderBean> productBinderBeanBinder : currentBinder) {

            if (!ObjectUtils.isEmpty(productBinderBeanBinder.getBean())
                    && productBinderBeanBinder.getBean().getSkuName().equals(productBinderBean.getBean().getSkuName())
                    && i!=index) {
                return false;
            }
            i++;
        }
        return true;
    }

}
