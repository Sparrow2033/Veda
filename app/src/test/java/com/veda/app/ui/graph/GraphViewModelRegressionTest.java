package com.veda.app.ui.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GraphViewModelRegressionTest {

    @Test
    @SuppressWarnings("unchecked")
    public void deriveEdgesFromNoteBodies_buildsExpectedEdgesFromInternalLinks() throws Exception {
        Application app = ApplicationProvider.getApplicationContext();
        GraphViewModel vm = new GraphViewModel(app);

        Method derive = GraphViewModel.class.getDeclaredMethod("deriveEdgesFromNoteBodies", List.class, Set.class);
        derive.setAccessible(true);

        List<FakeNote> notes = List.of(
                new FakeNote(1L, "<a href='veda://note/2'>2</a><a href='veda://note/1'>self</a><a href='veda://note/99'>out</a>"),
                new FakeNote(2L, "<a href='veda://note/3'>3</a>"),
                new FakeNote(3L, "without-links")
        );
        Set<Long> allowed = Set.of(1L, 2L, 3L);

        List<Object> edges = (List<Object>) derive.invoke(vm, notes, allowed);
        Set<String> normalized = new HashSet<>();
        for (Object edge : edges) {
            Field fromField = edge.getClass().getDeclaredField("fromId");
            fromField.setAccessible(true);
            long from = fromField.getLong(edge);

            Field toField = edge.getClass().getDeclaredField("toId");
            toField.setAccessible(true);
            long to = toField.getLong(edge);
            normalized.add(from + "->" + to);
        }

        assertEquals(Set.of("1->2", "2->3"), normalized);
    }

    @Test
    public void buildEdgeKey_returnsSameValueForDifferentDirection() throws Exception {
        Application app = ApplicationProvider.getApplicationContext();
        GraphViewModel vm = new GraphViewModel(app);

        Method edgeKey = GraphViewModel.class.getDeclaredMethod("buildEdgeKey", long.class, long.class);
        edgeKey.setAccessible(true);

        String left = (String) edgeKey.invoke(vm, 10L, 11L);
        String right = (String) edgeKey.invoke(vm, 11L, 10L);
        assertEquals("10_11", left);
        assertEquals(left, right);
        assertTrue(left.contains("_"));
    }

    private static final class FakeNote {
        public final long id;
        public final String contentHtml;

        private FakeNote(long id, String contentHtml) {
            this.id = id;
            this.contentHtml = contentHtml;
        }
    }
}
