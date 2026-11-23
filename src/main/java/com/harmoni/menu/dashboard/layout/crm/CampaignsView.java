package com.harmoni.menu.dashboard.layout.crm;

import com.harmoni.menu.dashboard.dto.growth.CampaignDto;
import com.harmoni.menu.dashboard.dto.growth.CreateCampaignRequest;
import com.harmoni.menu.dashboard.dto.growth.VoucherDto;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.service.data.rest.GrowthRestClientService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;

public class CampaignsView extends AbstractGrowthView {

    private final GrowthRestClientService client;
    private final TextField code = new TextField(Messages.get("growth.code"));
    private final TextField name = new TextField(Messages.get("growth.name"));
    private final ComboBox<String> type = new ComboBox<>(Messages.get("growth.type"),
            "DISCOUNT", "POINTS_MULTIPLIER", "CASHBACK", "VOUCHER");
    private final IntegerField campaignId = new IntegerField(Messages.get("growth.campaignId"));
    private final IntegerField customerId = new IntegerField(Messages.get("growth.customerId"));
    private final TextField voucherCode = new TextField(Messages.get("growth.voucherCode"));
    private final IntegerField orderId = new IntegerField(Messages.get("growth.orderId"));
    private final Div result = new Div();

    public CampaignsView(GrowthRestClientService client) {
        super("campaigns-view");
        this.client = client;
        customerId.setValue(1);
        type.setValue("VOUCHER");
        result.addClassName("growth-result");

        render(PageHeader.of("page.campaigns.title", "page.campaigns.description"),
                toolbar(code, name, type,
                        button("growth.createCampaign", VaadinIcon.PLUS, event -> create())),
                toolbar(campaignId, customerId,
                        button("growth.issueVoucher", VaadinIcon.TICKET, event -> issue())),
                toolbar(voucherCode, orderId,
                        button("growth.redeemVoucher", VaadinIcon.EXCHANGE, event -> redeem())),
                result);
    }

    private void create() {
        try {
            CreateCampaignRequest request = new CreateCampaignRequest(
                    code.getValue(), name.getValue(), type.getValue(), null, null, List.of());
            CampaignDto campaign = client.createCampaign(request);
            campaignId.setValue(Math.toIntExact(campaign.id()));
            result.setText(Messages.get("growth.result") + ": " + campaign.code());
        } catch (Exception e) {
            actionError(this::create);
        }
    }

    private void issue() {
        try {
            VoucherDto voucher = client.issueVoucher(campaignId.getValue(), customerId.getValue());
            voucherCode.setValue(voucher.code());
            result.setText(Messages.get("growth.result") + ": " + voucher.code());
        } catch (Exception e) {
            actionError(this::issue);
        }
    }

    private void redeem() {
        try {
            VoucherDto voucher = client.redeemVoucher(voucherCode.getValue(), orderId.getValue());
            result.setText(Messages.get("growth.result") + ": " + voucher.status());
        } catch (Exception e) {
            actionError(this::redeem);
        }
    }
}
