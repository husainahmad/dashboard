package com.harmoni.menu.dashboard.layout.menu.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CustomizationDto;
import com.harmoni.menu.dashboard.layout.util.AsyncUtil;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * "Add Customization" dialog: searches the customization master for the brand
 * and lets the user attach the selected items to the product. Items that are
 * already attached are shown but disabled. Confirmation delegates to
 * {@link CustomizationSection#onAddSelected}.
 */
public class CustomizationAddDialog extends Dialog {

    /**
     * @param section                      the owner section that persists the selection
     * @param delegate                     the owner form, used for UI feedback
     * @param asyncRestClientMenuService   async client feeding the search list
     * @param brandDto                     brand filter for the customization master
     */
    public CustomizationAddDialog(CustomizationSection section, ProductFormDelegate delegate,
                                  AsyncRestClientMenuService asyncRestClientMenuService,
                                  BrandDto brandDto) {
        setHeaderTitle(Messages.get("label.addCustomization"));

        TextField searchField = new TextField(Messages.get(Messages.Keys.LABEL_SEARCH));
        searchField.setPlaceholder(Messages.get("placeholder.searchCustomizations"));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);

        CheckboxGroup<CustomizationDto> group = new CheckboxGroup<>();
        group.setLabel(Messages.get("label.availableCustomizations"));
        group.setItemLabelGenerator(CustomizationDto::getName);
        group.setItemHelperGenerator(this::buildAddMeta);

        Set<Integer> attachedIds = section.getProductCustomizations().stream()
                .map(customization -> customization.getCustomizationId())
                .collect(Collectors.toSet());

        AtomicReference<List<CustomizationDto>> allCustomizations = new AtomicReference<>(new ArrayList<>());

        asyncRestClientMenuService.getAllCustomizationAsync(result -> {
                    if (ObjectUtils.isEmpty(result.get("data"))) {
                        return;
                    }
                    List<CustomizationDto> list = ObjectUtil.convertObjectToObject(result.get("data"),
                            new TypeReference<>() {
                            });
                    allCustomizations.set(list);
                    AsyncUtil.onUi(delegate.getUi(), () -> {
                        group.setItems(list);
                        group.setItemEnabledProvider(customization -> !attachedIds.contains(customization.getId()));
                    });
                },
                throwable -> AsyncUtil.onUi(delegate.getUi(),
                        () -> delegate.showErrorDialog(Messages.get(Messages.Keys.NOTIFICATION_CUSTOMIZATION_LOAD_FAILED))),
                brandDto.getId(), 1, 500, "");

        searchField.addValueChangeListener(event -> {
            String filter = event.getValue() == null ? "" : event.getValue().trim().toLowerCase();
            List<CustomizationDto> filtered = allCustomizations.get().stream()
                    .filter(customization -> customization.getName() == null
                            || customization.getName().toLowerCase().contains(filter))
                    .toList();
            group.setItems(filtered);
        });

        Button cancelButton = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> close());
        Button addSelectedButton = UiUtil.addButton(Messages.get("action.addSelected"),
                event -> section.onAddSelected(group.getSelectedItems(), this));

        VerticalLayout dialogContent = new VerticalLayout(searchField, group);
        add(dialogContent);
        getFooter().add(cancelButton, addSelectedButton);
    }

    /**
     * Builds the metadata string for a customization, showing its selection type,
     * required/optional status, and min/max selection range.
     *
     * @param customization the customization to build metadata for
     * @return a formatted string with the customization's metadata
     */
    private String buildAddMeta(CustomizationDto customization) {
        String type = customization.getSelectionType() == null ? ""
                : customization.getSelectionType().getLabel();
        String required = Boolean.TRUE.equals(customization.getRequired())
                ? Messages.get(Messages.Keys.LABEL_REQUIRED) : Messages.get("label.optional");
        String min = customization.getMinimumSelection() == null ? "0"
                : String.valueOf(customization.getMinimumSelection());
        String max = customization.getMaximumSelection() == null ? "n"
                : String.valueOf(customization.getMaximumSelection());
        return Messages.get("label.customizationMeta", type, required, min, max);
    }
}