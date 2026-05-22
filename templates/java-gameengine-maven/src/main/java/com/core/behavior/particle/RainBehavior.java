package com.core.behavior.particle;

import java.awt.Color;

import com.core.entity.Particle;
import com.core.entity.ParticleSystem;
import com.core.entity.World;

/**
 * Simulates falling rain.
 *
 * <p>Characteristics:</p>
 * <ul>
 *   <li>High emit rate, vertical streaks</li>
 *   <li>Particles spawn across the full emitter width ({@code emitAreaW})</li>
 *   <li>Blue–grey colour, short lifetime, rendered as thin vertical lines</li>
 *   <li>Wind offset ({@code windX}) produces angled rain</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * ParticleSystem rain = new ParticleSystem("rain")
 *     .setPosition(0, -10)
 *     .setMaxParticles(600)
 *     .addBehavior(new RainBehavior(world).setWidth(1200));
 * }</pre>
 */
public class RainBehavior extends ParticleEmitterBehavior {

    private static EmitterConfig defaultConfig(World world) {
        EmitterConfig c = new EmitterConfig();
        c.emitRate      = 150f;
        c.minLife       = 1.0f;
        c.maxLife       = 3.0f;
        c.direction     = (float) (Math.PI / 2.0);   // downward
        c.spread        = 0.05f;                       // nearly vertical
        c.minSpeed      = 250f;
        c.maxSpeed      = 450f;
        c.minSize       = 1f;
        c.maxSize       = 3f;
        c.emitAreaW     = 800f;
        c.emitAreaH     = 0f;
        c.gravityFactor = 0.8f;
        c.fadeOut       = true;
        c.shrink        = false;
        c.shape         = EmitterConfig.ParticleShape.LINE;
        c.startColor    = new Color(160, 200, 255, 200);
        c.endColor      = new Color(100, 160, 220, 0);
        if (world != null) {
            c.worldGravityX = world.gravityX;
            c.worldGravityY = world.gravityY;
        }
        return c;
    }

    public RainBehavior() {
        super(defaultConfig(null));
        setSplashEnabled(true);
    }

    public RainBehavior(World world) {
        super(defaultConfig(world), world);
        setSplashEnabled(true);
    }

    /**
     * Adjusts the horizontal spawn area width so rain covers the whole scene.
     *
     * @param width scene width in pixels
     * @return {@code this}
     */
    public RainBehavior setWidth(float width) {
        config.emitAreaW = width;
        return this;
    }

    /**
     * Sets a horizontal wind component in pixels/s².
     *
     * @param wx positive = rightward; negative = leftward
     * @return {@code this}
     */
    public RainBehavior setWind(float wx) {
        config.windX = wx;
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
        p.alpha       = 0.8f + rng.nextFloat() * 0.2f;
        p.color       = config.startColor;
    }
}
