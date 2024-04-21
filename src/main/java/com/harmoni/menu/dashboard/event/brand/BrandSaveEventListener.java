package com.harmoni.menu.dashboard.event.brand;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.layout.organization.brand.BrandForm;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrandSaveEventListener implements ComponentEventListener<ClickEvent<Button>> {

    private final static Logger log = LoggerFactory.getLogger(BrandSaveEventListener.class);
    private final BrandForm brandForm;

    private final RestClientOrganizationService restClientOrganizationService;

    public BrandSaveEventListener(BrandForm brandForm, RestClientOrganizationService restClientOrganizationService) {
        this.brandForm = brandForm;
        this.restClientOrganizationService = restClientOrganizationService;
    }
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.brandForm.getBinder().validate().hasErrors()) {
            return;
        };

        BrandDto brandDto = this.brandForm.getBrandDto();
        brandDto.setName(this.brandForm.getBrandNameField().getValue());
        brandDto.setChainId(this.brandForm.getChainBox().getValue().getId());
        restClientOrganizationService.createBrand(brandDto)
                .subscribe(this::accept);
    }

    private void reject(Throwable throwable) {
        log.error("Error {}", "Brand", throwable);
        Broadcaster.broadcast(BroadcastMessage.BRAND_INSERT_FAILED);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        Broadcaster.broadcast(BroadcastMessage.BRAND_INSERT_SUCCESS);
    }

}
