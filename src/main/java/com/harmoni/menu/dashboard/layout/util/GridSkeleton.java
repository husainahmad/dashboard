package com.harmoni.menu.dashboard.layout.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Placeholder shown over a grid while its data is being fetched. It mirrors a
 * typical list of rows with shimmering bars so the user can see exactly which
 * region is reloading, without the layout jumping. Hides automatically after a
 * timeout so it can never get stuck when a request fails silently.
 * All UI mutations go through {@code UI.access} so it is safe to call from
 * background (broadcast) threads.
 */
public class GridSkeleton extends VerticalLayout {

    private static final long AUTO_HIDE_SECONDS = 30;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "grid-skeleton");
        thread.setDaemon(true);
        return thread;
    });

    private static final String[] BAR_WIDTHS = {"22%", "16%", "12%", "10%", "10%", "14%", "8%"};

    private final AtomicInteger generation = new AtomicInteger();

    /**
     * Creates the overlay, hidden by default.
     *
     * @param rows the number of skeleton rows to render
     */
    public GridSkeleton(int rows) {
        addClassName("grid-skeleton-overlay");
        setPadding(true);
        setSpacing(false);
        setVisible(false);
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            HorizontalLayout row = new HorizontalLayout();
            row.addClassName("skeleton-row");
            row.setWidthFull();
            row.setPadding(false);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            for (String width : BAR_WIDTHS) {
                row.add(skeletonBar(width));
            }
            add(row);
        }
    }

    /** Shows the skeleton so it covers the grid while data reloads. */
    public void show() {
        int current = generation.incrementAndGet();
        getUI().ifPresent(ui -> {
            ui.access(() -> setVisible(true));
            ui.access(() -> SCHEDULER.schedule(
                    () -> ui.access(() -> {
                        if (generation.get() == current) {
                            setVisible(false);
                        }
                    }),
                    AUTO_HIDE_SECONDS, TimeUnit.SECONDS));
        });
    }

    /** Hides the skeleton once the data has arrived. */
    public void hide() {
        generation.incrementAndGet();
        getUI().ifPresent(ui -> ui.access(() -> setVisible(false)));
    }

    private static Component skeletonBar(String width) {
        Div bar = new Div();
        bar.addClassName("skeleton-bar");
        bar.getStyle().set("width", width);
        return bar;
    }
}