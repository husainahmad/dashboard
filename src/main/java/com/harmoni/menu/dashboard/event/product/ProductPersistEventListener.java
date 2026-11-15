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
import reactor.core.publisher.Mono;

/**
 * Called when the user clicks Save (new product) or Update (existing product) on
 * the product form: validates the binder and SKU rows, builds the payload and
 * posts it, closing the editor tab and broadcasting the result on success.
 *
 * <p>Save and update answer different endpoints and treat each other's status as
 * a failure, so the mode is fixed at construction time rather than inferred.</p>
 */
@RequiredArgsConstructor
@Slf4j
public class ProductPersistEventListener extends ProductEventListener implements
        ComponentEventListener<ClickEvent<Button>> {

    private final ProductForm productForm;
    private final RestClientMenuService restClientMenuService;
    private final boolean update;

    /**
     * Validates the product form and, if valid, posts the product; on the expected
     * response it broadcasts the result and closes the editor.
     *
     * @param buttonClickEvent the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        this.setProductForm(productForm);

        if (!this.getProductForm().getBinder().validate().isOk()) {
            return;
        }
        if (!this.getProductForm().getSkuSection().validate()) {
            return;
        }
        this.getProductForm().onSaveStart();
        Mono<RestAPIResponse> call = update
                ? restClientMenuService.updateProduct(this.populatePayload())
                : restClientMenuService.saveProduct(this.populatePayload());
        call.subscribe(this::accept, this::onError);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        int expectedStatus = update ? HttpStatus.OK.value() : HttpStatus.CREATED.value();
        if (restAPIResponse.getHttpStatus() == expectedStatus) {
            broadcastMessage(update
                    ? BroadcastMessage.PRODUCT_UPDATE_SUCCESS
                    : BroadcastMessage.PRODUCT_INSERT_SUCCESS, restAPIResponse);
            this.acceptResponse();
            return;
        }
        this.getProductForm().onSaveEnd();
    }

    private void onError(Throwable throwable) {
        log.error("{} product failed", update ? "Update" : "Save", throwable);
        this.getProductForm().onSaveEnd();
    }
}
