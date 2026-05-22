package com.core.behavior.particle;

import java.awt.Color;

/**
 * Immutable-by-convention configuration record for a particle emitter.
 * <p>
 * All fields are {@code public} for consistency with the engine's direct-field
 * access convention.  Behaviours read these values each frame; the Level Editor
 * writes them through the {@code @BehaviorParam}-annotated constructor.
 * </p>
 *
 * <h3>Coordinate conventions</h3>
 * <ul>
 *   <li>Direction 0 = rightward (+X)</li>
 *   <li>Direction PI/2 ≈ 1.5708 = downward (+Y, screen space)</li>
 *   <li>Direction −PI/2 ≈ −1.5708 = upward (−Y)</li>
 *   <li>Direction PI = leftward</li>
 * </ul>
 */
public class EmitterConfig {

    // ─── Emission ────────────────────────────────────────────────────────────

    /** Number of particles emitted per second. */
    public float emitRate = 60f;

    // ─── Lifetime ─────────────────────────────────────────────────────────────

    /** Minimum particle lifetime in seconds. */
    public float minLife = 1.0f;

    /** Maximum particle lifetime in seconds. */
    public float maxLife = 2.5f;

    // ─── Direction and spread ────────────────────────────────────────────────

    /**
     * Emission direction in radians.
     * Default: PI/2 = downward (suitable for rain).
     */
    public float direction = (float) (Math.PI / 2.0);

    /**
     * Half-angle of the emission cone in radians.
     * 0 = perfectly collimated; PI = omnidirectional.
     */
    public float spread = 0.3f;

    // ─── Speed ────────────────────────────────────────────────────────────────

    /** Minimum initial particle speed in pixels/second. */
    public float minSpeed = 80f;

    /** Maximum initial particle speed in pixels/second. */
    public float maxSpeed = 150f;

    // ─── Size ─────────────────────────────────────────────────────────────────

    /** Minimum initial particle diameter in pixels. */
    public float minSize = 2f;

    /** Maximum initial particle diameter in pixels. */
    public float maxSize = 6f;

    // ─── Emitter area ─────────────────────────────────────────────────────────

    /**
     * Width of the spawn area in pixels.
     * {@code 0} = point emitter.  Particles spawn at a uniformly random
     * X offset in {@code [−emitAreaW/2, +emitAreaW/2]} relative to the entity.
     */
    public float emitAreaW = 0f;

    /**
     * Height of the spawn area in pixels.
     * {@code 0} = point emitter.
     */
    public float emitAreaH = 0f;

    // ─── Forces ───────────────────────────────────────────────────────────────

    /** Constant horizontal wind acceleration (pixels/s²). */
    public float windX = 0f;

    /** Constant vertical wind acceleration (pixels/s²). */
    public float windY = 0f;

    /**
     * Fraction of world gravity (from {@link com.core.entity.World}) applied
     * to particles.  {@code 1.0} = full gravity; {@code 0.0} = weightless;
     * negative values give buoyancy (e.g., fire/smoke).
     */
    public float gravityFactor = 1.0f;

    /**
     * World gravity X component (pixels/s²).
     * Set automatically by {@link com.core.behavior.particle.ParticleEmitterBehavior}
     * from the scene's {@link com.core.entity.World}.
     */
    public float worldGravityX = 0f;

    /**
     * World gravity Y component (pixels/s²).
     * Set automatically by {@link com.core.behavior.particle.ParticleEmitterBehavior}.
     */
    public float worldGravityY = 980f;

    // ─── Colour ───────────────────────────────────────────────────────────────

    /** Colour at spawn (t = maxLife). */
    public Color startColor = Color.WHITE;

    /**
     * Colour at end-of-life (t = 0).
     * Behaviours interpolate between {@code startColor} and {@code endColor}
     * as the particle ages.
     */
    public Color endColor = new Color(255, 255, 255, 0);

    // ─── Visual options ───────────────────────────────────────────────────────

    /**
     * When {@code true} the particle alpha fades from 1 to 0 as life decays.
     */
    public boolean fadeOut = true;

    /**
     * When {@code true} the particle diameter shrinks from {@code initialSize}
     * to 0 as life decays.
     */
    public boolean shrink = false;

    /**
     * Visual shape of each particle.
     */
    public ParticleShape shape = ParticleShape.CIRCLE;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Supported primitive shapes for particle rendering. */
    public enum ParticleShape {
        /** Filled circle (default, suitable for most effects). */
        CIRCLE,
        /** Thin vertical line (elongated raindrop look). */
        LINE,
        /** Filled square (debris, sparks). */
        SQUARE
    }

    /**
     * Linearly interpolates between {@code startColor} and {@code endColor}.
     *
     * @param ratio age ratio: 1 = just spawned, 0 = about to die
     * @return interpolated colour (RGB only; alpha managed separately via {@code fadeOut})
     */
    public Color interpolateColor(float ratio) {
        if (startColor == null) return null;
        if (endColor   == null) return startColor;
        float r = startColor.getRed()   + (endColor.getRed()   - startColor.getRed())   * (1f - ratio);
        float g = startColor.getGreen() + (endColor.getGreen() - startColor.getGreen()) * (1f - ratio);
        float b = startColor.getBlue()  + (endColor.getBlue()  - startColor.getBlue())  * (1f - ratio);
        return new Color(
                Math.min(255, Math.max(0, (int) r)),
                Math.min(255, Math.max(0, (int) g)),
                Math.min(255, Math.max(0, (int) b)));
    }
}
