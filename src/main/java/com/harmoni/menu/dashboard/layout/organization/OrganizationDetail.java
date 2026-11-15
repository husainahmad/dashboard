package com.harmoni.menu.dashboard.layout.organization;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.OrganizationTreeDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.math.BigDecimal;

/**
 * Right-hand card of the organization page. Shows the selected brand, chain or
 * store and the settings that can be inherited or overridden at that level.
 */
public class OrganizationDetail extends VerticalLayout {

    private static final String INHERIT = "INHERIT";
    private static final String OWN = "OWN";

    private final RestClientOrganizationService restClientOrganizationService;
    private final Runnable onChanged;

    public OrganizationDetail(RestClientOrganizationService restClientOrganizationService,
                              Runnable onChanged) {
        this.restClientOrganizationService = restClientOrganizationService;
        this.onChanged = onChanged;
        addClassName("org-detail-card");
        setPadding(false);
        setSpacing(false);
        setSizeFull();
    }

    /**
     * Renders the detail for the given node.
     *
     * @param node the selected tree node
     */
    public void show(OrganizationNode node) {
        removeAll();
        if (node == null) {
            return;
        }
        add(header(node));
        add(subtitle(node));
        add(sectionTitle());
        add(settings(node));
        add(footerNote());
    }

    private HorizontalLayout header(OrganizationNode node) {
        H2 name = new H2(node.getName());
        name.addClassName("org-detail-name");

        Span badge = new Span(headerBadge(node));
        badge.addClassNames("org-badge", "org-badge-count");

        HorizontalLayout header = new HorizontalLayout(name, badge);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.addClassName("org-detail-header");
        header.setFlexGrow(1, name);
        return header;
    }

    private String headerBadge(OrganizationNode node) {
        if (node.isStore()) {
            OrganizationTreeDto.StoreNode store = node.getStore();
            return Messages.get("organization.count.terminalsTables",
                    store.getTerminalCount(), store.getTableCount());
        }
        if (node.isChain()) {
            return Messages.get("organization.count.stores", node.childCount());
        }
        return Messages.get("organization.count.chainsStores",
                node.childCount(), node.storeCount());
    }

    private Paragraph subtitle(OrganizationNode node) {
        Paragraph subtitle = new Paragraph();
        subtitle.addClassName("org-detail-subtitle");
        if (node.isStore()) {
            OrganizationTreeDto.StoreNode store = node.getStore();
            String chainName = node.getParent() == null ? "" : node.getParent().getName();
            subtitle.setText(Messages.get("organization.detail.storeSubtitle",
                    chainName, store.getAddress() == null ? "" : store.getAddress()));
        } else if (node.isChain()) {
            String brandName = node.getParent() == null ? "" : node.getParent().getName();
            subtitle.setText(Messages.get("organization.detail.chainSubtitle", brandName));
        } else {
            subtitle.setText(Messages.get("organization.detail.brandSubtitle"));
        }
        return subtitle;
    }

    private Span sectionTitle() {
        Span title = new Span(Messages.get("organization.settings.title"));
        title.addClassName("org-section-title");
        return title;
    }

    private VerticalLayout settings(OrganizationNode node) {
        VerticalLayout rows = new VerticalLayout();
        rows.addClassName("org-settings");
        rows.setPadding(false);
        rows.setSpacing(false);

        if (node.isStore()) {
            rows.add(storeRows(node));
        } else if (node.isChain()) {
            rows.add(chainRows(node));
        } else {
            rows.add(brandRows());
        }
        return rows;
    }

