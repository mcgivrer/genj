package com.core.behavior.particle;

import java.awt.Color;

import com.core.entity.Particle;
import com.core.entity.ParticleSystem;
import com.core.entity.World;

/**
 * Simulates a flickering torch or campfire.
 *
 * <p>Characteristics:</p>
 * <ul>
 *   <li>Particles rise upward with slight horizontal jitter</li>
 *   <li>Negative {@code gravityFactor} gives buoyancy (particles accelerate
 *       upward as hot gas would)</li>
 *   <li>Colour interpolates: bright yellow-orange → deep red → transparent smoke</li>
 *   <li>Particles shrink as they age, mimicking ember fade-out</li>
 *   <li>Slight random rotational velocity creates subtle visual variety</li>
 *   <li>A secondary "flicker" modulates the emit rate each frame to produce
 *       natural variation in brightness</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * ParticleSystem torch = new ParticleSystem("torch")
 *     .setPosition(300, 480)
 *     .setMaxParticles(120)
 *     .addBehavior(new TorchBehavior());
 * }</pre>
 */
public class TorchBehavior extends ParticleEmitterBehavior {

    /** Accumulated time for flicker cycle (seconds). */
    private float flickerTime = 0f;

    /** Base emit rate before flicker modulation. */
    private final float baseEmitRate;

    private static EmitterConfig defaultConfig() {
        EmitterConfig c = new EmitterConfig();
        c.emitRate      = 55f;
        c.minLife       = 0.4f;
        c.maxLife       = 1.2f;
        c.direction     = (float) (-Math.PI / 2.0);  // upward
        c.spread        = 0.5f;                        // wide flickering cone
        c.minSpeed      = 30f;
        c.maxSpeed      = 90f;
        c.minSize       = 5f;
        c.maxSize       = 14f;
        // Buoyancy: counteracts part of gravity so particles drift upward
        c.gravityFactor = -0.25f;
        c.fadeOut       = true;
        c.shrink        = true;
        c.shape         = EmitterConfig.ParticleShape.CIRCLE;
        // Bright yellow-orange at spawn, deep red at end
        c.startColor    = new Color(255, 200, 30);
        c.endColor      = new Color(120, 10, 0);
        // No world gravity needed for torch (free-standing effect)
        c.worldGravityY = 0f;
        return c;
    }

    public TorchBehavior() {
        super(defaultConfig());
        this.baseEmitRate = config.emitRate;
    }

    /** Creates a torch with an explicit world (uses world gravity for smoke drift). */
    public TorchBehavior(World world) {
        super(defaultConfig(), world);
        this.baseEmitRate = config.emitRate;
        // Reduced gravity factor keeps the flame mostly buoyant even with world gravity
        config.gravityFactor = -0.15f;
    }

    /**
     * Sets the intensity (emit rate) of the flame.
     *
     * @param rate particles per second
     * @return {@code this}
     */
    public TorchBehavior setIntensity(float rate) {
        config.emitRate = rate;
        return this;
    }

    @Override
    protected void spawnParticle(ParticleSystem ps, Particle p) {
        float speed = nextFloat(config.minSpeed, config.maxSpeed);
        applyVelocityFromCone(p, speed);

        p.life             = nextFloat(config.minLife, config.maxLife);
        p.maxLife          = p.life;
        p.size             = nextFloat(config.minSize, config.maxSize);
        p.initialSize      = p.size;
        p.alpha            = 0.85f + rng.nextFloat() * 0.15f;
        p.color            = config.startColor;
        // Small random spin for visual variety
        p.angularVelocity  = nextFloat(-1.5f, 1.5f);
    }

    @Override
    protected void updateParticle(Particle p, float dt, float ratio) {
        // Nothing extra per particle for torch.
    }

    /** Modulates emit rate to produce a natural flicker before delegating. */
    @Override
    protected void onPreUpdate(float dt) {
        flickerTime += dt;
        float flicker = 1f + 0.35f * (float) Math.sin(flickerTime * 12f)
                           + 0.15f * (float) Math.sin(flickerTime * 27f);
        config.emitRate = Math.max(0f, baseEmitRate * flicker);
    }
}
