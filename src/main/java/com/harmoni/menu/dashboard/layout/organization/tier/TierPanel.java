package com.harmoni.menu.dashboard.layout.organization.tier;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Base class for the settings panel shown on the right of the Tier page.
 * Subclasses load their data on attach and persist it from {@link #save()}.
 */
public abstract class TierPanel extends VerticalLayout {

    private Runnable dirtyListener;

    protected TierPanel() {
        addClassName("tier-panel");
        setPadding(false);
        setSpacing(true);
        setSizeFull();
    }

    /**
     * Registers the callback fired when the panel has unsaved changes.
     *
     * @param listener the dirty-state callback
     */
    public void setDirtyListener(Runnable listener) {
        this.dirtyListener = listener;
    }

    /** Marks the panel as having unsaved changes. */
    protected void markDirty() {
        if (dirtyListener != null) {
            dirtyListener.run();
        }
    }

    /** Persists the panel's current state. */
    public abstract void save();
}
