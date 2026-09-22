package com.harmoni.menu.dashboard.layout.organization.tier.price;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.organization.tier.TierForm;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Tier form specialized for price tiers (type {@code PRICE}), hosted in a
 * {@link TabManager} tab. Adds the brand and name fields, binds them, and
 * uses the brands preloaded by the list view.
 */
@Route("tier-price-form")
@Slf4j
public class TierPriceForm extends TierForm {

    /**
     * Creates the price tier form for the given tab.
     *
     * @param restClientOrganizationService     the synchronous REST client
     * @param asyncRestClientOrganizationService the async REST client
     * @param tabManager                        the manager of the hosting tab sheet
     * @param currentTab                        the tab showing this form
     * @param formAction                        whether the form creates or edits
     * @param tierDto                           the tier to bind, or a new one to create
     * @param brands                            the brands to offer in the combo box
     */
    public TierPriceForm(RestClientOrganizationService restClientOrganizationService,
                         AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                         TabManager tabManager, Tab currentTab, FormAction formAction, TierDto tierDto,
                         List<BrandDto> brands) {
        super(restClientOrganizationService, asyncRestClientOrganizationService, tabManager, currentTab, formAction, tierDto, brands);
        addValidation();
        brandBox.setItemLabelGenerator(BrandDto::getName);
        add(brandBox);
        add(tierNameField);
        add(createButtonsLayout(true, true));
        getBinder().bindInstanceFields(this);
    }

}
