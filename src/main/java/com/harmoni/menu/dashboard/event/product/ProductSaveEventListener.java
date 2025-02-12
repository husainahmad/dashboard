package com.harmoni.menu.dashboard.event.product;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.layout.menu.product.ProductForm;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Slf4j
public class ProductSaveEventListener extends ProductEventListener implements
        ComponentEventListener<ClickEvent<Button>>  {

    private final ProductForm productForm;
    private final RestClientMenuService restClientMenuService;

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        this.setProductForm(productForm);

        if (this.getProductForm().getBinder().validate().isOk()) {
            this.restClientMenuService.saveProduct(this.populatePayload()).subscribe(this::acceptResponseSave);
        }
    }

    private void acceptResponseSave(RestAPIResponse restAPIResponse) {
        if (restAPIResponse.getHttpStatus() == HttpStatus.CREATED.value()) {
            broadcastMessage(BroadcastMessage.PRODUCT_UPDATE_SUCCESS, restAPIResponse);
            this.acceptResponse();
        }
    }
}
