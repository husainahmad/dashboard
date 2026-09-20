package com.harmoni.menu.dashboard.layout.organization.tier.menu;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.layout.organization.tier.TierForm;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Tier form specialized for menu tiers (type {@code MENU}). Adds the brand and
 * name fields plus the create/edit buttons from the shared {@link TierForm}
 * layout.
 */
@Getter
@Route("tier-menu-form")
@Slf4j
public class TierMenuForm extends TierForm {

    /**
     * Creates the menu tier form with the given REST clients.
     *
     * @param restClientOrganizationService     the synchronous REST client
     * @param asyncRestClientOrganizationService the async REST client
     */
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
