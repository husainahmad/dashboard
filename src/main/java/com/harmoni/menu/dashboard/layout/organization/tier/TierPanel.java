package com.harmoni.menu.dashboard.layout.organization.tier;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Base class for the settings panel shown on the right of the Tier page.
 * Subclasses load their data on attach and persist it from {@link #save()}.
 */
public abstract class TierPanel extends VerticalLayout {

    private Runnable dirtyListener;
    private final boolean readOnly;

    protected TierPanel(boolean readOnly) {
        this.readOnly = readOnly;
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
        if (dirtyListener != null && !readOnly) {
            dirtyListener.run();
        }
    }

    /**
     * Whether the panel is rendered without editing controls.
     *
     * @return {@code true} when the user may only view this panel
     */
    protected boolean isReadOnly() {
        return readOnly;
    }

    /** Persists the panel's current state. */
    public abstract void save();
}
