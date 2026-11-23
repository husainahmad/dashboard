package com.harmoni.menu.dashboard.layout.crm;

import com.harmoni.menu.dashboard.dto.growth.LoyaltyAccountDto;
import com.harmoni.menu.dashboard.dto.growth.LoyaltyTransactionDto;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.component.StatCard;
import com.harmoni.menu.dashboard.service.data.rest.GrowthRestClientService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;

public class LoyaltyView extends AbstractGrowthView {

    private final GrowthRestClientService client;
    private final IntegerField customerId = new IntegerField(Messages.get("growth.customerId"));
    private final IntegerField points = new IntegerField(Messages.get("growth.points"));
    private final StatCard balance = new StatCard(VaadinIcon.MONEY, Messages.get("growth.balance"));
    private final StatCard lifetime = new StatCard(VaadinIcon.TROPHY, Messages.get("growth.lifetime"));
    private final StatCard tier = new StatCard(VaadinIcon.STAR, Messages.get("growth.tier"));
    private final Grid<LoyaltyTransactionDto> history = new Grid<>(LoyaltyTransactionDto.class, false);

    public LoyaltyView(GrowthRestClientService client) {
        super("loyalty-view");
        this.client = client;
        customerId.setValue(1);
        points.setValue(100);

        history.addColumn(LoyaltyTransactionDto::type).setHeader(Messages.get("growth.type"));
        history.addColumn(LoyaltyTransactionDto::points).setHeader(Messages.get("growth.points"));
        history.addColumn(LoyaltyTransactionDto::orderId).setHeader(Messages.get("growth.orderId"));
        history.addColumn(LoyaltyTransactionDto::createdAt).setHeader("Date");

        render(PageHeader.of("page.loyalty.title", "page.loyalty.description"),
                toolbar(customerId, points,
                        button("growth.load", VaadinIcon.SEARCH, event -> load()),
                        button("growth.redeem", VaadinIcon.REFRESH, event -> redeem())),
                cardRow(balance, lifetime, tier),
                gridSlot(history));
    }

    private void load() {
        try {
            show(client.loyaltyAccount(customerId.getValue()));
            history.setItems(client.loyaltyHistory(customerId.getValue()));
        } catch (Exception e) {
            loadError(this::load);
        }
    }

    private void redeem() {
        try {
            show(client.redeemPoints(customerId.getValue(), points.getValue()));
            history.setItems(client.loyaltyHistory(customerId.getValue()));
        } catch (Exception e) {
            actionError(this::redeem);
        }
    }

    private void show(LoyaltyAccountDto account) {
        balance.setValue(String.valueOf(account.pointsBalance()));
        lifetime.setValue(String.valueOf(account.lifetimePoints()));
        tier.setValue(account.tier());
    }
}
