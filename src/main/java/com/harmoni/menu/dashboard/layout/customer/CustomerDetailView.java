package com.harmoni.menu.dashboard.layout.customer;

import com.harmoni.menu.dashboard.dto.CustomerDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientCustomerService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Read-only detail tab for a single customer, opened from the
 * {@link CustomerListView} through {@link com.harmoni.menu.dashboard.layout.component.TabManager}.
 *
 * <p>Shows the fields the customer service returns and a back button returning
 * to the customer list tab. A missing or soft-deleted customer shows a
 * not-found message instead of an empty card.</p>
 */
@RequiredArgsConstructor
@Slf4j
public class CustomerDetailView extends VerticalLayout {

    private static final String DATE_TIME_PATTERN = "dd MMM yyyy HH:mm";

    private final AsyncRestClientCustomerService asyncRestClientCustomerService;

    /** The id of the customer shown by this tab. */
    private final Long customerId;

    /** The card the customer fields are rendered into. */
    private final VerticalLayout card = new VerticalLayout();

    /** The UI this view is attached to; set on attach. */
    private UI ui;

    /** Guards the one-time layout construction. */
    private boolean rendered;

    /**
     * Loads the customer identified by the constructor id and renders the
     * detail card.
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        ui = attachEvent.getUI();
        if (!rendered) {
            renderLayout();
            rendered = true;
        }
        loadCustomer();
    }

    /**
     * Builds the header with the back button and the detail card holding the
     * customer fields.
     */
    private void renderLayout() {
        Button backButton = new Button(Messages.get("action.back"), event -> selectListTab());
        backButton.setPrefixComponent(VaadinIcon.ARROW_LEFT.create());
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        card.setPadding(true);
        card.setSpacing(true);
        card.setSizeFull();

        add(backButton, card);
        setFlexGrow(1, card);
    }

    /**
     * Returns to the first (customer list) tab of the enclosing tab sheet.
     */
    private void selectListTab() {
        getParent()
                .filter(TabSheet.class::isInstance)
                .map(TabSheet.class::cast)
                .filter(tabSheet -> tabSheet.getTabCount() > 0)
                .ifPresent(tabSheet -> tabSheet.setSelectedIndex(0));
    }

    /**
     * Fetches the customer and populates the detail card, or shows a not-found
     * message when the record is missing or soft-deleted.
     */
    private void loadCustomer() {
        if (customerId == null) {
            showNotFound();
            return;
        }
        asyncRestClientCustomerService.getCustomerAsync(
                customer -> UiUtil.safeAccess(ui, () -> showCustomer(customer)),
                error -> UiUtil.safeAccess(ui, this::showNotFound),
                customerId);
    }

    /**
     * Renders the customer fields inside the detail card.
     *
     * @param customer the loaded customer
     */
    private void showCustomer(CustomerDto customer) {
        card.removeAll();
        card.add(
                field(Messages.get(Messages.Keys.LABEL_CUSTOMER_NAME), customer.getName()),
                field(Messages.get(Messages.Keys.LABEL_CUSTOMER_PHONE), customer.getPhone()),
                field(Messages.get(Messages.Keys.LABEL_CUSTOMER_EMAIL), customer.getEmail()),
                field(Messages.get("label.customer.createdAt"), formatDate(customer.getCreatedAt())),
                field(Messages.get("label.customer.updatedAt"), formatDate(customer.getUpdatedAt())));
    }

    /**
     * Renders the message shown when the customer cannot be loaded.
     */
    private void showNotFound() {
        card.removeAll();
        card.add(new Span(Messages.get(Messages.Keys.NOTIFICATION_CUSTOMER_NOT_FOUND)));
    }

    /**
     * Builds one label / value row of the detail card.
     *
     * @param label the field label
     * @param value the field value, may be {@code null}
     * @return the row component
     */
    private static Component field(String label, String value) {
        Span name = new Span(label);
        name.addClassName("customer-detail__label");
        Span content = new Span(ObjectUtils.isNotEmpty(value) ? value : "-");
        content.addClassName("customer-detail__value");
        HorizontalLayout row = new HorizontalLayout(name, content);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setWidthFull();
        return row;
    }

    /**
     * Formats a timestamp for display, or {@code -} when it is not set.
     *
     * @param date the timestamp to format
     * @return the formatted timestamp
     */
    private static String formatDate(Date date) {
        return date == null ? "-" : new SimpleDateFormat(DATE_TIME_PATTERN).format(date);
    }
}
