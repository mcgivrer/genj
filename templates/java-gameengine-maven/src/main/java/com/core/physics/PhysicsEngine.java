package com.core.physics;

import java.util.List;

import com.core.entity.Entity;
import com.core.entity.GameObject;
import com.core.entity.Nature;
import com.core.entity.World;

public class PhysicsEngine {
    private final CollisionEngine collisionEngine;

    public PhysicsEngine() {
        this.collisionEngine = new CollisionEngine();
    }

    public void update(World world, List<Entity<?>> entities, long elapsed) {
        float dt = elapsed / 1000f;

        // Step 1: carry DYNAMIC entities riding a moving platform (ridingPlatform from last frame)
        for (Entity<?> e : entities) {
            if (e.active && e.ridingPlatform != null && e.physicsType == PhysicsType.DYNAMIC) {
                e.x += e.ridingPlatform.vx * dt;
                // Carry upward; downward separation is handled naturally by gravity + collision
                if (e.ridingPlatform.vy < 0) {
                    e.y += e.ridingPlatform.vy * dt;
                }
            }
        }

        // Step 2: reset per-frame contact flags
        entities.forEach(e -> {
            e.onGround = false;
            e.ridingPlatform = null;
            e.collisionTop    = false;
            e.collisionBottom = false;
            e.collisionLeft   = false;
            e.collisionRight  = false;
        });

        // Step 3: physics pass
        for (Entity<?> e : entities) {
            if (!e.active || e.physicsType == PhysicsType.NONE) {
                continue;
            }
            if (e.physicsType == PhysicsType.DYNAMIC) {
                e.vx += world.gravityX * dt;
                e.vy += world.gravityY * dt;

                float damping = Math.max(0.0f, 1.0f - e.material.friction * 0.02f);
                e.vx *= damping;
                e.vy *= damping;

                // Angular damping
                float angDamping = Math.max(0.0f, 1.0f - e.material.rotationalFriction * 0.02f);
                e.angularVelocity *= angDamping;
            }
            e.update(elapsed);
            containInWorld(world, e);
        }

        collisionEngine.resolve(entities);

        // Post-collision pass: enforce RECTANGLE tipping threshold
        for (Entity<?> e : entities) {
            if (e.active && e.physicsType == PhysicsType.DYNAMIC
                    && e instanceof GameObject go && go.nature == Nature.RECTANGLE
                    && e.width > 0 && e.height > 0) {
                float speed = (float) Math.sqrt(e.vx * e.vx + e.vy * e.vy);
                float vTip  = Math.abs(world.gravityY) * e.width / (float) e.height;
                if (speed <= vTip) {
                    e.angularVelocity = 0f;
                }
            }
        }
    }

    private void containInWorld(World world, Entity<?> e) {
        if (e.physicsType != PhysicsType.DYNAMIC) {
            return;
        }

        float restitution = (e.material != null) ? e.material.elasticity : 0.3f;
        float friction    = (e.material != null) ? e.material.friction    : 0.2f;

        float preVx = e.vx, preVy = e.vy;

        if (e.x < world.minX()) {
            e.x  = world.minX();
            e.vx = Math.abs(preVx) * restitution;
            e.vy *= (1.0f - friction * 0.1f);
            applyBoundaryAngularImpulse(e, preVy, friction, true);
        } else if (e.x + e.width > world.maxX()) {
            e.x  = world.maxX() - e.width;
            e.vx = -Math.abs(preVx) * restitution;
            e.vy *= (1.0f - friction * 0.1f);
            applyBoundaryAngularImpulse(e, preVy, friction, true);
        }

        if (e.y < world.minY()) {
            e.y  = world.minY();
            e.vy = Math.abs(preVy) * restitution;
            e.vx *= (1.0f - friction * 0.1f);
            applyBoundaryAngularImpulse(e, preVx, friction, false);
        } else if (e.y + e.height > world.maxY()) {
            e.y  = world.maxY() - e.height;
            e.vy = -Math.abs(preVy) * restitution;
            e.vx *= (1.0f - friction * 0.1f);
            applyBoundaryAngularImpulse(e, preVx, friction, false);
        }
    }

    /**
     * Computes and adds an angular impulse when an entity bounces off a world boundary.
     *
     * @param e          the bouncing entity
     * @param tangentVel the pre-bounce velocity component tangent to the boundary
     *                   (vy for a vertical wall, vx for a horizontal floor/ceiling)
     * @param friction   surface friction coefficient
     * @param vertWall   {@code true} when hitting a vertical wall (X bounce),
     *                   {@code false} when hitting a horizontal surface (Y bounce)
     */
    private void applyBoundaryAngularImpulse(Entity<?> e, float tangentVel,
                                              float friction, boolean vertWall) {
        // Moment of inertia for a rectangular body: I = m*(w²+h²)/12
        float I = e.mass * (e.width * e.width + e.height * e.height) / 12f;
        if (I < 0.001f) return;

        // Lever arm: half-width for horizontal boundary, half-height for vertical wall
        float lever = vertWall ? e.width * 0.5f : e.height * 0.5f;
        e.angularVelocity += tangentVel * lever * friction / I;
    }
}
