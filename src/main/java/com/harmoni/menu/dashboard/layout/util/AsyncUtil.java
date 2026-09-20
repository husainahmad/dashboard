package com.harmoni.menu.dashboard.layout.util;

import com.harmoni.menu.dashboard.layout.component.DialogClosing;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.vaadin.flow.component.UI;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * Shared helpers for the REST-callback style used across the dashboard:
 * {@code .subscribe(...)} calls arrive on a non-UI thread, so every success and
 * error handler has to be marshalled back with {@code UI.access}. These helpers
 * do that once and log/show the error dialog in a single place.
 */
@Slf4j
public final class AsyncUtil {

    private AsyncUtil() {
    }

    /**
     * Runs {@code action} on the UI thread via {@code ui.access}, safe to call
     * from any thread.
     *
     * @param ui     the UI to schedule on, or {@code null} to skip
     * @param action the work to run on the UI thread
     */
    public static void onUi(UI ui, Runnable action) {
        if (ui != null) {
            ui.access(action::run);
        }
    }

    /**
     * Subscribes to a REST call with a default error handler that logs the
     * failure and opens a {@link DialogClosing} error dialog. Success is
     * handled on the UI thread.
     *
     * @param mono        the reactive result to subscribe to
     * @param ui          the UI to marshal callbacks onto
     * @param errorMessage message shown in the error dialog
     * @param onSuccess   success callback, invoked with the response on the UI thread
     */
    public static void subscribe(Mono<RestAPIResponse> mono, UI ui, String errorMessage,
                                 Consumer<RestAPIResponse> onSuccess) {
        subscribe(mono, ui, onSuccess, throwable -> showError(ui, errorMessage, throwable));
    }

    /**
     * Subscribes to a REST call with explicit success and error handlers, both
     * run on the UI thread.
     *
     * @param mono      the reactive result to subscribe to
     * @param ui        the UI to marshal callbacks onto
     * @param onSuccess success callback, invoked with the response on the UI thread
     * @param onError   error callback, invoked with the cause on the UI thread
     */
    public static void subscribe(Mono<RestAPIResponse> mono, UI ui,
                                 Consumer<RestAPIResponse> onSuccess, Consumer<Throwable> onError) {
        mono.subscribe(response -> onUi(ui, () -> onSuccess.accept(response)),
                throwable -> onUi(ui, () -> onError.accept(throwable)));
    }

    private static void showError(UI ui, String message, Throwable throwable) {
        log.error(message, throwable);
        if (ui != null) {
            DialogClosing dialog = new DialogClosing(message);
            ui.add(dialog);
            dialog.open();
        }
    }
}