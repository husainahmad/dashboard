package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.dto.growth.DashboardAiAnswerDto;
import com.harmoni.menu.dashboard.layout.ai.AiPageContext;
import com.harmoni.menu.dashboard.service.data.rest.GrowthRestClientService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/**
 * Right-side assistant drawer opened from the application toolbar. It asks the
 * AI about the page context currently rendered by the active view.
 */
@Slf4j
public class DashboardAiPanel extends VerticalLayout {

    private static final String MESSAGE_CLASS = "dashboard-ai-message";

    private final transient GrowthRestClientService client;
    private final Supplier<AiPageContext> contextSupplier;
    private final TextArea question = new TextArea(Messages.get("dashboard.ai.question"));
    private final Div answerArea = new Div();
    private final Button askButton = new Button(Messages.get("dashboard.ai.ask"), VaadinIcon.PAPERPLANE.create());
    private final Span contextLabel = new Span();

    private AppLayout host;
    private AiPageContext currentContext;

    public DashboardAiPanel(GrowthRestClientService client, Supplier<AiPageContext> contextSupplier) {
        this.client = client;
        this.contextSupplier = contextSupplier;
        this.currentContext = contextOrFallback(null);

        addClassName("dashboard-ai-panel");
        setPadding(true);
        setSpacing(true);
        setSizeFull();
        getElement().setAttribute("role", "complementary");

        H2 title = new H2(Messages.get("dashboard.ai.title"));
        title.addClassName("dashboard-ai-panel-title");

        contextLabel.addClassName("dashboard-ai-context");

        Button closeButton = new Button(VaadinIcon.CLOSE_SMALL.create(), event -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        closeButton.setAriaLabel(Messages.get(Messages.Keys.ACTION_CLOSE));
        closeButton.addClassName("dashboard-ai-panel-close");

        HorizontalLayout header = new HorizontalLayout(title, contextLabel, closeButton);
        header.addClassName("dashboard-ai-panel-header");
        header.setWidthFull();
        header.setPadding(false);
        header.setSpacing(true);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setFlexGrow(1, title);

        question.setWidthFull();
        question.setMinHeight("7rem");
        question.setMaxLength(2000);
        question.addClassName("dashboard-ai-question");
        question.getElement().addEventListener("keydown", event -> {}).stopPropagation();

        answerArea.addClassName("dashboard-ai-answer");
        answerArea.getElement().setAttribute("aria-live", "polite");
        updateContextLabel();
        showHint();

        askButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        askButton.addClickListener(event -> ask());

        HorizontalLayout actions = new HorizontalLayout(askButton);
        actions.addClassName("dashboard-ai-panel-actions");
        actions.setWidthFull();
        actions.setPadding(false);
        actions.setSpacing(false);
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        add(header, question, actions, answerArea);
        setFlexGrow(1, answerArea);
    }

    public void attachTo(AppLayout hostLayout) {
        this.host = hostLayout;
    }

    public void open() {
        currentContext = contextOrFallback(contextSupplier.get());
        updateContextLabel();
        showHint();

        if (host != null) {
            host.getElement().setAttribute("ai-drawer-open", "");
        }
        question.focus();
    }

    public void close() {
        if (host != null) {
            host.getElement().removeAttribute("ai-drawer-open");
        }
    }

    public void toggle() {
        if (isOpen()) {
            close();
        } else {
            open();
        }
    }

    public boolean isOpen() {
        return host != null && host.getElement().hasAttribute("ai-drawer-open");
    }

    public void onPageContextChanged(AiPageContext context) {
        currentContext = contextOrFallback(context);
        updateContextLabel();
        if (isOpen() && (question.getValue() == null || question.getValue().isBlank())) {
            showHint();
        }
    }

    private void ask() {
        String message = question.getValue() == null ? "" : question.getValue().trim();
        if (message.isBlank()) {
            question.setInvalid(true);
            question.setErrorMessage(Messages.get("dashboard.ai.required"));
            return;
        }
        question.setInvalid(false);

        AiPageContext context = contextOrFallback(contextSupplier.get());
        currentContext = context;
        updateContextLabel();
        String token = VaadinSessionUtil.getAttribute(VaadinSessionUtil.JWT_TOKEN, String.class);
        UI ui = getUI().orElse(null);
        askButton.setEnabled(false);
        showLoading();

        CompletableFuture.supplyAsync(() -> client.askDashboardAi(
                        message, context.title(), context.toolName(), context.summary(), token))
                .whenComplete((response, error) -> {
                    if (ui == null) {
                        return;
                    }
                    ui.access(() -> {
                        askButton.setEnabled(true);
                        if (error != null) {
                            showError(error);
                            return;
                        }
                        showAnswer(answerText(response));
                    });
                });
    }

    private void showHint() {
        showMessage(Messages.get("dashboard.ai.hintForPage", currentContext.title()),
                "dashboard-ai-message--hint");
    }

    private void showLoading() {
        showMessage(Messages.get("dashboard.ai.thinkingForPage", currentContext.title()),
                "dashboard-ai-message--loading");
    }

    private void updateContextLabel() {
        contextLabel.setText(Messages.get("dashboard.ai.context", currentContext.title()));
        question.setPlaceholder(Messages.get("dashboard.ai.placeholderForPage", currentContext.title()));
    }

    private static AiPageContext contextOrFallback(AiPageContext context) {
        String title = context == null || context.title() == null || context.title().isBlank()
                ? Messages.get("dashboard.ai.unknownPage") : context.title();
        String summary = context == null || context.summary() == null || context.summary().isBlank()
                ? Messages.get("dashboard.ai.noPageData", title) : context.summary();
        String toolName = context == null || context.toolName() == null ? "" : context.toolName();
        return new AiPageContext(title, summary, toolName);
    }

    private void showAnswer(String text) {
        try {
            showMessage(new MarkdownText(text), "dashboard-ai-message--answer");
        } catch (LinkageError | RuntimeException error) {
            log.warn("Could not render the AI answer as markdown; showing plain text", error);
            showMessage(new Span(text), "dashboard-ai-message--answer");
        }
    }

    private void showMessage(String text, String modifier) {
        showMessage(new Span(text), modifier);
    }

    private void showMessage(Component body, String modifier) {
        answerArea.removeAll();
        Div message = new Div();
        message.addClassName(MESSAGE_CLASS);
        message.addClassName(modifier);
        body.addClassName("dashboard-ai-message-text");
        message.add(body);
        answerArea.add(message);
    }

    private void showError(Throwable error) {
        log.warn("Could not get a dashboard AI answer", error);

        Span title = new Span(Messages.get("dashboard.ai.errorTitle"));
        title.addClassName("dashboard-ai-error-title");
        Span detail = new Span(errorMessage(error));
        detail.addClassName("dashboard-ai-error-detail");

        Button retry = new Button(Messages.get("dashboard.ai.retry"), event -> ask());
        retry.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
        retry.addClassName("dashboard-ai-retry");

        Div message = new Div(title, detail, retry);
        message.addClassName(MESSAGE_CLASS);
        message.addClassName("dashboard-ai-message--error");

        answerArea.removeAll();
        answerArea.add(message);
    }

    private static String errorMessage(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null
                ? error.getCause() : error;
        if (cause instanceof WebClientResponseException response) {
            int status = response.getStatusCode().value();
            if (status == 401 || status == 403) {
                return Messages.get("dashboard.ai.errorAuth");
            }
            if (status >= 500) {
                return Messages.get("dashboard.ai.errorUnavailable");
            }
        }
        if (cause instanceof WebClientRequestException) {
            return Messages.get("dashboard.ai.errorUnavailable");
        }
        return Messages.get("dashboard.ai.error");
    }

    private static String answerText(DashboardAiAnswerDto response) {
        if (response == null || response.answer() == null || response.answer().isBlank()) {
            return Messages.get("dashboard.ai.empty");
        }
        return response.answer();
    }
}
