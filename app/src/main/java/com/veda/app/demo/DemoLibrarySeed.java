package com.veda.app.demo;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DemoLibrarySeed {

    public final int version;
    public final List<DemoSubjectSeed> subjects;
    public final List<DemoNoteSeed> notes;

    public DemoLibrarySeed(int version,
                           @NonNull List<DemoSubjectSeed> subjects,
                           @NonNull List<DemoNoteSeed> notes) {
        this.version = version;
        this.subjects = Collections.unmodifiableList(new ArrayList<>(subjects));
        this.notes = Collections.unmodifiableList(new ArrayList<>(notes));
    }
}