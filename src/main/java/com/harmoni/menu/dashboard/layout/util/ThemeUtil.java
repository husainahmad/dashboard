package com.harmoni.menu.dashboard.layout.util;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.theme.lumo.Lumo;

import java.util.function.Consumer;

/**
 * Night-first theme handling with a persisted user choice.
 *
 * The dark theme is applied synchronously on attach so the
 * default (night) renders without a flash; a saved "light"
 * preference in localStorage then corrects the mode. The
 * toggle persists every change.
 */
public final class ThemeUtil {

    private static final String STORAGE_KEY = "posharmoni-theme";
    private static final String LIGHT = "light";

    private ThemeUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static void applySavedTheme(UI ui) {
        applySavedTheme(ui, null);
    }

    public static void applySavedTheme(UI ui, Consumer<Boolean> onDarkApplied) {
        ui.getElement().getThemeList().add(Lumo.DARK);
        ui.getElement()
                .executeJs("return window.localStorage.getItem($0)", STORAGE_KEY)
                .then(String.class, saved -> ui.access(() -> {
                    boolean dark = !LIGHT.equals(saved);
                    if (dark) {
                        ui.getElement().getThemeList().add(Lumo.DARK);
                    } else {
                        ui.getElement().getThemeList().remove(Lumo.DARK);
                    }
                    if (onDarkApplied != null) {
                        onDarkApplied.accept(dark);
                    }
                    ui.getElement().executeJs(
                            "document.documentElement.style.removeProperty('background')");
                }));
    }

    public static void storeTheme(UI ui, boolean dark) {
        ui.getElement().executeJs(
                "window.localStorage.setItem($0, $1)",
                STORAGE_KEY,
                dark ? "dark" : LIGHT);
    }
}
