package com.demo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.core.entity.Entity;
import com.core.spatial.QuadTree;

@DisplayName("QuadTree")
class QuadTreeTest {

    /** World covering (0, 0, 400, 400) — square for easy manual subdivision math. */
    private static final float WX = 0, WY = 0, WW = 400, WH = 400;

    /** Helper: create a named entity with an AABB at (x, y, w, h). */
    private Entity<?> entity(String name, float x, float y, int w, int h) {
        Entity<?> e = new Entity<>(name);
        e.x = x;
        e.y = y;
        e.width  = w;
        e.height = h;
        return e;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Insertion")
    class Insertion {

        private QuadTree tree;

        @BeforeEach
        void setup() {
            // maxItems=4, maxDepth=8 — standard config
            tree = new QuadTree(WX, WY, WW, WH, 4, 8);
        }

        @Test
        @DisplayName("entity inside bounds is returned by a full-world query")
        void insertedEntityIsQueryable() {
            Entity<?> e = entity("e1", 50, 50, 20, 20);
            tree.insert(e);

            List<Entity<?>> result = tree.query(WX, WY, WW, WH);

            assertTrue(result.contains(e));
        }

        @Test
        @DisplayName("entity outside world bounds is silently ignored")
        void entityOutsideBoundsNotInserted() {
            Entity<?> e = entity("out", 500, 500, 20, 20);
            tree.insert(e);

            List<Entity<?>> result = tree.query(WX, WY, WW, WH);

            assertFalse(result.contains(e));
        }

        @Test
        @DisplayName("multiple entities all appear in full-world query")
        void multipleEntitiesAllReturned() {
            Entity<?> a = entity("a", 10,  10,  10, 10);
            Entity<?> b = entity("b", 210, 10,  10, 10);
            Entity<?> c = entity("c", 10,  210, 10, 10);
            Entity<?> d = entity("d", 210, 210, 10, 10);

            tree.insert(a);
            tree.insert(b);
            tree.insert(c);
            tree.insert(d);

            List<Entity<?>> result = tree.query(WX, WY, WW, WH);

            assertAll(
                () -> assertTrue(result.contains(a)),
                () -> assertTrue(result.contains(b)),
                () -> assertTrue(result.contains(c)),
                () -> assertTrue(result.contains(d))
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Query")
    class Query {

        private QuadTree tree;

        @BeforeEach
        void setup() {
            tree = new QuadTree(WX, WY, WW, WH, 4, 8);
        }

        @Test
        @DisplayName("query in NW quadrant returns only entities in that area")
        void queryNwQuadrant() {
            Entity<?> nw = entity("nw", 50,  50,  10, 10); // NW
            Entity<?> se = entity("se", 300, 300, 10, 10); // SE

            tree.insert(nw);
            tree.insert(se);

            // Query the NW quadrant only
            List<Entity<?>> result = tree.query(0, 0, 200, 200);

            assertTrue(result.contains(nw));
            assertFalse(result.contains(se));
        }

        @Test
        @DisplayName("query with no overlapping entities returns empty list")
        void queryEmptyArea() {
            tree.insert(entity("far", 350, 350, 10, 10));

            List<Entity<?>> result = tree.query(0, 0, 100, 100);

            assertTrue(result.isEmpty());
        }

        @ParameterizedTest(name = "entity at ({0},{1}) size {2}x{3} — visible in [{4},{5},{6},{7}]")
        @DisplayName("entity partially overlapping query rect is included")
        @CsvSource({
            // entity partially overlaps left edge of query rect
            "95, 50, 20, 20,  100, 0, 200, 200",
            // entity partially overlaps top edge
            "50, 95, 20, 20,  0, 100, 200, 200",
        })
        void entityPartiallyOverlappingIsIncluded(
                float ex, float ey, int ew, int eh,
                float qx, float qy, float qw, float qh) {
            Entity<?> e = entity("e", ex, ey, ew, eh);
            tree.insert(e);

            List<Entity<?>> result = tree.query(qx, qy, qw, qh);

            assertTrue(result.contains(e));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Subdivision")
    class Subdivision {

        @Test
        @DisplayName("inserting more than maxItems entities triggers subdivision")
        void subdivisionOnCapacityExceeded() {
            // maxItems=2 — subdivision fires after the 3rd insert
            QuadTree small = new QuadTree(WX, WY, WW, WH, 2, 8);

            // Place entities far apart so they land in distinct quadrants after subdivision
            Entity<?> e1 = entity("e1",  10,  10, 5, 5);
            Entity<?> e2 = entity("e2", 300,  10, 5, 5);
            Entity<?> e3 = entity("e3",  10, 300, 5, 5);

            small.insert(e1);
            small.insert(e2);
            small.insert(e3);

            // After subdivision all three entities must still be queryable
            List<Entity<?>> result = small.query(WX, WY, WW, WH);

            assertAll(
                () -> assertTrue(result.contains(e1)),
                () -> assertTrue(result.contains(e2)),
                () -> assertTrue(result.contains(e3))
            );
        }

        @Test
        @DisplayName("maxDepth=0 prevents any subdivision — all entities stay in root")
        void maxDepthZeroNoSubdivision() {
            QuadTree flat = new QuadTree(WX, WY, WW, WH, 1, 0);

            Entity<?> a = entity("a", 10,  10, 5, 5);
            Entity<?> b = entity("b", 300, 300, 5, 5);

            flat.insert(a);
            flat.insert(b);

            List<Entity<?>> result = flat.query(WX, WY, WW, WH);

            assertAll(
                () -> assertTrue(result.contains(a)),
                () -> assertTrue(result.contains(b))
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Deduplication")
    class Deduplication {

        @Test
        @DisplayName("entity spanning two quadrants appears exactly once in query result")
        void entitySpanningQuadrantsDeduplicatedInQuery() {
            // maxItems=1 forces immediate subdivision
            QuadTree tree = new QuadTree(WX, WY, WW, WH, 1, 8);

            // This entity straddles the horizontal split (y=200) — occupies both NW and SW
            Entity<?> straddler = entity("straddler", 50, 190, 20, 20);
            Entity<?> trigger   = entity("trigger",   10,  10,  5,  5); // forces subdivision

            tree.insert(trigger);
            tree.insert(straddler);

            List<Entity<?>> result = tree.query(WX, WY, WW, WH);

            long count = result.stream().filter(e -> e.name.equals("straddler")).count();
            assertEquals(1L, count, "straddler must appear exactly once");
        }

        @Test
        @DisplayName("clear() removes all entities and resets to single leaf")
        void clearResetsTree() {
            QuadTree tree = new QuadTree(WX, WY, WW, WH, 2, 8);

            tree.insert(entity("a",  10,  10, 5, 5));
            tree.insert(entity("b", 300,  10, 5, 5));
            tree.insert(entity("c",  10, 300, 5, 5));

            tree.clear();

            List<Entity<?>> result = tree.query(WX, WY, WW, WH);
            assertTrue(result.isEmpty(), "tree must be empty after clear()");
        }
    }
}
