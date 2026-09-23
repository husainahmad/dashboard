package com.harmoni.menu.dashboard.event.brand;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BrandHandler;
import com.harmoni.menu.dashboard.layout.organization.brand.BrandForm;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Called when the user clicks Update on the brand form: validates the binder,
 * posts the updated brand, shows a success toast and closes the form.
 */
@RequiredArgsConstructor
@Slf4j
public class BrandUpdateEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final BrandForm brandForm;
    private final RestClientOrganizationService restClientOrganizationService;

    /**
     * Validates the brand form and, if valid, posts the updated brand.
     *
     * @param buttonClickEvent the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.brandForm.getBinder().validate().hasErrors()) {
            return;
        }
        BrandDto brandDto = this.brandForm.getBrandDto();
        brandDto.setName(this.brandForm.getBrandNameField().getValue());
        restClientOrganizationService.updateBrand(brandDto)
                .doOnError(error -> new BrandHandler(this.brandForm.getUi(),
                        Messages.get(Messages.Keys.NOTIFICATION_BRAND_INSERT_ERROR, error.getMessage())))
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        this.brandForm.getUi().access(()->{
            UiUtil.success(Messages.get(Messages.Keys.NOTIFICATION_BRAND_CREATED));
            this.brandForm.close();
            broadcastMessage(BroadcastMessage.BRAND_SUCCESS_UPDATED, restAPIResponse);
        });
    }
}
