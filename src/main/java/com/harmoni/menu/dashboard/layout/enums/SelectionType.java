package com.harmoni.menu.dashboard.layout.enums;

import lombok.Getter;

/**
 * Selection behavior for a multi-choice field.
 */
@Getter
public enum SelectionType {
    /** Allows choosing exactly one value. */
    SINGLE("Single"),
    /** Allows choosing more than one value. */
    MULTIPLE("Multiple");

    private final String label;

    SelectionType(String label) {
        this.label = label;
    }

}
