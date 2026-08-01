package com.harmoni.menu.dashboard.layout.enums;

import lombok.Getter;

@Getter
public enum SelectionType {
    SINGLE("Single"),
    MULTIPLE("Multiple");

    private final String label;

    SelectionType(String label) {
        this.label = label;
    }

}
