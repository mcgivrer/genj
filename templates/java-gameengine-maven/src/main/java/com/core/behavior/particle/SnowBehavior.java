package com.core.behavior.particle;

import java.awt.Color;

import com.core.entity.Particle;
import com.core.entity.ParticleSystem;
import com.core.entity.World;

/**
 * Simulates softly falling snow.
 *
 * <p>Characteristics:</p>
 * <ul>
 *   <li>Low gravity factor — particles drift lazily</li>
 *   <li>Sinusoidal horizontal oscillation per particle (wave phase stored in
 *       {@link Particle#rotation}, wave frequency in {@link Particle#angularVelocity})</li>
 *   <li>White opaque circles, wide spawn area, long lifetime</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * ParticleSystem snow = new ParticleSystem("snow")
 *     .setPosition(0, -20)
 *     .setMaxParticles(300)
 *     .addBehavior(new SnowBehavior(world).setWidth(1200));
 * }</pre>
 */
public class SnowBehavior extends ParticleEmitterBehavior {

    /** Horizontal oscillation amplitude in pixels/second. */
    private float waveAmplitude = 25f;

    private static EmitterConfig defaultConfig(World world) {
        EmitterConfig c = new EmitterConfig();
        c.emitRate      = 40f;
        c.minLife       = 6.0f;
        c.maxLife       = 12.0f;
        c.direction     = (float) (Math.PI / 2.0);   // downward
        c.spread        = 0.6f;                        // wide cone
        c.minSpeed      = 20f;
        c.maxSpeed      = 70f;
        c.minSize       = 2f;
        c.maxSize       = 7f;
        c.emitAreaW     = 800f;
        c.gravityFactor = 0.05f;                       // nearly weightless
        c.fadeOut       = true;
        c.shrink        = false;
        c.shape         = EmitterConfig.ParticleShape.CIRCLE;
        c.startColor    = new Color(240, 248, 255, 220);
        c.endColor      = new Color(220, 235, 255, 0);
        if (world != null) {
            c.worldGravityX = world.gravityX;
            c.worldGravityY = world.gravityY;
        }
        return c;
    }

    public SnowBehavior() {
        super(defaultConfig(null));
    }

    public SnowBehavior(World world) {
        super(defaultConfig(world), world);
    }

    public SnowBehavior setWidth(float width) {
        config.emitAreaW = width;
        return this;
    }

    /**
     * Sets the amplitude of the horizontal oscillation.
     *
     * @param amplitude max lateral drift in pixels/second
     * @return {@code this}
     */
    public SnowBehavior setWaveAmplitude(float amplitude) {
        this.waveAmplitude = amplitude;
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
        p.alpha            = 0.7f + rng.nextFloat() * 0.3f;
        p.color            = config.startColor;

        // Store random wave frequency in angularVelocity (rad/s),
        // and a random phase offset in data.
        p.angularVelocity  = nextFloat(0.5f, 2.0f);   // wave frequency
        p.data             = nextFloat(0f, (float) (2 * Math.PI)); // phase offset
        p.rotation         = p.data;                  // initialise phase accumulator
    }

    /**
     * Overrides horizontal velocity each frame to produce sinusoidal drift.
     * The wave phase is accumulated in {@link Particle#rotation}.
     */
    @Override
    protected void updateParticle(Particle p, float dt, float ratio) {
        // vx = waveAmplitude * sin(phase)  (rotation already advanced by base)
        p.vx = (float) Math.sin(p.rotation) * waveAmplitude;
    }
}
