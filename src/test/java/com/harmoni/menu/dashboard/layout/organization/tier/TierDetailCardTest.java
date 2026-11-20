package com.harmoni.menu.dashboard.layout.organization.tier;

import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.support.UiTestSupport;
import com.harmoni.menu.dashboard.util.Messages;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class TierDetailCardTest {

    @Test
    void readOnlyCardHasNoSaveOrDiscardBar() {
        TierDetailCard card = card(false);
        card.show(row(), Map.of());

        assertNull(UiTestSupport.buttonByText(card, Messages.get("tier.saveChanges")));
        assertNull(UiTestSupport.buttonByText(card, Messages.get("tier.discard")));
    }

    @Test
    void editableCardKeepsTheActionBar() {
        TierDetailCard card = card(true);
        card.show(row(), Map.of());

        assertNotNull(UiTestSupport.buttonByText(card, Messages.get("tier.saveChanges")));
        assertNotNull(UiTestSupport.buttonByText(card, Messages.get("tier.discard")));
    }

    private static TierDetailCard card(boolean canEdit) {
        return new TierDetailCard(mock(AsyncRestClientMenuService.class),
                mock(RestClientMenuService.class),
                mock(AsyncRestClientOrganizationService.class),
                mock(RestClientOrganizationService.class),
                () -> { }, canEdit);
    }

    private static TierRow row() {
        TierDto tier = new TierDto();
        tier.setId(1);
        tier.setName("Retail");
        tier.setBrandId(1);
        tier.setType(TierTypeDto.PRICE);
        return new TierRow(tier, 0);
    }
}
