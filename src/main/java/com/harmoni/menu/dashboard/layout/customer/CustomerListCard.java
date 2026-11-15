package com.harmoni.menu.dashboard.layout.customer;

import com.harmoni.menu.dashboard.dto.CustomerDto;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Left-hand card of the Customer page: the current page of customers with their
 * masked phone number and the pager.
 */
public class CustomerListCard extends VerticalLayout {

    private final Span count = new Span();
    private final Div loading = new Div();
    private final VerticalLayout list = new VerticalLayout();
    private final Div empty = new Div();
    private final Span summary = new Span();
    private final Button previous = new Button(VaadinIcon.ARROW_LEFT.create());
    private final Button next = new Button(VaadinIcon.ARROW_RIGHT.create());
    private final Map<Div, CustomerDto> rowComponents = new HashMap<>();

    private List<CustomerDto> customers = new ArrayList<>();
    private CustomerDto selected;
    private int currentPage = 1;
    private int totalPages;
    private long totalCustomers;

    private Consumer<CustomerDto> selectionListener;
    private Runnable previousListener;
    private Runnable nextListener;

    public CustomerListCard() {
        addClassName("customer-list-card");
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        H3 title = new H3(Messages.get("customer.list.title"));
        title.addClassName("org-card-title");

        count.addClassName("customer-list-count");
        HorizontalLayout heading = new HorizontalLayout(title, count);
        heading.setWidthFull();
        heading.setAlignItems(Alignment.CENTER);
        heading.setFlexGrow(1, title);

        loading.addClassName("customer-list-loading");
        loading.setText(Messages.get("customer.list.loading"));
        loading.setVisible(false);

        list.addClassName("customer-list");
        list.setPadding(false);
        list.setSpacing(false);

        empty.addClassName("customer-list-empty");
        empty.setText(Messages.get("customer.list.empty"));
        empty.setVisible(false);

        summary.addClassName("customer-list-summary");

        previous.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        previous.getElement().setAttribute("title", Messages.get("action.previous"));
        previous.addClickListener(event -> {
            if (previousListener != null) {
                previousListener.run();
            }
        });

        next.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        next.getElement().setAttribute("title", Messages.get("action.next"));
        next.addClickListener(event -> {
            if (nextListener != null) {
                nextListener.run();
            }
        });

        HorizontalLayout footer = new HorizontalLayout(summary, previous, next);
        footer.addClassName("customer-list-footer");
        footer.setWidthFull();
        footer.setAlignItems(Alignment.CENTER);
        footer.setFlexGrow(1, summary);

        add(heading, loading, list, empty, footer);
        setFlexGrow(1, list);
        updatePager();
    }

    /**
     * Replaces the visible customers.
     *
     * @param customers the customers of the current page
     */
    public void setCustomers(List<CustomerDto> customers) {
        this.customers = customers == null ? new ArrayList<>() : new ArrayList<>(customers);
        renderRows();
    }

    /**
     * Updates the pager labels and button state.
     *
     * @param currentPage    the one-based current page
     * @param totalPages     the number of pages, zero when empty
     * @param totalCustomers the total number of customers across all pages
     */
    public void setPageInfo(int currentPage, int totalPages, long totalCustomers) {
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalCustomers = totalCustomers;
        updatePager();
    }

    /**
     * Shows or hides the loading state.
     *
     * @param loadingState true while a page is being fetched
     */
    public void setLoading(boolean loadingState) {
        loading.setVisible(loadingState);
        updatePager();
        renderRows();
    }

    /**
     * Highlights the given customer row.
     *
     * @param customer the customer to select, or {@code null} to clear
     */
    public void select(CustomerDto customer) {
        this.selected = customer;
        rowComponents.forEach((component, candidate) -> component.getElement().getClassList()
                .set("customer-list-row-selected", sameCustomer(candidate, customer)));
    }

    public void setSelectionListener(Consumer<CustomerDto> listener) {
        this.selectionListener = listener;
    }

    public void setPreviousListener(Runnable listener) {
        this.previousListener = listener;
    }

    public void setNextListener(Runnable listener) {
        this.nextListener = listener;
    }

    private void renderRows() {
        list.removeAll();
        rowComponents.clear();
        for (CustomerDto customer : customers) {
            Div row = rowComponent(customer);
            rowComponents.put(row, customer);
            list.add(row);
        }
        boolean isLoading = loading.isVisible();
        list.setVisible(!isLoading && !customers.isEmpty());
        empty.setVisible(!isLoading && customers.isEmpty());
        if (selected != null) {
            select(selected);
        }
    }

    private Div rowComponent(CustomerDto customer) {
        Div item = new Div();
        item.addClassName("customer-list-row");

        Div avatar = new Div();
        avatar.addClassName("customer-list-avatar");
        avatar.setText(CustomerMask.initials(customer.getName()));

        Div text = new Div();
        text.addClassName("customer-list-text");

        Span name = new Span(ObjectUtils.isNotEmpty(customer.getName()) ? customer.getName() : "-");
        name.addClassName("customer-list-name");

        Span phone = new Span(maskedPhone(customer));
        phone.addClassName("customer-list-phone");

        text.add(name, phone);
        item.add(avatar, text);
        item.addClickListener(event -> {
            select(customer);
            if (selectionListener != null) {
                selectionListener.accept(customer);
            }
        });
        return item;
    }

    private String maskedPhone(CustomerDto customer) {
        String masked = CustomerMask.phone(customer.getPhone());
        return masked.isEmpty() ? Messages.get("customer.list.noPhone") : masked;
    }

    private void updatePager() {
        int pages = Math.max(totalPages, 1);
        summary.setText(Messages.get("customer.list.summary", currentPage, pages, totalCustomers));
        previous.setEnabled(currentPage > 1);
        next.setEnabled(currentPage < totalPages);
        count.setText(totalCustomers <= 0 ? "" : String.valueOf(totalCustomers));
        count.setVisible(!loading.isVisible() && totalCustomers > 0);
    }

    private boolean sameCustomer(CustomerDto left, CustomerDto right) {
        if (left == null || right == null) {
            return left == right;
        }
        return Objects.equals(left.getId(), right.getId());
    }
}
