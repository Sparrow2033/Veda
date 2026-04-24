package com.veda.app.demo;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

public final class DemoLibraryHtml {

    private DemoLibraryHtml() {
    }

    @NonNull
    public static String buildInitialContentHtml(@NonNull DemoNoteSeed note) {
        StringBuilder sb = new StringBuilder();

        for (String paragraph : note.bodyParagraphs) {
            appendParagraph(sb, paragraph);
        }

        if (!note.linkSlugs.isEmpty()) {
            appendParagraph(sb, "Эта заметка входит в сеть знаний и после импорта будет связана с другими заметками библиотеки.");
        }

        return sb.toString();
    }

    @NonNull
    public static String buildFinalContentHtml(@NonNull DemoNoteSeed note,
                                               @NonNull Map<String, DemoNoteSeed> notesBySlug,
                                               @NonNull Map<String, Long> noteIdsBySlug) {
        StringBuilder sb = new StringBuilder();

        for (String paragraph : note.bodyParagraphs) {
            appendParagraph(sb, paragraph);
        }

        if (!note.linkSlugs.isEmpty()) {
            sb.append("<p>");
            sb.append(escapeHtml("Смотри также: "));

            for (int i = 0; i < note.linkSlugs.size(); i++) {
                String slug = note.linkSlugs.get(i);
                Long id = noteIdsBySlug.get(slug);
                DemoNoteSeed linked = notesBySlug.get(slug);

                if (id == null || linked == null) continue;

                if (i > 0) {
                    sb.append(escapeHtml(", "));
                }

                sb.append("<a href=\"veda://note/")
                        .append(id)
                        .append("\">")
                        .append(escapeHtml(linked.title))
                        .append("</a>");
            }

            sb.append("</p>");
        }

        return sb.toString();
    }

    private static void appendParagraph(@NonNull StringBuilder sb, @NonNull String text) {
        sb.append("<p>")
                .append(escapeHtml(text))
                .append("</p>");
    }

    @NonNull
    private static String escapeHtml(@NonNull String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}