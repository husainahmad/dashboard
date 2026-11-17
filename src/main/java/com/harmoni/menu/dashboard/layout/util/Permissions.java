package com.harmoni.menu.dashboard.layout.util;

/**
 * Permission names shared by navigation, route guards and action gating.
 */
public final class Permissions {

    public static final String DASHBOARD_VIEW = "dashboard:view";
    public static final String REPORT_VIEW = "report:view";
    public static final String REPORT_EXPORT = "report:export";
    public static final String MENU_VIEW = "menu:view";
    public static final String MENU_EDIT = "menu:edit";
    public static final String AVAILABILITY_VIEW = "availability:view";
    public static final String AVAILABILITY_EDIT = "availability:edit";
    public static final String TABLE_VIEW = "table:view";
    public static final String TABLE_EDIT = "table:edit";
    public static final String TERMINAL_VIEW = "terminal:view";
    public static final String TERMINAL_EDIT = "terminal:edit";
    public static final String EMPLOYEE_VIEW = "employee:view";
    public static final String EMPLOYEE_EDIT = "employee:edit";
    public static final String ORDER_VIEW = "order:view";
    public static final String ORDER_APPROVE = "order:approve";
    public static final String RBAC_MANAGE = "rbac:manage";

    public static final String CATALOG_READ = "catalog:read";
    public static final String CATALOG_WRITE = "catalog:write";
    public static final String CATALOG_DELETE = "catalog:delete";

    public static final String CUSTOMER_READ = "customer:read";
    public static final String CUSTOMER_WRITE = "customer:write";
    public static final String CUSTOMER_DELETE = "customer:delete";

    public static final String OPERATION_READ = "operation:read";
    public static final String OPERATION_WRITE = "operation:write";
    public static final String OPERATION_DELETE = "operation:delete";

    public static final String REPORT_READ = "report:read";

    public static final String ORGANIZATION_READ = "organization:read";
    public static final String ORGANIZATION_WRITE = "organization:write";
    public static final String ORGANIZATION_DELETE = "organization:delete";

    public static final String USER_READ = "user:read";
    public static final String USER_WRITE = "user:write";
    public static final String USER_DELETE = "user:delete";

    public static final String RBAC_READ = "rbac:read";
    public static final String RBAC_WRITE = "rbac:write";
    public static final String RBAC_DELETE = "rbac:delete";

    public static final String SETTING_READ = "setting:read";
    public static final String SETTING_WRITE = "setting:write";
    public static final String SETTING_DELETE = "setting:delete";

    private Permissions() {
        throw new IllegalStateException("Utility class");
    }
}
