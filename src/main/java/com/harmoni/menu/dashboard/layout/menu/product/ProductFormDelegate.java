package com.harmoni.menu.dashboard.layout.menu.product;

import com.vaadin.flow.component.UI;

/**
 * Narrow contract the product form exposes to its sub-sections so they can give
 * feedback and reach the current product without depending on the whole view.
 */
public interface ProductFormDelegate {

    /**
     * Returns the {@link UI} the form is attached to, so asynchronous callbacks
     * can be marshalled back onto the Vaadin thread.
     *
     * @return the owning UI, which may be {@code null} before attach or after detach
     */
    UI getUi();

    /**
     * Shows a success-style notification to the user.
     *
     * @param message the text to display
     */
    void showNotification(String message);

    /**
     * Shows a modal error dialog to the user.
     *
     * @param message the error text to display
     */
    void showErrorDialog(String message);

    /**
     * Returns the id of the product currently being edited, if it has been
     * persisted yet.
     *
     * @return the persisted product id, or {@code null} for a brand-new product
     */
    Integer getProductId();
}