    private VerticalLayout storeRows(OrganizationNode node) {
        OrganizationTreeDto.StoreNode store = node.getStore();
        VerticalLayout rows = new VerticalLayout();
        rows.setPadding(false);
        rows.setSpacing(false);

        rows.add(new SettingRow(
                Messages.get("organization.setting.menuPrice"),
                badge(store.getMenuPriceMode()),
                isOwn(store.getMenuPriceMode()),
                source(store.getMenuPriceMode(), "organization.source.store", "organization.source.brand"),
                action(store.getMenuPriceMode(), "organization.action.override", "organization.action.brand"),
                () -> toggleStore(node, StoreDto::setMenuPriceMode, store.getMenuPriceMode())));

        rows.add(new SettingRow(
                Messages.get("organization.setting.taxService"),
                badge(store.getTaxServiceMode()),
                isOwn(store.getTaxServiceMode()),
                source(store.getTaxServiceMode(), "organization.source.store", "organization.source.chain"),
                action(store.getTaxServiceMode(), "organization.action.override", "organization.action.chain"),
                () -> changeStoreTax(node)));

        rows.add(new SettingRow(
                Messages.get("organization.setting.receiptLogo"),
                badge(store.getReceiptLogoMode()),
                isOwn(store.getReceiptLogoMode()),
                source(store.getReceiptLogoMode(), "organization.source.store", "organization.source.brand"),
                action(store.getReceiptLogoMode(), "organization.action.override", "organization.action.brand"),
                () -> toggleStore(node, StoreDto::setReceiptLogoMode, store.getReceiptLogoMode())));

        rows.add(new SettingRow(
                Messages.get("organization.setting.orderType"),
                badge(store.getOrderTypeMode()),
                isOwn(store.getOrderTypeMode()),
                source(store.getOrderTypeMode(), "organization.source.store", "organization.source.chain"),
                action(store.getOrderTypeMode(), "organization.action.override", "organization.action.chain"),
                () -> toggleStore(node, StoreDto::setOrderTypeMode, store.getOrderTypeMode())));
        return rows;
    }

    private VerticalLayout chainRows(OrganizationNode node) {
        OrganizationTreeDto.ChainNode chain = node.getChain();
        VerticalLayout rows = new VerticalLayout();
        rows.setPadding(false);
        rows.setSpacing(false);

        rows.add(new SettingRow(
                Messages.get("organization.setting.menuPrice"),
                Messages.get("organization.badge.inherited"),
                false,
                Messages.get("organization.source.brand"),
                null, null));

        rows.add(new SettingRow(
                Messages.get("organization.setting.taxService"),
                badge(chain.getTaxServiceMode()),
                isOwn(chain.getTaxServiceMode()),
                source(chain.getTaxServiceMode(), "organization.source.atChain", "organization.source.brand"),
                action(chain.getTaxServiceMode(), "organization.action.overrideChain", "organization.action.brand"),
                () -> changeChainTax(node)));

        rows.add(new SettingRow(
                Messages.get("organization.setting.receiptLogo"),
                Messages.get("organization.badge.inherited"),
                false,
                Messages.get("organization.source.brand"),
                null, null));

        rows.add(new SettingRow(
                Messages.get("organization.setting.orderType"),
                Messages.get("organization.badge.managed"),
                false,
                Messages.get("organization.source.chain"),
                null, null));
        return rows;
    }

    private VerticalLayout brandRows() {
        VerticalLayout rows = new VerticalLayout();
        rows.setPadding(false);
        rows.setSpacing(false);

        rows.add(new SettingRow(
                Messages.get("organization.setting.menuPrice"),
                Messages.get("organization.badge.managed"),
                false,
                Messages.get("organization.source.brand"),
                null, null));
        rows.add(new SettingRow(
                Messages.get("organization.setting.taxService"),
                Messages.get("organization.badge.managed"),
                false,
                Messages.get("organization.source.chain"),
                null, null));
        rows.add(new SettingRow(
                Messages.get("organization.setting.receiptLogo"),
                Messages.get("organization.badge.managed"),
                false,
                Messages.get("organization.source.brand"),
                null, null));
        rows.add(new SettingRow(
                Messages.get("organization.setting.orderType"),
                Messages.get("organization.badge.managed"),
                false,
                Messages.get("organization.source.chain"),
                null, null));
        return rows;
    }

    private Div footerNote() {
        Div note = new Div();
        note.addClassName("org-detail-note");
        note.setText(Messages.get("organization.detail.note"));
        return note;
    }

    private void toggleStore(OrganizationNode node, java.util.function.BiConsumer<StoreDto, String> setter,
                             String currentMode) {
        StoreDto dto = toStoreDto(node.getStore());
        setter.accept(dto, isOwn(currentMode) ? INHERIT : OWN);
        persistStore(dto);
    }

