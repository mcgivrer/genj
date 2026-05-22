package com.core.physics;

import java.util.List;

import com.core.entity.Entity;

public class CollisionEngine {
    public void resolve(List<Entity<?>> entities) {
        for (int i = 0; i < entities.size(); i++) {
            Entity<?> a = entities.get(i);
            if (!a.active || a.physicsType == PhysicsType.NONE) {
                continue;
            }
            for (int j = i + 1; j < entities.size(); j++) {
                Entity<?> b = entities.get(j);
                if (!b.active || b.physicsType == PhysicsType.NONE) {
                    continue;
                }
                if (!a.isIntersect(b)) {
                    continue;
                }
                resolvePair(a, b);
            }
        }
    }

    private void resolvePair(Entity<?> a, Entity<?> b) {
        float overlapX = Math.min(a.x + a.width, b.x + b.width) - Math.max(a.x, b.x);
        float overlapY = Math.min(a.y + a.height, b.y + b.height) - Math.max(a.y, b.y);
        if (overlapX <= 0 || overlapY <= 0) {
            return;
        }

        boolean separateX = overlapX < overlapY;
        // coefficient de restitution moyen
        float e = (a.material.elasticity + b.material.elasticity) * 0.5f;
        float avgFriction = (a.material.friction + b.material.friction) * 0.5f;
        float ma = a.mass;
        float mb = b.mass;
        float totalMass = ma + mb;

        // Save pre-collision velocities for angular impulse computation
        float aVxPre = a.vx, aVyPre = a.vy;
        float bVxPre = b.vx, bVyPre = b.vy;

        if (separateX) {
            if (a.physicsType == PhysicsType.DYNAMIC && b.physicsType == PhysicsType.DYNAMIC) {
                // séparation positionnelle pondérée par la masse inverse
                float pushA = overlapX * (mb / totalMass);
                float pushB = overlapX * (ma / totalMass);
                if (a.x < b.x) { a.x -= pushA; b.x += pushB; a.collisionRight = true; b.collisionLeft  = true; }
                else            { a.x += pushA; b.x -= pushB; a.collisionLeft  = true; b.collisionRight = true; }
                // formule de collision élastique 1D avec restitution
                float va = a.vx, vb = b.vx;
                a.vx = ((ma - e * mb) * va + (1 + e) * mb * vb) / totalMass;
                b.vx = ((mb - e * ma) * vb + (1 + e) * ma * va) / totalMass;
            } else if (a.physicsType == PhysicsType.DYNAMIC && b.physicsType == PhysicsType.STATIC) {
                if (a.x < b.x) { a.x -= overlapX; a.collisionRight = true; }
                else            { a.x += overlapX; a.collisionLeft  = true; }
                a.vx = -a.vx * e;
            } else if (a.physicsType == PhysicsType.STATIC && b.physicsType == PhysicsType.DYNAMIC) {
                if (b.x < a.x) { b.x -= overlapX; b.collisionRight = true; }
                else            { b.x += overlapX; b.collisionLeft  = true; }
                b.vx = -b.vx * e;
            }
            if (a.physicsType == PhysicsType.DYNAMIC) a.vy *= (1.0f - avgFriction * 0.15f);
            if (b.physicsType == PhysicsType.DYNAMIC) b.vy *= (1.0f - avgFriction * 0.15f);
        } else {
            if (a.physicsType == PhysicsType.DYNAMIC && b.physicsType == PhysicsType.DYNAMIC) {
                float pushA = overlapY * (mb / totalMass);
                float pushB = overlapY * (ma / totalMass);
                if (a.y < b.y) { a.y -= pushA; b.y += pushB; a.onGround = true; a.collisionBottom = true; b.collisionTop    = true; }
                else            { a.y += pushA; b.y -= pushB; b.onGround = true; a.collisionTop    = true; b.collisionBottom = true; }
                float va = a.vy, vb = b.vy;
                a.vy = ((ma - e * mb) * va + (1 + e) * mb * vb) / totalMass;
                b.vy = ((mb - e * ma) * vb + (1 + e) * ma * va) / totalMass;
            } else if (a.physicsType == PhysicsType.DYNAMIC && b.physicsType == PhysicsType.STATIC) {
                if (a.y < b.y) { a.y -= overlapY; a.onGround = true; a.ridingPlatform = b; a.collisionBottom = true; }
                else            { a.y += overlapY;                                            a.collisionTop    = true; }
                a.vy = -a.vy * e;
            } else if (a.physicsType == PhysicsType.STATIC && b.physicsType == PhysicsType.DYNAMIC) {
                if (b.y < a.y) { b.y -= overlapY; b.onGround = true; b.ridingPlatform = a; b.collisionBottom = true; }
                else            { b.y += overlapY;                                            b.collisionTop    = true; }
                b.vy = -b.vy * e;
            }
            if (a.physicsType == PhysicsType.DYNAMIC) a.vx *= (1.0f - avgFriction * 0.15f);
            if (b.physicsType == PhysicsType.DYNAMIC) b.vx *= (1.0f - avgFriction * 0.15f);
        }

        // Apply angular impulses from the collision
        applyCollisionAngularImpulse(a, aVxPre, aVyPre, separateX, avgFriction);
        applyCollisionAngularImpulse(b, bVxPre, bVyPre, separateX, avgFriction);
    }

    /**
     * Adds an angular impulse to a DYNAMIC entity resulting from a collision.
     * <p>
     * The tangential velocity component at the contact edge creates a torque:
     * <ul>
     *   <li>X-axis separation (vertical wall contact): lever arm = half-height,
     *       tangential component = pre-collision vy</li>
     *   <li>Y-axis separation (horizontal surface contact): lever arm = half-width,
     *       tangential component = pre-collision vx</li>
     * </ul>
     *
     * @param e          the entity to spin
     * @param preVx      pre-collision X velocity
     * @param preVy      pre-collision Y velocity
     * @param separateX  {@code true} when the separation axis is X (vertical wall)
     * @param friction   average friction coefficient for the contact pair
     */
    private void applyCollisionAngularImpulse(Entity<?> e, float preVx, float preVy,
                                               boolean separateX, float friction) {
        if (e.physicsType != PhysicsType.DYNAMIC) return;
        // Moment of inertia: I = m*(w²+h²)/12
        float I = e.mass * (e.width * e.width + e.height * e.height) / 12f;
        if (I < 0.001f) return;

        float lever, tangentVel;
        if (separateX) {
            // Vertical wall contact: the tangential velocity is vy
            lever      = e.height * 0.5f;
            tangentVel = preVy;
        } else {
            // Horizontal surface contact: the tangential velocity is vx
            lever      = e.width * 0.5f;
            tangentVel = preVx;
        }
        e.angularVelocity += tangentVel * lever * friction / I;
    }
}
