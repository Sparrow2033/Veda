package com.veda.app.data.repo;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class RepoTransforms {
    private RepoTransforms() {
    }

    @NonNull
    static Set<Long> sanitizeToNoteIds(long fromNoteId, List<Long> toNoteIds) {
        if (toNoteIds == null || toNoteIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> uniq = new LinkedHashSet<>();
        for (Long id : toNoteIds) {
            if (id == null) continue;
            if (id <= 0L) continue;
            if (id == fromNoteId) continue;
            uniq.add(id);
        }
        return uniq;
    }
}
