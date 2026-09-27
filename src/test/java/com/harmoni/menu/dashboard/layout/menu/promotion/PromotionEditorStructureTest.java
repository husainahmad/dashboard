package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Guards the structural property of the promotion editor that no compiler check
 * covers: that it is not exposed as a route, because its constructor cannot be
 * satisfied by Spring.
 */
class PromotionEditorStructureTest {

    /**
     * The editor needs the {@code TabManager} and {@code Tab} it was opened on, and
     * neither is a Spring bean. A {@code @Route} would make Vaadin try to autowire it
     * and fail at navigation time, so it must stay a directly constructed component.
     */
    @Test
    void promotionEditor_isNotExposedAsARoute() {
        assertFalse(PromotionFormWithPreview.class.isAnnotationPresent(Route.class),
                "PromotionFormWithPreview must not be a @Route: its constructor needs a TabManager and Tab, "
                        + "which are not Spring beans, so route navigation would fail to instantiate it");
    }

    /**
     * A second constructor would make the component ambiguous for Spring's
     * constructor resolution. Only the single explicit one is allowed.
     */
    @Test
    void promotionEditor_hasExactlyOneConstructor() {
        Constructor<?>[] constructors = PromotionFormWithPreview.class.getDeclaredConstructors();

        assertEquals(1, constructors.length,
                "a second constructor would reintroduce the ambiguity that made the @Route unusable");
    }
}
