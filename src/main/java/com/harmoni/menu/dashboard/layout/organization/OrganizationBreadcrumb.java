package com.harmoni.menu.dashboard.layout.organization;

import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Brand → Chain → Store selector shown above the organization page. The three
 * combo boxes cascade and stay in sync with the tree selection.
 */
public class OrganizationBreadcrumb extends HorizontalLayout {

    private final ComboBox<OrganizationNode> brandBox = new ComboBox<>();
    private final ComboBox<OrganizationNode> chainBox = new ComboBox<>();
    private final ComboBox<OrganizationNode> storeBox = new ComboBox<>();

    private Consumer<OrganizationNode> selectionListener;
    private boolean updating;

    public OrganizationBreadcrumb() {
        addClassName("org-breadcrumb");
        setAlignItems(FlexComponent.Alignment.CENTER);
        setSpacing(true);

        configure(brandBox, Messages.get("organization.breadcrumb.brand"));
        configure(chainBox, Messages.get("organization.breadcrumb.chain"));
        configure(storeBox, Messages.get("organization.breadcrumb.store"));

        brandBox.addValueChangeListener(event -> {
            if (updating) {
                return;
            }
            OrganizationNode brand = event.getValue();
            chainBox.setItems(brand == null ? Collections.emptyList() : brand.getChildren());
            chainBox.clear();
            storeBox.setItems(Collections.emptyList());
            storeBox.clear();
            if (brand != null) {
                notifySelection(brand);
            }
        });

        chainBox.addValueChangeListener(event -> {
            if (updating) {
                return;
            }
            OrganizationNode chain = event.getValue();
            storeBox.setItems(chain == null ? Collections.emptyList() : chain.getChildren());
            storeBox.clear();
            if (chain != null) {
                notifySelection(chain);
            }
        });

        storeBox.addValueChangeListener(event -> {
            if (updating) {
                return;
            }
            if (event.getValue() != null) {
                notifySelection(event.getValue());
            }
        });

        add(brandBox, separator(), chainBox, separator(), storeBox);
    }

    private void configure(ComboBox<OrganizationNode> box, String label) {
        box.setLabel(label);
        box.setItemLabelGenerator(OrganizationNode::getName);
        box.setAllowCustomValue(false);
        box.setWidth("220px");
    }

    private Span separator() {
        Span separator = new Span("›");
        separator.addClassName("org-breadcrumb-separator");
        return separator;
    }

    /**
     * Populates the brand selector.
     *
     * @param brands the brand nodes
     */
    public void setBrands(List<OrganizationNode> brands) {
        brandBox.setItems(brands);
    }

    /**
     * Selects the given node in the matching combo box without firing the
     * selection callback.
     *
     * @param node the node to select
     */
    public void select(OrganizationNode node) {
        updating = true;
        try {
            if (node.isBrand()) {
                brandBox.setValue(node);
                chainBox.setItems(node.getChildren());
                chainBox.clear();
                storeBox.setItems(Collections.emptyList());
                storeBox.clear();
            } else if (node.isChain()) {
                OrganizationNode brand = node.getParent();
                brandBox.setValue(brand);
                chainBox.setItems(brand == null ? Collections.emptyList() : brand.getChildren());
                chainBox.setValue(node);
                storeBox.setItems(node.getChildren());
                storeBox.clear();
            } else {
                OrganizationNode chain = node.getParent();
                OrganizationNode brand = chain == null ? null : chain.getParent();
                brandBox.setValue(brand);
                chainBox.setItems(brand == null ? Collections.emptyList() : brand.getChildren());
                chainBox.setValue(chain);
                storeBox.setItems(chain == null ? Collections.emptyList() : chain.getChildren());
                storeBox.setValue(node);
            }
        } finally {
            updating = false;
        }
    }

    /**
     * Registers the selection callback.
     *
     * @param listener the callback invoked with the selected node
     */
    public void setSelectionListener(Consumer<OrganizationNode> listener) {
        this.selectionListener = listener;
    }

    private void notifySelection(OrganizationNode node) {
        if (selectionListener != null) {
            selectionListener.accept(node);
        }
    }
}
