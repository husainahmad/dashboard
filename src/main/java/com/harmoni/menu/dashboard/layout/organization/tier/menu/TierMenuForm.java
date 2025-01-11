package com.harmoni.menu.dashboard.layout.organization.tier.menu;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.layout.organization.tier.TierForm;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Route("tier-menu-form")
@Slf4j
public class TierMenuForm extends TierForm {

    public TierMenuForm(RestClientOrganizationService restClientOrganizationService,
                        AsyncRestClientOrganizationService asyncRestClientOrganizationService) {
        super(restClientOrganizationService, asyncRestClientOrganizationService);
        brandBox.setItemLabelGenerator(BrandDto::getName);
        add(brandBox);
        add(tierNameField);
        add(createButtonsLayout(true, true));
        addValidation();
    }


}
