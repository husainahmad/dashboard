package com.harmoni.menu.dashboard.layout.component;

import com.vaadin.flow.component.html.Span;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;

public class MarkdownText extends Span {

    private static final Parser PARSER = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();

    private static final HtmlRenderer RENDERER = HtmlRenderer.builder()
            .softbreak("<br />")
            .escapeHtml(true)
            .sanitizeUrls(true)
            .extensions(List.of(TablesExtension.create()))
            .build();

    public MarkdownText(String markdown) {
        addClassName("dashboard-ai-markdown");
        setMarkdown(markdown);
    }

    public void setMarkdown(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            getElement().setProperty("innerHTML", "");
            return;
        }
        Node node = PARSER.parse(markdown);
        getElement().setProperty("innerHTML", RENDERER.render(node));
    }
}
