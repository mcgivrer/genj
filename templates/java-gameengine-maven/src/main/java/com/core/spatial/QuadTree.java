package com.core.spatial;

import com.core.entity.Entity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Axis-Aligned Bounding Box (AABB) QuadTree for 2D spatial partitioning.
 *
 * <p>The tree recursively subdivides its area into four equal quadrants (NW, NE, SW, SE)
 * when a node exceeds {@code maxItems} capacity and the maximum depth has not been reached.
 * An entity whose bounding box spans multiple quadrants is inserted into all overlapping
 * children; deduplication is handled by {@link #query} via a {@link LinkedHashSet}.
 *
 * <p>The tree is designed to be rebuilt every frame: call {@link #clear()} then re-insert
 * all active entities. The root bounds must match the world dimensions.
 *
 * @see com.core.scene.BaseScene#rebuildSpatialIndex()
 */
public class QuadTree {

    private static final int NW = 0, NE = 1, SW = 2, SE = 3;

    private final float x, y, w, h;
    private final int maxItems, maxDepth, depth;

    /** Entities stored in this node (only used when this node is a leaf). */
    private final List<Entity<?>> items = new ArrayList<>();

    /** Child nodes; {@code null} while this node is a leaf. */
    private QuadTree[] children = null;

    /**
     * Creates a root QuadTree node covering the given rectangular area.
     *
     * @param x        world-space left edge
     * @param y        world-space top edge
     * @param w        width of the area
     * @param h        height of the area
     * @param maxItems maximum entities per leaf before subdivision
     * @param maxDepth maximum recursion depth (prevents unbounded memory growth)
     */
    public QuadTree(float x, float y, float w, float h, int maxItems, int maxDepth) {
        this(x, y, w, h, maxItems, maxDepth, 0);
    }

    private QuadTree(float x, float y, float w, float h,
                     int maxItems, int maxDepth, int depth) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.maxItems = maxItems;
        this.maxDepth = maxDepth;
        this.depth = depth;
    }

    /**
     * Inserts an entity into the tree. If the entity's AABB does not overlap this
     * node's area, the call is silently ignored.
     *
     * @param e the entity to insert
     */
    public void insert(Entity<?> e) {
        if (!intersects(e.x, e.y, e.width, e.height)) return;

        if (children != null) {
            // Internal node — propagate to matching children
            for (QuadTree child : children) child.insert(e);
            return;
        }

        // Leaf node
        items.add(e);
        if (items.size() > maxItems && depth < maxDepth) {
            subdivide();
            // Re-insert existing items into children
            List<Entity<?>> overflow = new ArrayList<>(items);
            items.clear();
            for (Entity<?> item : overflow) {
                for (QuadTree child : children) child.insert(item);
            }
        }
    }

    /**
     * Returns all entities whose AABB overlaps the given query rectangle.
     * Entities spanning multiple quadrants appear only once in the result.
     *
     * @param qx query rectangle left edge (world space)
     * @param qy query rectangle top edge (world space)
     * @param qw query rectangle width
     * @param qh query rectangle height
     * @return deduplicated list of candidate entities
     */
    public List<Entity<?>> query(float qx, float qy, float qw, float qh) {
        Set<Entity<?>> result = new LinkedHashSet<>();
        queryInto(qx, qy, qw, qh, result);
        return new ArrayList<>(result);
    }

    /**
     * Clears all entities and collapses all child nodes, resetting the tree to a
     * single empty root leaf. Reuses this instance for the next frame.
     */
    public void clear() {
        items.clear();
        children = null;
    }

    /**
     * Snapshot of a single QuadTree node used for debug rendering.
     *
     * @param x      node's world-space left edge
     * @param y      node's world-space top edge
     * @param w      node's width
     * @param h      node's height
     * @param depth  node's depth in the tree (0 = root)
     * @param count  number of entities stored directly in this node (> 0 only on leaves)
     * @param isLeaf {@code true} when this node has no children
     */
    public record NodeInfo(float x, float y, float w, float h,
                            int depth, int count, boolean isLeaf) {}

    /**
     * Visits every node in the tree depth-first and calls {@code visitor} for each.
     * Both internal nodes and leaf nodes are visited.
     *
     * @param visitor callback receiving a {@link NodeInfo} for every visited node
     */
    public void visitNodes(Consumer<NodeInfo> visitor) {
        visitNodesInto(visitor);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void visitNodesInto(Consumer<NodeInfo> visitor) {
        visitor.accept(new NodeInfo(x, y, w, h, depth, items.size(), children == null));
        if (children != null) {
            for (QuadTree child : children) child.visitNodesInto(visitor);
        }
    }

    private void queryInto(float qx, float qy, float qw, float qh, Set<Entity<?>> out) {
        if (!intersects(qx, qy, qw, qh)) return;

        for (Entity<?> e : items) {
            if (e.x < qx + qw && e.x + e.width  > qx
             && e.y < qy + qh && e.y + e.height > qy) {
                out.add(e);
            }
        }

        if (children != null) {
            for (QuadTree child : children) child.queryInto(qx, qy, qw, qh, out);
        }
    }

    private void subdivide() {
        float hw = w / 2f;
        float hh = h / 2f;
        children = new QuadTree[] {
            new QuadTree(x,      y,      hw, hh, maxItems, maxDepth, depth + 1), // NW
            new QuadTree(x + hw, y,      hw, hh, maxItems, maxDepth, depth + 1), // NE
            new QuadTree(x,      y + hh, hw, hh, maxItems, maxDepth, depth + 1), // SW
            new QuadTree(x + hw, y + hh, hw, hh, maxItems, maxDepth, depth + 1)  // SE
        };
    }

    /**
     * Returns {@code true} if the given AABB overlaps this node's area.
     * Touching edges (zero-width/height overlap) count as intersecting.
     */
    private boolean intersects(float ox, float oy, float ow, float oh) {
        return ox < x + w && ox + ow > x
            && oy < y + h && oy + oh > y;
    }
}
