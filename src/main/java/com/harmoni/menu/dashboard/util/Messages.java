package com.harmoni.menu.dashboard.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Static accessor for the {@link MessageSource} defined in
 * {@code messages.properties} (and {@code messages_id.properties}).
 *
 * <p>Lets any component, form or static utility resolve a translated string
 * without being a Spring bean itself; falls back to the key when the context
 * is not yet available or the key is missing.
 * </p>
 *
 * <p>Translations come from the locale the user picked for their session (see
 * {@link VaadinSessionUtil#setLocale(Locale)}), falling back to the JVM default
 * locale.
 * </p>
 */
@Component
public class Messages implements ApplicationContextAware {

    /**
     * Message keys that are referenced from multiple places, so a key only
     * needs to be changed here rather than at every call site.
     */
    public static final class Keys {

        private Keys() {
            throw new IllegalStateException("Utility class");
        }

        public static final String ACTION_CANCEL = "action.cancel";
        public static final String ACTION_CLOSE = "action.close";
        public static final String ACTION_DELETE = "action.delete";
        public static final String ACTION_EDIT = "action.edit";
        public static final String ACTION_EDIT_NAME = "action.edit.name";
        public static final String ACTION_EDIT_NAME_FLAT = "action.editName";
        public static final String ACTION_NEW_BRAND = "action.newBrand";
        public static final String ACTION_NEW_CATEGORY = "action.newCategory";
        public static final String ACTION_NEW_CHAIN = "action.newChain";
        public static final String ACTION_NEW_PRODUCT = "action.newProduct";
        public static final String ACTION_NEW_SERVICE = "action.newService";
        public static final String ACTION_NEW_STORE = "action.newStore";
        public static final String ACTION_NEW_TABLE = "action.newTable";
        public static final String ACTION_NEW_USER = "action.newUser";
        public static final String ACTION_SAVE = "action.save";
        public static final String ACTION_UPDATE = "action.update";
        public static final String ACTION_VIEW_DETAIL = "action.viewDetail";
        public static final String ACTION_VIEW_NAME = "action.view.name";
        public static final String DASHBOARD_PRODUCT = "dashboard.product";
        public static final String DASHBOARD_SKU = "dashboard.sku";
        public static final String DIALOG_CONFIRM_TITLE = "dialog.confirmTitle";
        public static final String GRID_HEADER_ACTION = "grid.header.action";
        public static final String GRID_HEADER_ACTIVE = "grid.header.active";
        public static final String GRID_HEADER_BRAND_NAME = "grid.header.brandName";
        public static final String GRID_HEADER_CREATED_AT = "grid.header.createdAt";
        public static final String GRID_HEADER_EMAIL = "grid.header.email";
        public static final String GRID_HEADER_NAME = "grid.header.name";
        public static final String GRID_HEADER_PHONE = "grid.header.phone";
        public static final String GRID_HEADER_PRICE = "grid.header.price";
        public static final String GRID_HEADER_TYPE = "grid.header.type";
        public static final String LABEL_ALL = "label.all";
        public static final String LABEL_BRAND = "label.brand";
        public static final String LABEL_CAPACITY = "label.capacity";
        public static final String LABEL_CATEGORY = "label.category";
        public static final String LABEL_CUSTOMER_EMAIL = "label.customer.email";
        public static final String LABEL_CUSTOMER_NAME = "label.customer.name";
        public static final String LABEL_CUSTOMER_PHONE = "label.customer.phone";
        public static final String LABEL_DESCRIPTION = "label.description";
        public static final String LABEL_FIELD_CUSTOMIZATION_NAME = "label.field.customizationName";
        public static final String LABEL_FIELD_SERVICE_NAME = "label.field.serviceName";
        public static final String LABEL_FIELD_TABLE_NAME = "label.field.tableName";
        public static final String LABEL_PASSWORD = "label.password";
        public static final String LABEL_PRICE = "label.price";
        public static final String LABEL_REQUIRED = "label.required";
        public static final String LABEL_SEARCH = "label.search";
        public static final String LABEL_SELECTION_TYPE = "label.selectionType";
        public static final String LABEL_SERVICE = "label.service";
        public static final String LABEL_STORE = "label.store";
        public static final String LABEL_TIER = "label.tier";
        public static final String NOTIFICATION_BRAND_CREATED = "notification.brand.created";
        public static final String NOTIFICATION_BRAND_INSERT_ERROR = "notification.brand.insertError";
        public static final String NOTIFICATION_BRAND_LOAD_FAILED = "notification.brand.loadFailed";
        public static final String NOTIFICATION_CATEGORY_CREATED = "notification.category.created";
        public static final String NOTIFICATION_CATEGORY_LOAD_FAILED = "notification.category.loadFailed";
        public static final String NOTIFICATION_CHAIN_LOAD_FAILED = "notification.chain.loadFailed";
        public static final String NOTIFICATION_CUSTOMIZATION_LOAD_FAILED = "notification.customization.loadFailed";
        public static final String NOTIFICATION_CUSTOMIZATION_REMOVED = "notification.customization.removed";
        public static final String NOTIFICATION_CUSTOMIZATION_UPDATED = "notification.customization.updated";
        public static final String NOTIFICATION_CUSTOMER_LOAD_FAILED = "notification.customer.loadFailed";
        public static final String NOTIFICATION_CUSTOMER_NOT_FOUND = "notification.customer.notFound";
        public static final String NOTIFICATION_LOGIN_FAILED = "notification.login.failed";
        public static final String NOTIFICATION_PRODUCT_LOAD_FAILED = "notification.product.loadFailed";
        public static final String NOTIFICATION_SERVICE_LOAD_FAILED = "notification.service.loadFailed";
        public static final String NOTIFICATION_TABLE_LOAD_FAILED = "notification.table.loadFailed";
        public static final String NOTIFICATION_TIER_DELETE_ERROR = "notification.tier.deleteError";
        public static final String NOTIFICATION_TIER_UPDATE_ERROR = "notification.tier.updateError";
        public static final String UI_KEYS_TITLE = "ui.keys.title";
        public static final String VALIDATION_BRAND_REQUIRED = "validation.brand.required";
        public static final String VALIDATION_CUSTOMIZATION_FILL_OPTION_FIELDS = "validation.customization.fillOptionFields";
        public static final String VALIDATION_CUSTOMIZATION_FORM_ERRORS = "validation.customization.formErrors";
        public static final String VALIDATION_CUSTOMIZATION_MIN_OPTION = "validation.customization.minOption";
        public static final String VALIDATION_NAME_MIN_LENGTH = "validation.name.minLength";
    }

    private static MessageSource messageSource;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        messageSource = applicationContext.getBean(MessageSource.class);
    }

    /**
     * Resolves a message key against the locale of the current session.
     *
     * @param key  the message key
     * @param args optional placeholders, formatted into the message value
     * @return the translated text, or the key itself when unknown
     */
    public static String get(String key, Object... args) {
        if (messageSource == null) {
            return key;
        }
        return messageSource.getMessage(key, args, key, resolveLocale());
    }

    /**
     * Resolves the locale messages are translated to: the one selected for the
     * current user session, falling back to the JVM default when the user has
     * not chosen one.
     *
     * <p>Reading the locale per session keeps the language choice scoped to the
     * signed-in user instead of the whole server.</p>
     *
     * @return the locale to translate to
     */
    private static Locale resolveLocale() {
        Locale sessionLocale = VaadinSessionUtil.getLocale();
        return sessionLocale != null ? sessionLocale : Locale.getDefault();
    }
}