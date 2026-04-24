package com.veda.app.demo;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DemoNoteSeed {

    public final String slug;
    public final String title;
    public final String subjectSlug;
    public final List<String> tags;
    public final List<String> bodyParagraphs;
    public final List<String> linkSlugs;

    public DemoNoteSeed(@NonNull String slug,
                        @NonNull String title,
                        @NonNull String subjectSlug,
                        @NonNull List<String> tags,
                        @NonNull List<String> bodyParagraphs,
                        @NonNull List<String> linkSlugs) {
        this.slug = slug;
        this.title = title;
        this.subjectSlug = subjectSlug;
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
        this.bodyParagraphs = Collections.unmodifiableList(new ArrayList<>(bodyParagraphs));
        this.linkSlugs = Collections.unmodifiableList(new ArrayList<>(linkSlugs));
    }
}