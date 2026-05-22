package com.core.behavior.particle;

import java.awt.Color;

import com.core.entity.Particle;
import com.core.entity.ParticleSystem;
import com.core.entity.World;

/**
 * Simulates a water fountain (jet d'eau).
 *
 * <p>Characteristics:</p>
 * <ul>
 *   <li>Particles launched upward in a cone; full gravity pulls them back
 *       down, tracing a parabolic arc</li>
 *   <li>Cyan–blue colour that fades out as the particle falls</li>
 *   <li>Spread and speed control the "shape" of the jet</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * ParticleSystem fountain = new ParticleSystem("fountain")
 *     .setPosition(600, 500)
 *     .setMaxParticles(250)
 *     .addBehavior(new FountainBehavior(world));
 * }</pre>
 */
public class FountainBehavior extends ParticleEmitterBehavior {

    private static EmitterConfig defaultConfig(World world) {
        EmitterConfig c = new EmitterConfig();
        c.emitRate      = 80f;
        c.minLife       = 1.5f;
        c.maxLife       = 3.0f;
        c.direction     = (float) (-Math.PI / 2.0);  // upward (−Y)
        c.spread        = 0.35f;
        c.minSpeed      = 120f;
        c.maxSpeed      = 260f;
        c.minSize       = 3f;
        c.maxSize       = 7f;
        c.emitAreaW     = 8f;
        c.gravityFactor = 1.0f;                        // full gravity → arc
        c.fadeOut       = true;
        c.shrink        = false;
        c.shape         = EmitterConfig.ParticleShape.CIRCLE;
        c.startColor    = new Color(80, 200, 255, 230);
        c.endColor      = new Color(30, 100, 200, 0);
        if (world != null) {
            c.worldGravityX = world.gravityX;
            c.worldGravityY = world.gravityY;
        }
        return c;
    }

    public FountainBehavior() {
        super(defaultConfig(null));
    }

    public FountainBehavior(World world) {
        super(defaultConfig(world), world);
    }

    /**
     * Sets the jet height by tuning the speed range.
     *
     * @param minSpeed minimum particle speed (px/s)
     * @param maxSpeed maximum particle speed (px/s)
     * @return {@code this}
     */
    public FountainBehavior setJetSpeed(float minSpeed, float maxSpeed) {
        config.minSpeed = minSpeed;
        config.maxSpeed = maxSpeed;
        return this;
    }

    /**
     * Widens or narrows the fountain spread.
     *
     * @param halfAngle half-angle in radians
     * @return {@code this}
     */
    public FountainBehavior setSpread(float halfAngle) {
        config.spread = halfAngle;
        return this;
    }

    @Override
    protected void spawnParticle(ParticleSystem ps, Particle p) {
        float speed = nextFloat(config.minSpeed, config.maxSpeed);
        applyVelocityFromCone(p, speed);

        p.life        = nextFloat(config.minLife, config.maxLife);
        p.maxLife     = p.life;
        p.size        = nextFloat(config.minSize, config.maxSize);
        p.initialSize = p.size;
        p.alpha       = 0.9f;
        p.color       = config.startColor;
    }
}
