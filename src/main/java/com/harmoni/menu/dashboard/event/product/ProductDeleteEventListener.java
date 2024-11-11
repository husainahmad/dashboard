package com.harmoni.menu.dashboard.event.product;

import com.harmoni.menu.dashboard.dto.SkuDto;
import com.harmoni.menu.dashboard.layout.menu.product.ProductDialogEdit;
import com.harmoni.menu.dashboard.layout.menu.product.binder.ProductBinderBean;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Slf4j
public class ProductDeleteEventListener implements ComponentEventListener<ClickEvent<Button>> {
    private final BeanValidationBinder<ProductBinderBean> binder;
    private final ProductDialogEdit productDialogEdit;
    private final RestClientMenuService restClientMenuService;
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {

        if (!this.productDialogEdit.getProductTreeItem().getSkus().isEmpty()) {
            for (SkuDto skuDto: this.productDialogEdit.getProductTreeItem().getSkus()) {
                if (ObjectUtils.isNotEmpty(skuDto) &&
                        ObjectUtils.isNotEmpty(this.binder.getBean()) &&
                        skuDto.getId().equals(this.binder.getBean().getSkuId())) {
                    this.productDialogEdit.getConfirmDialog().removeAll();
                    setConfirmDialogDelete(skuDto, binder);
                }
            }
        }
    }

    private void setConfirmDialogDelete(SkuDto skuDto, BeanValidationBinder<ProductBinderBean> binder) {
        if (skuDto.getId()>0) {
            this.productDialogEdit.getConfirmDialog().setHeader("Confirm to remove SKU");
            this.productDialogEdit.getConfirmDialog().setText("Do you want to remove the SKU %s?".formatted(skuDto.getName()));
            this.productDialogEdit.getConfirmDialog().setCancelable(true);
            this.productDialogEdit.getConfirmDialog().addConfirmListener(confirmEvent ->
                    callRemoveAPI(skuDto, binder, confirmEvent));
            this.productDialogEdit.getConfirmDialog().open();
        } else {
            removeSkuFromListDisplay(skuDto, binder);
        }
    }

    private void removeSkuFromListDisplay(SkuDto skuDto, BeanValidationBinder<ProductBinderBean> binder) {
        this.productDialogEdit.getProductTreeItem().getSkus().remove(skuDto);
        this.productDialogEdit.getBinders().remove(binder);
        this.productDialogEdit.setDataProvider(this.productDialogEdit.getProductTreeItem().getSkus());
    }

    private void callRemoveAPI(SkuDto skuDto, BeanValidationBinder<ProductBinderBean> binder,
                               ConfirmDialog.ConfirmEvent confirmEvent) {
        log.debug("confirmEvent {}", confirmEvent);
        this.restClientMenuService.deleteSku(skuDto).subscribe(restAPIResponse -> {
            if (restAPIResponse.getHttpStatus() == HttpStatus.NO_CONTENT.value()) {
                removeSkuFromListDisplay(skuDto, binder);
            }
        });
    }

}
