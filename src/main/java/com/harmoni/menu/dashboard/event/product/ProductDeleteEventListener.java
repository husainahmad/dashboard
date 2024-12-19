package com.harmoni.menu.dashboard.event.product;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.menu.product.ProductTreeItem;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Slf4j
public class ProductDeleteEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final RestClientMenuService restClientMenuService;
    private final transient ProductTreeItem productTreeItem;

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        setConfirmDialogDelete();
    }

    private void setConfirmDialogDelete() {

        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Confirmation");
        confirmDialog.setText("Do you want to remove this product ".concat(productTreeItem.getName()).concat("?"));
        confirmDialog.setCancelable(true);
        confirmDialog.addConfirmListener(confirmEvent -> callRemoveAPI());
        confirmDialog.open();
    }

    private void callRemoveAPI() {
        ProductDto productDto = new ProductDto();
        productDto.setId(productTreeItem.getProductId());
        this.restClientMenuService.deleteProduct(productDto).subscribe(restAPIResponse -> {
            if (restAPIResponse.getHttpStatus() == HttpStatus.OK.value()) {
                broadcastMessage(BroadcastMessage.PRODUCT_UPDATE_SUCCESS, restAPIResponse);
            }
        });
    }

}
