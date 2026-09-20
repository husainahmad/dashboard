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

/**
 * Called when the user clicks Save on the product form: validates the binder,
 * builds the product payload and posts it, closing the editor tab and
 * broadcasting the result on success.
 */
@RequiredArgsConstructor
@Slf4j
public class ProductSaveEventListener extends ProductEventListener implements
        ComponentEventListener<ClickEvent<Button>>  {

    private final ProductForm productForm;
    private final RestClientMenuService restClientMenuService;

    /**
     * Validates the product form and, if valid, posts the new product; on a
     * created response it broadcasts the result and closes the editor.
     *
     * @param buttonClickEvent the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        this.setProductForm(productForm);

        if (!this.getProductForm().getBinder().validate().isOk()) {
            return;
        }
        this.getProductForm().onSaveStart();
        this.restClientMenuService.saveProduct(this.populatePayload())
                .subscribe(this::acceptResponseSave, this::onSaveError);
    }

    private void acceptResponseSave(RestAPIResponse restAPIResponse) {
        if (restAPIResponse.getHttpStatus() == HttpStatus.CREATED.value()) {
            broadcastMessage(BroadcastMessage.PRODUCT_UPDATE_SUCCESS, restAPIResponse);
            this.acceptResponse();
            return;
        }
        this.getProductForm().onSaveEnd();
    }

    private void onSaveError(Throwable throwable) {
        log.error("Save product failed", throwable);
        this.getProductForm().onSaveEnd();
    }
}
