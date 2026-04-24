package com.veda.app.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NoteHtml {
    private static final Pattern LINK_PATTERN = Pattern.compile("veda://note/(\\d+)");

    private NoteHtml() {}

    public static List<Long> extractLinkedNoteIds(String html) {
        if (html == null || html.isBlank()) return List.of();

        Matcher m = LINK_PATTERN.matcher(html);
        Set<Long> ids = new LinkedHashSet<>();
        while (m.find()) {
            try {
                long id = Long.parseLong(m.group(1));
                if (id > 0) ids.add(id);
            } catch (Exception ignored) {
                // no-op
            }
        }
        return new ArrayList<>(ids);
    }
}