package com.core.entity;

import java.awt.Graphics2D;

import com.core.physics.PhysicsType;

public class World extends Entity<World> {
    /** Pixels per metre — used to convert m/s² gravity values to px/s². */
    public static final float PPM = 50f;

    public float gravityX = 0.0f;
    public float gravityY = 200.0f;

    public World(String name) {
        super(name);
        // NONE: world bounds are enforced by PhysicsEngine.containInWorld() — not by
        // AABB collision. Using STATIC would incorrectly make CollisionEngine resolve
        // overlaps between every entity and the world rectangle.
        this.physicsType = PhysicsType.NONE;
        // Drawn before all game objects so that any World behaviors (e.g. debug overlays)
        // appear behind entities.
        this.renderPriority = -100;
    }

    /** Set gravity directly in pixels/s² (legacy / low-level). */
    public World setGravity(float gx, float gy) {
        this.gravityX = gx;
        this.gravityY = gy;
        return this;
    }

    /**
     * Set gravity expressed in m/s² — converted to px/s² using {@link #PPM}.
     * Earth gravity is approximately 9.81 m/s².
     *
     * @param gxMs2 horizontal gravity (m/s²)
     * @param gyMs2 vertical gravity (m/s²), positive = downward
     */
    public World setGravityMs2(float gxMs2, float gyMs2) {
        return setGravity(gxMs2 * PPM, gyMs2 * PPM);
    }

    public float minX() {
        return x;
    }

    public float minY() {
        return y;
    }

    public float maxX() {
        return x + width;
    }

    public float maxY() {
        return y + height;
    }

    /**
     * World has no visual representation by default; its area can be painted by
     * attaching {@link com.core.behavior.Behavior behaviors} to it.
     */
    @Override
    public void draw(Graphics2D g) {
        // intentionally empty
    }

}
