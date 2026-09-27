package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.event.promotion.PromotionSaveEventListener;
import com.harmoni.menu.dashboard.event.promotion.PromotionStatusEventListener;
import com.harmoni.menu.dashboard.event.promotion.PromotionUpdateEventListener;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientPromotionService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

/**
 * The promotion editor: a {@link PromotionForm} beside a live {@link PromotionPreview},
 * with the action footer for the form's current {@code FormAction}.
 *
 * <p>Not a route: the editor is only ever built by
 * {@link PromotionListView} inside its parent {@code TabSheet}, and it needs the
 * {@link TabManager} and {@link Tab} it was opened on, neither of which is a Spring
 * bean. It is therefore constructed directly rather than autowired.</p>
 */
@Slf4j
public class PromotionFormWithPreview extends VerticalLayout {

    @Getter
    private final RestClientPromotionService restClientPromotionService;

    @Getter
    private final AsyncRestClientMenuService asyncRestClientMenuService;

    @Getter
    private final TabManager tabManager;

    @Getter
    private final Tab currentTab;

    @Getter
    private final FormAction formAction;

    @Getter
    private final PromotionDto promotionDto;

    @Getter
    private final PromotionForm promotionForm;

    @Getter
    private final PromotionPreview promotionPreview;

    private final Button saveButton = new Button(Messages.get(Messages.Keys.ACTION_SAVE));
    private final Button updateButton = new Button(Messages.get(Messages.Keys.ACTION_UPDATE));
    private final Button statusButton = new Button(Messages.get("action.applyStatus"));
    private final Button closeButton = new Button(Messages.get(Messages.Keys.ACTION_CANCEL));

    private UI ui;

    public PromotionFormWithPreview(RestClientPromotionService restClientPromotionService,
                                     AsyncRestClientMenuService asyncRestClientMenuService,
                                     TabManager tabManager,
                                     Tab currentTab,
                                     FormAction formAction,
                                     PromotionDto promotionDto,
                                     AccessService accessService) {
        this.restClientPromotionService = restClientPromotionService;
        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.tabManager = tabManager;
        this.currentTab = currentTab;
        this.formAction = formAction;
        this.promotionDto = promotionDto;

        this.promotionForm = new PromotionForm(restClientPromotionService, asyncRestClientMenuService, tabManager,
                currentTab, formAction, promotionDto, accessService);
        this.promotionPreview = new PromotionPreview();

        promotionForm.setPreviewUpdater(promotionPreview::updatePreview);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();
        buildLayout();
        addFooterButtons();
        restructureButton(formAction);
        promotionForm.firePreviewUpdate();
    }

    private void buildLayout() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(false);
        mainLayout.setPadding(false);
        mainLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        mainLayout.addClassName("promotion-editor-layout");

        // Form column - takes the remaining space, capped at 1000px. Its sizing is
        // declared in forms.css, not here, so the single-column fallback in the media
        // query can still reach it: an inline width outranks every stylesheet rule,
        // however specific, and the two used to disagree with the Java always winning.
        VerticalLayout formWrapper = new VerticalLayout(promotionForm);
        formWrapper.setPadding(true);
        formWrapper.addClassName("promotion-form-wrapper");
        releaseDefaultWidth(formWrapper);

        promotionForm.setWidthFull();

        // Preview column - a fixed band, for the same reason as the form column. All
        // three of width, min-width and max-width were set here before, which left the
        // media query's full-width fallback for this column unreachable.
        VerticalLayout previewWrapper = new VerticalLayout(promotionPreview);
        previewWrapper.setPadding(true);
        previewWrapper.setAlignItems(FlexComponent.Alignment.STRETCH);
        previewWrapper.addClassName("promotion-preview-wrapper");
        releaseDefaultWidth(previewWrapper);

        mainLayout.add(formWrapper, previewWrapper);

        add(mainLayout);
        setFlexGrow(1, mainLayout);
    }

    /**
     * Drops the inline {@code width: 100%} that {@code VerticalLayout}'s constructor
     * puts on every instance, so the stylesheet alone describes the column's width.
     *
     * <p>Leaving the default in place is what makes the split sources of truth: on the
     * form column it is merely inert, because the rule's flex-basis is what sizes a
     * flex item on its container's main axis, but on the preview column the width is
     * the entire point of the column, and it would win over both the rule and the media
     * query that widens it on a narrow screen.</p>
     *
     * @param column the wrapper whose constructor default should not apply
     */
    private static void releaseDefaultWidth(VerticalLayout column) {
        column.getElement().getStyle().remove("width");
    }

    /**
     * Builds the footer. Exactly one of the three committing buttons is shown,
     * depending on the action the editor was opened for, so the operator is never
     * offered a save that would post the wrong payload.
     */
    private void addFooterButtons() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        statusButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);
        statusButton.addClickShortcut(Key.ENTER);
        closeButton.addClickShortcut(Key.ESCAPE);

        saveButton.addClickListener(new PromotionSaveEventListener(promotionForm, restClientPromotionService));
        updateButton.addClickListener(new PromotionUpdateEventListener(promotionForm, restClientPromotionService));
        statusButton.addClickListener(new PromotionStatusEventListener(promotionForm, restClientPromotionService));
        closeButton.addClickListener(event -> close());

        HorizontalLayout footer = new HorizontalLayout(closeButton, saveButton, updateButton, statusButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setSpacing(true);
        footer.setPadding(true);
        footer.addClassName("promotion-editor-footer");

        add(footer);
    }

    /**
     * Shows the button matching the current action: Save for a new promotion, Update
     * for an edit, and Apply Status for a status transition. The form applies the
     * matching read-only state.
     *
     * @param formAction the action the editor was opened for
     */
    public void restructureButton(FormAction formAction) {
        boolean creating = ObjectUtils.defaultIfNull(formAction, FormAction.CREATE) == FormAction.CREATE;
        boolean statusOnly = formAction == FormAction.STATUS;
        saveButton.setVisible(creating);
        updateButton.setVisible(!creating && !statusOnly);
        statusButton.setVisible(statusOnly);
        closeButton.setVisible(true);
        promotionForm.restructureButton(formAction);
    }

    public void close() {
        UiUtil.safeAccess(ui, () -> tabManager.closeAndSelectFirst(currentTab));
    }
}
