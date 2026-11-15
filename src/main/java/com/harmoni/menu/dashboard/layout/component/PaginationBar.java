package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Previous / page-info / next bar shared by paged grids and dialogs.
 *
 * <p>The bar only renders state; the owner supplies the previous and next
 * actions, which usually mutate the owner's page counter and refetch.</p>
 */
public class PaginationBar extends HorizontalLayout {

    private final Text pageInfo = new Text("");
    private final Button previousButton;
    private final Button nextButton;

    /**
     * @param onPrevious action for the previous-page button
     * @param onNext     action for the next-page button
     */
    public PaginationBar(Runnable onPrevious, Runnable onNext) {
        previousButton = new Button(Messages.get("action.previous"), event -> onPrevious.run());
        nextButton = new Button(Messages.get("action.next"), event -> onNext.run());

        add(previousButton, pageInfo, nextButton);
        addClassName("pagination");
        setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        setWidthFull();
        setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        update(1, 0);
    }

    /**
     * Refreshes the page caption and button states.
     *
     * @param currentPage the page being shown, 1-based
     * @param totalPages  the number of pages available
     */
    public void update(int currentPage, int totalPages) {
        pageInfo.setText(Messages.get("pagination.page", currentPage, totalPages));
        previousButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);
    }

    /**
     * Refreshes the bar for cursor-based paging, where the total page count is
     * unknown and only the current page and the availability of neighbours can
     * be shown.
     *
     * @param currentPage the page being shown, 1-based
     * @param hasPrevious whether a previous page can be reached
     * @param hasNext     whether a next page can be reached
     */
    public void updateCursor(int currentPage, boolean hasPrevious, boolean hasNext) {
        pageInfo.setText(Messages.get("pagination.cursorPage", currentPage));
        previousButton.setEnabled(hasPrevious);
        nextButton.setEnabled(hasNext);
    }

    /**
     * Enables or disables both navigation buttons, e.g. while a fetch is running.
     *
     * @param enabled whether navigation should be available
     */
    public void setNavigationEnabled(boolean enabled) {
        previousButton.setEnabled(enabled);
        nextButton.setEnabled(enabled);
    }
}
