package com.core.scene;

import java.util.List;
import java.util.Map;

import com.core.App;
import com.core.entity.Entity;
import com.core.entity.World;
import com.core.gfx.Camera;
import com.core.spatial.QuadTree;

public interface Scene {
    default void load(App app) {
    }

    void create(App app);

    default void unload(App app) {
    }

    void update(App app, Map<String, Object> stats, float deltaTime);

    World getWorld();

    List<Entity<?>> getEntities();

    Entity<?> getEntity(String string);

    /**
     * Returns the list of cameras registered in this scene.
     * The default implementation returns an empty list for backward compatibility.
     */
    default List<Camera> getCameras() {
        return List.of();
    }

    /**
     * Returns the entities whose AABB overlaps the given world-space rectangle.
     * The default implementation returns all entities (no spatial filtering).
     *
     * @param wx world-space left edge of the query rectangle
     * @param wy world-space top edge of the query rectangle
     * @param ww width of the query rectangle
     * @param wh height of the query rectangle
     * @return filtered entity list
     */
    default List<Entity<?>> getVisibleEntities(float wx, float wy, float ww, float wh) {
        return getEntities();
    }

    /**
     * Rebuilds the spatial index from the current entity positions.
     * Called once per frame after physics, before rendering.
     * The default implementation is a no-op.
     */
    default void rebuildSpatialIndex() {
    }

    /**
     * Returns the live {@link QuadTree} spatial index, or {@code null} if none
     * has been built yet (e.g. scenes that do not override
     * {@link #rebuildSpatialIndex()}).
     */
    default QuadTree getQuadTree() {
        return null;
    }
}
