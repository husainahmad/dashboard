package com.harmoni.menu.dashboard.layout.organization;

import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;

import java.util.List;
import java.util.function.Consumer;

/**
 * Left-hand card of the organization page: a tree of brands, chains and stores
 * with type badges, child counts and store status.
 */
public class OrganizationTree extends VerticalLayout {

    private final TreeGrid<OrganizationNode> treeGrid = new TreeGrid<>();
    private Consumer<OrganizationNode> selectionListener;

    public OrganizationTree() {
        addClassName("org-tree-card");
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        H3 title = new H3(Messages.get("organization.tree.title"));
        title.addClassName("org-card-title");

        configureGrid();

        add(title, treeGrid);
        setFlexGrow(1, treeGrid);
    }

    private void configureGrid() {
        treeGrid.addClassName("org-tree");
        treeGrid.setSizeFull();
        treeGrid.setSelectionMode(com.vaadin.flow.component.grid.Grid.SelectionMode.SINGLE);

        treeGrid.addComponentHierarchyColumn(this::nameCell)
                .setHeader(Messages.get("organization.tree.name"))
                .setFlexGrow(4);
        treeGrid.addComponentColumn(this::countCell)
                .setHeader(Messages.get("organization.tree.count"))
                .setAutoWidth(true)
                .setFlexGrow(0);
        treeGrid.addComponentColumn(this::statusCell)
                .setHeader(Messages.get("organization.tree.status"))
                .setWidth("84px")
                .setFlexGrow(0);

        treeGrid.addSelectionListener(event -> {
            if (selectionListener != null) {
                event.getFirstSelectedItem().ifPresent(selectionListener);
            }
        });
    }

    /**
     * Replaces the tree contents and expands every brand and chain.
     *
     * @param roots the brand nodes
     */
    public void setRoots(List<OrganizationNode> roots) {
        treeGrid.setItems(roots, OrganizationNode::getChildren);
        treeGrid.expand(roots);
        roots.forEach(root -> treeGrid.expand(root.getChildren()));
    }

    /**
     * Selects the given node and notifies the listener.
     *
     * @param node the node to select
     */
    public void select(OrganizationNode node) {
        treeGrid.select(node);
    }

    /**
     * Registers the selection callback.
     *
     * @param listener the callback invoked with the selected node
     */
    public void setSelectionListener(Consumer<OrganizationNode> listener) {
        this.selectionListener = listener;
    }

    private Component nameCell(OrganizationNode node) {
        Div row = new Div();
        row.addClassName("org-tree-name");

        Span name = new Span(node.getName());
        name.addClassName("org-tree-label");
        row.add(name);

        Span badge = new Span(typeLabel(node));
        badge.addClassName("org-badge");
        badge.addClassName(switch (node.getType()) {
            case BRAND -> "org-badge-brand";
            case CHAIN -> "org-badge-chain";
            case STORE -> "org-badge-store";
        });
        row.add(badge);
        return row;
    }

    private Component countCell(OrganizationNode node) {
        Span count = new Span();
        count.addClassName("org-tree-count");
        if (node.isBrand()) {
            count.setText(Messages.get("organization.count.chainsStores",
                    node.childCount(), node.storeCount()));
        } else if (node.isChain()) {
            count.setText(Messages.get("organization.count.stores", node.childCount()));
        }
        return count;
    }

    private Component statusCell(OrganizationNode node) {
        Span status = new Span();
        status.addClassName("org-tree-status");
        if (node.isStore()) {
            status.setText(Messages.get("organization.status.active"));
            status.addClassName("org-status-active");
        }
        return status;
    }

    private String typeLabel(OrganizationNode node) {
        return switch (node.getType()) {
            case BRAND -> Messages.get("organization.type.brand");
            case CHAIN -> Messages.get("organization.type.chain");
            case STORE -> Messages.get("organization.type.store");
        };
    }
}
