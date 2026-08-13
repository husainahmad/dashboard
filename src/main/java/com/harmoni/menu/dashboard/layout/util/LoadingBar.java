package com.harmoni.menu.dashboard.layout.util;

import com.vaadin.flow.component.progressbar.ProgressBar;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Indeterminate progress bar shown while data is being fetched asynchronously.
 * Hides automatically after a timeout so it can never get stuck on load errors.
 * All UI mutations go through {@code UI.access} so it is safe to call from
 * background (broadcast) threads.
 */
public class LoadingBar extends ProgressBar {

    private static final long AUTO_HIDE_SECONDS = 30;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "loading-bar");
        thread.setDaemon(true);
        return thread;
    });

    private final AtomicInteger generation = new AtomicInteger();

    public LoadingBar() {
        setIndeterminate(true);
        setVisible(false);
        addClassName("loading-bar");
    }

    public void start() {
        int current = generation.incrementAndGet();
        getUI().ifPresent(ui -> {
            ui.access(() -> setVisible(true));
            SCHEDULER.schedule(
                    () -> ui.access(() -> {
                        if (generation.get() == current) {
                            setVisible(false);
                        }
                    }),
                    AUTO_HIDE_SECONDS, TimeUnit.SECONDS);
        });
    }

    public void stop() {
        generation.incrementAndGet();
        getUI().ifPresent(ui -> ui.access(() -> setVisible(false)));
    }
}