    private void changeStoreTax(OrganizationNode node) {
        OrganizationTreeDto.StoreNode store = node.getStore();
        if (isOwn(store.getTaxServiceMode())) {
            StoreDto dto = toStoreDto(store);
            dto.setTaxServiceMode(INHERIT);
            persistStore(dto);
            return;
        }
        new TaxOverrideDialog(store.getTaxPercent(), store.getServiceChargePercent(),
                (tax, service) -> {
                    StoreDto dto = toStoreDto(store);
                    dto.setTaxServiceMode(OWN);
                    dto.setTaxPercent(tax);
                    dto.setServiceChargePercent(service);
                    persistStore(dto);
                }).open();
    }

    private void changeChainTax(OrganizationNode node) {
        OrganizationTreeDto.ChainNode chain = node.getChain();
        if (isOwn(chain.getTaxServiceMode())) {
            ChainDto dto = toChainDto(chain);
            dto.setTaxServiceMode(INHERIT);
            persistChain(dto);
            return;
        }
        new TaxOverrideDialog(chain.getTaxPercent(), chain.getServiceChargePercent(),
                (tax, service) -> {
                    ChainDto dto = toChainDto(chain);
                    dto.setTaxServiceMode(OWN);
                    dto.setTaxPercent(tax);
                    dto.setServiceChargePercent(service);
                    persistChain(dto);
                }).open();
    }

    private void persistStore(StoreDto dto) {
        restClientOrganizationService.updateStore(dto).subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), this::saved),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("organization.settings.failed"))));
    }

    private void persistChain(ChainDto dto) {
        restClientOrganizationService.updateChain(dto).subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), this::saved),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("organization.settings.failed"))));
    }

    private void saved() {
        UiUtil.success(Messages.get("organization.settings.saved"));
        if (onChanged != null) {
            onChanged.run();
        }
    }

    private StoreDto toStoreDto(OrganizationTreeDto.StoreNode store) {
        StoreDto dto = new StoreDto();
        dto.setId(store.getId());
        dto.setName(store.getName());
        dto.setChainId(store.getChainId());
        dto.setAddress(store.getAddress());
        dto.setOrderTypes(store.getOrderTypes());
        dto.setTaxServiceMode(store.getTaxServiceMode());
        dto.setTaxPercent(store.getTaxPercent());
        dto.setServiceChargePercent(store.getServiceChargePercent());
        dto.setReceiptLogoMode(store.getReceiptLogoMode());
        dto.setLogoUrl(store.getLogoUrl());
        dto.setMenuPriceMode(store.getMenuPriceMode());
        dto.setOrderTypeMode(store.getOrderTypeMode());
        dto.setTierMenuId(store.getTierMenuId());
        dto.setTierPriceId(store.getTierPriceId());
        dto.setTierServiceId(store.getTierServiceId());
        return dto;
    }

    private ChainDto toChainDto(OrganizationTreeDto.ChainNode chain) {
        ChainDto dto = new ChainDto();
        dto.setId(chain.getId());
        dto.setName(chain.getName());
        dto.setBrandId(chain.getBrandId());
        dto.setType(chain.getType());
        dto.setManagerEmail(chain.getManagerEmail());
        dto.setTaxServiceMode(chain.getTaxServiceMode());
        dto.setTaxPercent(chain.getTaxPercent());
        dto.setServiceChargePercent(chain.getServiceChargePercent());
        return dto;
    }

    private BrandDto toBrandDto(OrganizationTreeDto.BrandNode brand) {
        BrandDto dto = new BrandDto();
        dto.setId(brand.getId());
        dto.setName(brand.getName());
        dto.setLogoUrl(brand.getLogoUrl());
        dto.setTimeZone(brand.getTimeZone());
        dto.setCurrency(brand.getCurrency());
        dto.setAdminEmail(brand.getAdminEmail());
        dto.setInitialMenuMode(brand.getInitialMenuMode());
        dto.setSourceBrandId(brand.getSourceBrandId());
        return dto;
    }

    private boolean isOwn(String mode) {
        return OWN.equalsIgnoreCase(mode);
    }

    private String badge(String mode) {
        return isOwn(mode)
                ? Messages.get("organization.badge.override")
                : Messages.get("organization.badge.inherited");
    }

    private String source(String mode, String ownKey, String inheritedKey) {
        return isOwn(mode)
                ? Messages.get(ownKey)
                : Messages.get(inheritedKey);
    }

    private String action(String mode, String overrideKey, String inheritedKey) {
        return isOwn(mode)
                ? Messages.get("organization.action.back", Messages.get(inheritedKey))
                : Messages.get(overrideKey);
    }
}
