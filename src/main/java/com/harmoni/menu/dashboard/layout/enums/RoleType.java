package com.harmoni.menu.dashboard.layout.enums;

import lombok.Getter;

@Getter
public enum RoleType {
    ADMIN(1),
    MANAGER(2),
    USER(3);

    private final int id;

    RoleType(int id) {
        this.id = id;
    }
}

