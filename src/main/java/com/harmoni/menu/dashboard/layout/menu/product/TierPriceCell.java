package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

/**
 * An in-place editor for one SKU/tier price cell.
 *
 * <p>Owns its own committed value and in-flight flag, so the grid does not need
 * the {@code AtomicReference}/{@code boolean[]} workarounds that used to hold
 * this state from the list view. The value is committed to the backend when it
 * changes, the field is disabled while the request is in flight, and it reverts
 * with an error toast when the save fails.</p>
 */
final class TierPriceCell extends NumberField {

    private final transient ProductTreeItem productTreeItem;
    private final Integer tierId;
    private final transient RestClientMenuService restClientMenuService;
    private final transient UI ui;
    private final boolean editable;

    private Double committed;
    private boolean saving;

    /**
     * @param productTreeItem the SKU row this cell belongs to
     * @param tierId          the price tier edited by this cell
     * @param service         client used to persist the price
     * @param ui              the owning UI, used to marshal async callbacks
     * @param typingFields    fields that suppress list shortcuts while focused
     * @param editable        whether this user may change the price
     */
    TierPriceCell(ProductTreeItem productTreeItem, Integer tierId, RestClientMenuService service,
                  UI ui, Set<Component> typingFields, boolean editable) {
        this.productTreeItem = productTreeItem;
        this.tierId = tierId;
        this.restClientMenuService = service;
        this.ui = ui;
        this.editable = editable;

        addClassName("money-field");
        setValueChangeMode(ValueChangeMode.ON_CHANGE);
        setPlaceholder("0");
        setMin(0);
        setWidth("7em");
        setTitle(Messages.get("label.priceHint"));
        UiUtil.guardShortcutField(typingFields, this);

        committed = currentPrice();
        if (committed != null) {
            setValue(committed);
        }
        if (!editable) {
            setReadOnly(true);
            return;
        }
        wireDirtyTracking();
        addValueChangeListener(this::onValueChange);
    }

    private Double currentPrice() {
        return productTreeItem.getTierPrices() == null ? null : productTreeItem.getTierPrices().get(tierId);
    }

    private void wireDirtyTracking() {
        Span commitHint = new Span("\u21b5");
        commitHint.addClassName("price-commit-hint");
        addFocusListener(event -> {
            if (!saving) {
                addClassName(Css.PRICE_DIRTY);
                setSuffixComponent(commitHint);
            }
        });
        addBlurListener(event -> {
            if (!saving) {
                removeClassName(Css.PRICE_DIRTY);
                setSuffixComponent(null);
            }
        });
    }

    private void onValueChange(ValueChangeEvent<Double> event) {
        if (!editable || saving) {
            return;
        }
        Double next = event.getValue();
        if (Objects.equals(committed, next)) {
            return;
        }
        if (next == null || next <= 0) {
            revert();
            return;
        }
        save(next);
    }

    private void revert() {
        saving = true;
        setValue(committed);
        saving = false;
    }

    private void save(Double next) {
        Double previous = committed;
        saving = true;
        setEnabled(false);
        setSuffixComponent(null);
        addClassName(Css.PRICE_SAVING);
        removeClassName(Css.PRICE_DIRTY);
        committed = next;

        restClientMenuService.updateSkuTierPrice(productTreeItem.getSkuId(), tierId, next)
                .doOnError(error -> UiUtil.safeAccess(ui, () -> onSaveError(previous)))
                .subscribe(response -> UiUtil.safeAccess(ui, this::onSaved));
    }

    private void onSaveError(Double previous) {
        saving = false;
        setEnabled(true);
        removeClassName(Css.PRICE_SAVING);
        addClassName(Css.PRICE_ERROR);
        UiUtil.flash(this, Css.PRICE_ERROR, 1500);
        committed = previous;
        setValue(previous);
        UiUtil.error(Messages.get("notification.price.saveFailed"));
    }

    private void onSaved() {
        saving = false;
        setEnabled(true);
        removeClassName(Css.PRICE_SAVING);
        addClassName(Css.PRICE_SAVED);
        UiUtil.flash(this, Css.PRICE_SAVED, 1200);
        if (productTreeItem.getTierPrices() == null) {
            productTreeItem.setTierPrices(new HashMap<>());
        }
        productTreeItem.getTierPrices().put(tierId, committed);
        UiUtil.success(Messages.get("notification.price.updated"));
    }
}
