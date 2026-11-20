package com.harmoni.menu.dashboard.layout.security;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutePermissionTest {

    @Test
    void everyMainLayoutRouteDeclaresItsPermission() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Route.class));

        List<String> missing = new ArrayList<>();
        for (var definition : scanner.findCandidateComponents("com.harmoni.menu.dashboard.layout")) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            Route route = type.getAnnotation(Route.class);
            if (route == null || !MainLayout.class.equals(route.layout())) {
                continue;
            }
            if (!Component.class.isAssignableFrom(type)) {
                continue;
            }
            if (type.getAnnotation(RequiresPermission.class) == null
                    && !type.equals(AccessDeniedView.class)) {
                missing.add(type.getName());
            }
        }

        assertTrue(missing.isEmpty(), "Routes without @RequiresPermission: " + missing);
    }
}
