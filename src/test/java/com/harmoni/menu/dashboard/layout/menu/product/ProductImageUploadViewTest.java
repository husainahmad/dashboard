package com.harmoni.menu.dashboard.layout.menu.product;

import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the division of labour between the tile and the product preview.
 *
 * <p>The tile is the way an image is picked, cropped and removed; the preview panel is
 * the only place the result is looked at. The tile therefore keeps its controls and
 * drops its 64px thumbnail, so the operator is not shown the same picture twice.</p>
 *
 * <p>The view renders on attach and there is no session here, so {@code renderLayout}
 * is invoked directly - the same one the attach hook would call - to get a tree worth
 * asserting on.</p>
 */
class ProductImageUploadViewTest {

    @Test
    void theTileStillOffersTheUploadControls() {
        ProductImageUploadView view = rendered();

        assertTrue(hasClass(view, "upload-tile"),
                "the tile is the only way to set a product image, so it has to survive "
                        + "the thumbnail being hidden");
    }

    @Test
    void itKeepsTheControlToPickAnImage() {
        ProductImageUploadView view = rendered();

        assertTrue(containsButton(view),
                "hiding the thumbnail must not take the button with it: picking, cropping "
                        + "and removing all hang off this tile, and without it a product "
                        + "image could never be set at all");
    }

    @Test
    void theHiddenThumbnailDoesNotTakeTheTileWithIt() {
        ProductImageUploadView view = rendered();

        assertTrue(hasClass(view, "upload-tile"),
                "the tile is the row holding the labels and the actions, so hiding one "
                        + "child of it must leave the row standing");
    }

    @Test
    void theThumbnailIsHiddenSoTheImageIsOnlyShownInThePreview() {
        ProductImageUploadView view = rendered();

        assertFalse(thumbnail(view).isVisible(),
                "the product preview panel is where the image belongs now; showing it in "
                        + "both places is one more copy to keep in step");
    }

    // ---------------------------------------------------------------- helpers

    /** Builds the tile the way the attach hook would, since there is no session here. */
    private static ProductImageUploadView rendered() {
        ProductImageUploadView view = new ProductImageUploadView(null, null, null);
        try {
            java.lang.reflect.Method renderLayout =
                    ProductImageUploadView.class.getDeclaredMethod("renderLayout");
            renderLayout.setAccessible(true);
            renderLayout.invoke(view);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new AssertionError("renderLayout failed", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return view;
    }

    private static Component thumbnail(Component root) {
        return findByClass(root, "product-image-thumbnail");
    }

    private static boolean containsButton(Component root) {
        for (Component child : root.getChildren().toList()) {
            if (child instanceof com.vaadin.flow.component.button.Button) {
                return true;
            }
            if (containsButton(child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasClass(Component root, String className) {
        try {
            findByClass(root, className);
            return true;
        } catch (AssertionError absent) {
            return false;
        }
    }

    /** Depth-first search for a component carrying every one of {@code classNames}. */
    private static Component findByClass(Component root, String... classNames) {
        for (Component child : root.getChildren().toList()) {
            boolean matches = java.util.Arrays.stream(classNames)
                    .allMatch(className -> child.getClassNames().contains(className));
            if (matches) {
                return child;
            }
            try {
                return findByClass(child, classNames);
            } catch (AssertionError absent) {
                // keep looking
            }
        }
        throw new AssertionError("no component with classes " + String.join(" ", classNames));
    }
}
