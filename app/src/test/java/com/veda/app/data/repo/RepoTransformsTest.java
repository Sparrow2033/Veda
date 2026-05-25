package com.veda.app.data.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

public class RepoTransformsTest {

    @Test
    public void sanitizeToNoteIds_removesNullInvalidSelfAndDuplicates() {
        assertEquals(
                new LinkedHashSet<>(List.of(2L, 3L)),
                RepoTransforms.sanitizeToNoteIds(1L, Arrays.asList(null, -1L, 0L, 1L, 2L, 2L, 3L))
        );
    }

    @Test
    public void sanitizeToNoteIds_handlesNullAndEmptyInput() {
        assertTrue(RepoTransforms.sanitizeToNoteIds(1L, null).isEmpty());
        assertTrue(RepoTransforms.sanitizeToNoteIds(1L, List.of()).isEmpty());
    }
}
