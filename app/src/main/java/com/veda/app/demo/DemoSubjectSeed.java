package com.veda.app.demo;

import androidx.annotation.NonNull;

public final class DemoSubjectSeed {

    public final String slug;
    public final String name;
    public final String colorHex;

    public DemoSubjectSeed(@NonNull String slug,
                           @NonNull String name,
                           @NonNull String colorHex) {
        this.slug = slug;
        this.name = name;
        this.colorHex = colorHex;
    }
}