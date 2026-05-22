package com.core.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.core.App;
import com.core.entity.Entity;
import com.core.entity.World;
import com.core.gfx.Camera;
import com.core.physics.PhysicsType;
import com.core.spatial.QuadTree;
import com.core.spatial.QuadTreeDebugBehavior;

public class BaseScene implements Scene {

    private List<Entity<?>> entities = new CopyOnWriteArrayList<>();
    private Map<String, Entity<?>> entitiesMap = new ConcurrentHashMap<>();
    private List<Camera> cameras = new CopyOnWriteArrayList<>();
    protected App app;
    protected World world;

    /** Spatial index rebuilt every frame for frustum-culling queries. */
    private QuadTree spatialTree = null;

    protected BaseScene(App app) {
        this.app = app;
    }

    @Override
    public void create(App app) {
    }

    @Override
    public void update(App app, Map<String, Object> stats, float deltaTime) {
    }

    public void add(Entity<?> e) {
        if (!entities.contains(e)) {
            entities.add(e);
            entitiesMap.putIfAbsent(e.name, e);
        }
    }

    public void addCamera(Camera camera) {
        cameras.add(camera);
    }

    @Override
    public List<Camera> getCameras() {
        return cameras;
    }

    @Override
    public Entity<?> getEntity(String name) {
        return entitiesMap.get(name);
    }

    @Override
    public World getWorld() {
        return world;
    }

    /**
     * Sets the scene's World, registers it in the entity list so that it
     * participates in the render pipeline, and attaches the
     * {@link QuadTreeDebugBehavior} overlay (active when {@code debug > 3}).
     *
     * <p>Call this instead of assigning {@code this.world} directly in
     * {@link #create(App)}.
     *
     * @param w the world to register; must not be {@code null}
     */
    public void setWorld(World w) {
        this.world = w;
        add(w);
        w.addBehavior(new QuadTreeDebugBehavior(this::getQuadTree));
    }

    @Override
    public List<Entity<?>> getEntities() {
        return entities;
    }

    /**
     * Rebuilds the QuadTree from all currently active entities.
     * The tree bounds are taken from the scene's World; if no World is set the
     * call is a no-op.
     */
    @Override
    public void rebuildSpatialIndex() {
        World w = getWorld();
        if (w == null) return;
        if (spatialTree == null) {
            spatialTree = new QuadTree(w.x, w.y, w.width, w.height, 4, 8);
        } else {
            spatialTree.clear();
        }
        for (Entity<?> e : entities) {
            // Skip NONE entities (e.g. World): their bounds span the full world area
            // and would pollute every spatial query.
            if (e.active && e.physicsType != PhysicsType.NONE) spatialTree.insert(e);
        }
    }

    /**
     * Returns entities visible within the given world-space query rectangle.
     * Falls back to the full entity list if the spatial index has not been built yet.
     */
    @Override
    public List<Entity<?>> getVisibleEntities(float wx, float wy, float ww, float wh) {
        if (spatialTree == null) return entities;
        List<Entity<?>> result = new ArrayList<>(spatialTree.query(wx, wy, ww, wh));
        // Zero-AABB entities (e.g. ParticleSystem) are never inserted into the QuadTree;
        // always include them so they are not culled by the frustum.
        for (Entity<?> e : entities) {
            if (e.active && e.width == 0 && e.height == 0) {
                result.add(e);
            }
        }
        return result;
    }

    @Override
    public QuadTree getQuadTree() {
        return spatialTree;
    }

}
