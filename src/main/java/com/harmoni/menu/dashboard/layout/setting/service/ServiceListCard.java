package com.harmoni.menu.dashboard.layout.setting.service;

import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Left-hand card of the Service page: search plus the service rows with their
 * sub-service count.
 */
public class ServiceListCard extends VerticalLayout {

    private final Span count = new Span();
    private final TextField search = new TextField();
    private final Div loading = new Div();
    private final VerticalLayout list = new VerticalLayout();
    private final Div empty = new Div();
    private final Span summary = new Span();
    private final Map<Div, ServiceDto> rowComponents = new HashMap<>();

    private List<ServiceDto> services = new ArrayList<>();
    private ServiceDto selected;
    private Consumer<ServiceDto> selectionListener;

    public ServiceListCard() {
        addClassName("service-list-card");
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        H3 title = new H3(Messages.get("service.list.title"));
        title.addClassName("org-card-title");

        count.addClassName("service-list-count");
        HorizontalLayout heading = new HorizontalLayout(title, count);
        heading.setWidthFull();
        heading.setAlignItems(Alignment.CENTER);
        heading.setFlexGrow(1, title);

        search.setPlaceholder(Messages.get("service.search.placeholder"));
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setWidthFull();
        search.addValueChangeListener(event -> applyFilter());
        search.addClassName("service-list-search");

        loading.addClassName("service-list-loading");
        loading.setText(Messages.get("service.list.loading"));
        loading.setVisible(false);

        list.addClassName("service-list");
        list.setPadding(false);
        list.setSpacing(false);

        empty.addClassName("service-list-empty");
        empty.setText(Messages.get("service.list.empty"));
        empty.setVisible(false);

        summary.addClassName("service-list-summary");

        add(heading, search, loading, list, empty, summary);
        setFlexGrow(1, list);
        updateSummary();
    }

    /**
     * Replaces the services shown by the list.
     *
     * @param services all services
     */
    public void setServices(List<ServiceDto> services) {
        this.services = services == null ? new ArrayList<>() : new ArrayList<>(services);
        applyFilter();
    }

    /**
     * Shows or hides the loading state.
     *
     * @param loadingState true while services are being fetched
     */
    public void setLoading(boolean loadingState) {
        loading.setVisible(loadingState);
        applyFilter();
    }

    /**
     * Highlights the given service row.
     *
     * @param service the service to select, or {@code null} to clear
     */
    public void select(ServiceDto service) {
        this.selected = service;
        rowComponents.forEach((component, candidate) -> component.getElement().getClassList()
                .set("service-list-row-selected", sameService(candidate, service)));
    }

    public void setSelectionListener(Consumer<ServiceDto> listener) {
        this.selectionListener = listener;
    }

    private void applyFilter() {
        String term = search.getValue() == null ? "" : search.getValue().trim().toLowerCase();
        List<ServiceDto> filtered = services.stream()
                .filter(service -> term.isEmpty()
                        || (service.getName() != null && service.getName().toLowerCase().contains(term)))
                .toList();

        list.removeAll();
        rowComponents.clear();
        for (ServiceDto service : filtered) {
            Div row = rowComponent(service);
            rowComponents.put(row, service);
            list.add(row);
        }

        boolean isLoading = loading.isVisible();
        list.setVisible(!isLoading && !filtered.isEmpty());
        empty.setVisible(!isLoading && filtered.isEmpty());
        if (selected != null) {
            select(selected);
        }
        updateSummary();
    }

    private Div rowComponent(ServiceDto service) {
        Div item = new Div();
        item.addClassName("service-list-row");

        Div avatar = new Div(new Icon(VaadinIcon.COG));
        avatar.addClassName("service-list-avatar");

        Div text = new Div();
        text.addClassName("service-list-text");

        Span name = new Span(ObjectUtils.isNotEmpty(service.getName()) ? service.getName() : "-");
        name.addClassName("service-list-name");

        Span subServices = new Span(Messages.get("service.list.subServiceCount", subServiceCount(service)));
        subServices.addClassName("service-list-count-pill");

        text.add(name, subServices);
        item.add(avatar, text);
        item.addClickListener(event -> {
            select(service);
            if (selectionListener != null) {
                selectionListener.accept(service);
            }
        });
        return item;
    }

    private int subServiceCount(ServiceDto service) {
        return service.getSubServices() == null ? 0 : service.getSubServices().size();
    }

    private void updateSummary() {
        count.setText(services.isEmpty() ? "" : String.valueOf(services.size()));
        count.setVisible(!loading.isVisible() && !services.isEmpty());
        summary.setText(Messages.get("service.list.summary", services.size()));
    }

    private boolean sameService(ServiceDto left, ServiceDto right) {
        if (left == null || right == null) {
            return left == right;
        }
        return Objects.equals(left.getId(), right.getId());
    }
}
