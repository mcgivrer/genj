package com.core.entity;

import java.util.ArrayList;
import java.util.List;

import com.core.physics.PhysicsType;

/**
 * An entity that continuously emits and manages a pool of {@link Particle}
 * instances.
 *
 * <h3>Design rationale</h3>
 * <ul>
 *   <li>Particles are <em>not</em> child {@code Entity} objects — they live in
 *       a flat {@link ArrayList} to avoid per-particle scene overhead.</li>
 *   <li>The emitter position ({@code x}, {@code y}) is the spawn origin.  Its
 *       {@code width} and {@code height} default to 0 so the entity has no
 *       AABB in the collision engine.</li>
 *   <li>{@code physicsType} defaults to {@link PhysicsType#STATIC}: the
 *       {@code PhysicsEngine} will call {@link #update(long)} each frame
 *       (triggering the attached {@link com.core.behavior.particle.ParticleEmitterBehavior})
 *       but will not apply gravity or collision to the emitter itself.</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * ParticleSystem rain = new ParticleSystem("rain")
 *     .setPosition(600, 0)
 *     .setMaxParticles(400)
 *     .addBehavior(new RainBehavior(world));
 * scene.add(rain);
 * }</pre>
 */
public class ParticleSystem extends Entity<ParticleSystem> {

    /**
     * Upper bound on simultaneously active particles.
     * New particles are suppressed when this limit is reached.
     */
    public int maxParticles = 200;

    /**
     * The live particle pool.  Dead particles ({@code alive == false}) are
     * recycled by the emitter behaviour instead of removed, avoiding
     * list-resizing allocations at steady state.
     */
    public final List<Particle> particles = new ArrayList<>();

    public ParticleSystem(String name) {
        super(name);
        // STATIC: PhysicsEngine calls update() but applies no gravity/containment.
        this.physicsType = PhysicsType.STATIC;
        // Zero bounding box → invisible to collision engine.
        this.width  = 0;
        this.height = 0;
    }

    /**
     * Sets the maximum number of simultaneous particles.
     *
     * @param max positive integer (clamped to at least 1)
     * @return {@code this} for fluent chaining
     */
    public ParticleSystem setMaxParticles(int max) {
        this.maxParticles = Math.max(1, max);
        return this;
    }

    /**
     * Returns the number of currently alive particles.
     */
    public int aliveCount() {
        int count = 0;
        for (Particle p : particles) {
            if (p.alive) count++;
        }
        return count;
    }

    /**
     * Kills all particles immediately (e.g., on scene reset or when the
     * emitter is disabled).
     */
    public void clearParticles() {
        particles.forEach(p -> p.alive = false);
    }
}
