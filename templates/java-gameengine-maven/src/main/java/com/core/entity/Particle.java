package com.core.entity;

import java.awt.Color;

/**
 * A single lightweight particle managed by a {@link ParticleSystem}.
 * <p>
 * Particles are <strong>not</strong> {@link Entity} instances — they are plain
 * data objects allocated in a fixed pool inside {@code ParticleSystem} and
 * recycled when their lifetime expires.  This keeps GC pressure low even at
 * several hundred particles per frame.
 * </p>
 */
public class Particle {

    /** World-space position. */
    public float x, y;

    /** Velocity in pixels/second. */
    public float vx, vy;

    /** Remaining life in seconds.  Counts down to 0. */
    public float life;

    /** Initial life (used to compute the 0..1 age ratio for fading/sizing). */
    public float maxLife;

    /** Current rendered diameter in pixels. */
    public float size;

    /** Initial size, captured at spawn (for shrink interpolation). */
    public float initialSize;

    /**
     * Rotation angle in radians.
     * Also repurposed by {@code SnowBehavior} as a wave-phase accumulator.
     */
    public float rotation;

    /**
     * Angular velocity (rad/s), or wave frequency (rad/s) for snow particles.
     */
    public float angularVelocity;

    /** Alpha channel 0..1 applied at render time. */
    public float alpha = 1f;

    /** Current draw colour (may be interpolated between start and end colours). */
    public Color color;

    /**
     * General-purpose per-particle data slot used by behaviours that need
     * to store one extra float per particle without allocating extra objects.
     * (e.g., individual randomised wind offset for snow).
     */
    public float data;

    /** {@code false} means the slot is available for reuse. */
    public boolean alive = false;

    /** Resets all fields so the slot can be reused for a new particle. */
    public void reset() {
        x = 0; y = 0;
        vx = 0; vy = 0;
        life = 0; maxLife = 1;
        size = 1; initialSize = 1;
        rotation = 0; angularVelocity = 0;
        alpha = 1f;
        color = null;
        data = 0f;
        alive = false;
    }
}
