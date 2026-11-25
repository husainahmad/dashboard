package com.harmoni.menu.dashboard.layout.component;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownTextTest {

    @Test
    void rendersMarkdownAsHtml() {
        MarkdownText text = new MarkdownText("**Total**\n\n- one\n- two");

        assertThat(text.getElement().getProperty("innerHTML", ""))
                .contains("<strong>Total</strong>", "<li>one</li>", "<li>two</li>");
    }

    @Test
    void rendersGfmTables() {
        MarkdownText text = new MarkdownText("| Item | Qty |\n| --- | --- |\n| Tea | 2 |");

        assertThat(text.getElement().getProperty("innerHTML", ""))
                .contains("<table>", "<th>Item</th>", "<td>Tea</td>");
    }

    @Test
    void escapesRawHtmlFromModelOutput() {
        MarkdownText text = new MarkdownText("<script>alert('x')</script>");

        assertThat(text.getElement().getProperty("innerHTML", ""))
                .doesNotContain("<script>")
                .contains("&lt;script&gt;");
    }

    @Test
    void sanitizesUnsafeLinkUrls() {
        MarkdownText text = new MarkdownText("[click](javascript:alert(1))");

        assertThat(text.getElement().getProperty("innerHTML", ""))
                .doesNotContain("javascript:");
    }

    @Test
    void blankInputRendersNothing() {
        MarkdownText text = new MarkdownText("   ");

        assertThat(text.getElement().getProperty("innerHTML", "")).isEmpty();
    }

    @Test
    void setMarkdownReplacesContent() {
        MarkdownText text = new MarkdownText("first");
        text.setMarkdown("**second**");

        assertThat(text.getElement().getProperty("innerHTML", ""))
                .contains("<strong>second</strong>")
                .doesNotContain("first");
    }
}
