package com.harmoni.menu.dashboard.support;

import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Small component-tree helpers shared by the UI permission tests.
 */
public final class UiTestSupport {

    private UiTestSupport() {
    }

    public static List<Component> descendants(Component root) {
        List<Component> all = new ArrayList<>();
        collect(root, all);
        return all;
    }

    public static Button buttonByText(Component root, String text) {
        return descendants(root).stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> text.equals(button.getText()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Creates an authorization service that grants exactly the given permissions.
     *
     * @param permissions the permission names to grant
     * @return a mocked service
     */
    public static AuthorizationService authorization(String... permissions) {
        Set<String> granted = Set.of(permissions);
        return new AuthorizationService() {
            @Override
            public boolean hasPermission(String permission) {
                return permission != null && granted.contains(permission);
            }

            @Override
            public boolean hasAnyPermission(String... permissions) {
                return permissions != null
                        && Arrays.stream(permissions).anyMatch(granted::contains);
            }
        };
    }

    private static void collect(Component component, List<Component> sink) {
        sink.add(component);
        component.getChildren().forEach(child -> collect(child, sink));
    }
}
