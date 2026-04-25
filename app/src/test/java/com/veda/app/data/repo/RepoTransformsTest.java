package com.veda.app.data.repo;

import static org.junit.Assert.assertIterableEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class RepoTransformsTest {

    @Test
    public void sanitizeToNoteIds_removesNullInvalidSelfAndDuplicates() {
        assertIterableEquals(
                List.of(2L, 3L),
                RepoTransforms.sanitizeToNoteIds(1L, List.of(null, -1L, 0L, 1L, 2L, 2L, 3L))
        );
    }

    @Test
    public void sanitizeToNoteIds_handlesNullAndEmptyInput() {
        assertTrue(RepoTransforms.sanitizeToNoteIds(1L, null).isEmpty());
        assertTrue(RepoTransforms.sanitizeToNoteIds(1L, List.of()).isEmpty());
    }
}
