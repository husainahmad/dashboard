package com.harmoni.menu.dashboard.layout.setting.table;

import com.harmoni.menu.dashboard.dto.FloorDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientSettingService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.IntegerField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Guards the floor dialog's two modes.
 *
 * <p>Creating a floor needs the brand → chain → store cascade because the API
 * requires a store id; the "order" field is an edit-only concern and must stay
 * out of the create form. Editing keeps the store fixed and hides the order
 * field, which is managed by the floor reordering action instead.</p>
 */
class FloorFormDialogTest {

    private final RestClientSettingService rest = mock(RestClientSettingService.class);
    private final AsyncRestClientOrganizationService organization =
            mock(AsyncRestClientOrganizationService.class);
    private final AccessService access = mock(AccessService.class);

    @Test
    void createFormOffersTheStoreCascadeAndHidesSortOrder() {
        FloorFormDialog dialog = new FloorFormDialog(new FloorDto(), new StoreDto(),
                rest, organization, access, () -> { });

        assertTrue(hasCombo(dialog, "label.brand"), "a new floor needs a brand to scope its stores");
        assertTrue(hasCombo(dialog, "label.chain"), "a new floor needs a chain to scope its stores");
        assertTrue(hasCombo(dialog, "label.store"), "the API requires storeId, so store selection is mandatory");
        assertFalse(hasIntegerField(dialog, "label.floor.sortOrder"),
                "order is assigned automatically on create and must not be asked for");
    }

    @Test
    void editFormKeepsTheStoreAndHidesOrder() {
        FloorDto floor = new FloorDto();
        floor.setId(11);
        floor.setName("Lantai Satu");

        StoreDto store = new StoreDto();
        store.setId(7);
        store.setName("Store A");

        FloorFormDialog dialog = new FloorFormDialog(floor, store,
                rest, organization, access, () -> { });

        ComboBox<?> storeBox = combo(dialog, "label.store");
        assertNotNull(storeBox, "the owned store is shown read-only on edit");
        assertTrue(storeBox.isReadOnly(), "moving a floor between stores would move its tables");
        assertEquals(7, ((StoreDto) storeBox.getValue()).getId());

        assertFalse(hasIntegerField(dialog, "label.floor.sortOrder"),
                "order is managed by the floor reordering action and must not be edited here");
    }

    private static boolean hasCombo(Component root, String label) {
        return combo(root, label) != null;
    }

    private static ComboBox<?> combo(Component root, String label) {
        return descendants(root).stream()
                .filter(ComboBox.class::isInstance)
                .map(ComboBox.class::cast)
                .filter(box -> label.equals(box.getLabel()))
                .findFirst()
                .orElse(null);
    }

    private static boolean hasIntegerField(Component root, String label) {
        return descendants(root).stream()
                .filter(IntegerField.class::isInstance)
                .map(IntegerField.class::cast)
                .anyMatch(field -> label.equals(field.getLabel()));
    }

    private static List<Component> descendants(Component root) {
        List<Component> all = new ArrayList<>();
        collect(root, all);
        return all;
    }

    private static void collect(Component component, List<Component> sink) {
        sink.add(component);
        component.getChildren().forEach(child -> collect(child, sink));
    }
}
