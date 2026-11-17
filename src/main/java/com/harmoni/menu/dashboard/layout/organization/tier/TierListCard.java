package com.harmoni.menu.dashboard.layout.organization.tier;

import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Left-hand card of the Tier page: the tiers of the selected brand and type
 * with their store count.
 */
public class TierListCard extends VerticalLayout {

    private final TextField search = new TextField();
    private final VerticalLayout list = new VerticalLayout();
    private final Div empty = new Div();
    private final Div summary = new Div();
    private final Map<Div, TierRow> rowComponents = new HashMap<>();

    private List<TierRow> allRows = new ArrayList<>();
    private TierRow selectedRow;
    private Consumer<TierRow> selectionListener;

    public TierListCard() {
        addClassName("tier-list-card");
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        H3 title = new H3(Messages.get("tier.list.title"));
        title.addClassName("org-card-title");

        search.setLabel(Messages.get(Messages.Keys.LABEL_SEARCH));
        search.setPlaceholder(Messages.get("tier.list.search"));
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.addValueChangeListener(event -> applyFilter());
        search.addClassName("tier-list-search");
        search.setWidthFull();

        list.addClassName("tier-list");
        list.setPadding(false);
        list.setSpacing(false);

        empty.addClassName("tier-list-empty");
        empty.setText(Messages.get("tier.list.empty"));
        empty.setVisible(false);

        summary.addClassName("tier-list-summary");

        add(title, search, list, empty, summary);
        setFlexGrow(1, list);
    }

    /**
     * Replaces the list contents.
     *
     * @param rows the tiers to show
     */
    public void setRows(List<TierRow> rows) {
        this.allRows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        applyFilter();
    }

    /**
     * Registers the selection callback.
     *
     * @param listener the callback invoked with the selected row
     */
    public void setSelectionListener(Consumer<TierRow> listener) {
        this.selectionListener = listener;
    }

    /**
     * Selects the given row and highlights it.
     *
     * @param row the row to select
     */
    public void select(TierRow row) {
        this.selectedRow = row;
        rowComponents.forEach((component, candidate) ->
                component.getElement().getClassList()
                        .set("tier-list-row-selected", candidate.equals(row)));
    }

    private void applyFilter() {
        String term = search.getValue() == null ? "" : search.getValue().trim().toLowerCase();
        List<TierRow> filtered = allRows.stream()
                .filter(row -> term.isEmpty()
                        || row.getTier().getName().toLowerCase().contains(term))
                .toList();

        list.removeAll();
        rowComponents.clear();
        for (TierRow row : filtered) {
            Div component = rowComponent(row);
            rowComponents.put(component, row);
            list.add(component);
        }

        boolean isEmpty = filtered.isEmpty();
        empty.setVisible(isEmpty);
        list.setVisible(!isEmpty);
        if (selectedRow != null) {
            select(selectedRow);
        }
        updateSummary();
    }

    private void updateSummary() {
        int stores = allRows.stream().mapToInt(TierRow::getStoreCount).sum();
        summary.setText(Messages.get("tier.list.summary", allRows.size(), stores));
    }

    private Div rowComponent(TierRow row) {
        Div item = new Div();
        item.addClassName("tier-list-row");

        TierTypeDto type = row.getTier().getType();
        Div avatar = new Div(new Icon(iconFor(type)));
        avatar.addClassName("tier-list-avatar");
        avatar.addClassName("tier-list-avatar-" + typeClass(type));

        Div text = new Div();
        text.addClassName("tier-list-text");

        Span name = new Span(row.getTier().getName());
        name.addClassName("tier-list-name");

        Span count = new Span(Messages.get("tier.storeCount", row.getStoreCount()));
        count.addClassName("tier-list-count-pill");

        text.add(name, count);
        item.add(avatar, text);
        item.addClickListener(event -> {
            select(row);
            if (selectionListener != null) {
                selectionListener.accept(row);
            }
        });
        return item;
    }

    private VaadinIcon iconFor(TierTypeDto type) {
        if (type == null) {
            return VaadinIcon.TAG;
        }
        return switch (type) {
            case MENU -> VaadinIcon.COFFEE;
            case SERVICE -> VaadinIcon.COG;
            default -> VaadinIcon.TAG;
        };
    }

    private String typeClass(TierTypeDto type) {
        return type == null ? "price" : type.name().toLowerCase();
    }
}
