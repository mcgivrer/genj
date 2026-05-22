package com.core.gfx.plugin;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Stroke;

import com.core.behavior.particle.EmitterConfig.ParticleShape;
import com.core.entity.Entity;
import com.core.entity.Particle;
import com.core.entity.ParticleSystem;

/**
 * {@link EntityRenderer} plugin that draws all alive {@link Particle}s owned
 * by a {@link ParticleSystem}.
 *
 * <p>Three visual modes are supported, driven by
 * {@link com.core.behavior.particle.EmitterConfig#shape}:</p>
 * <ul>
 *   <li>{@code CIRCLE} — filled anti-aliased circle (default)</li>
 *   <li>{@code LINE}   — thin vertical stroke (rain streaks)</li>
 *   <li>{@code SQUARE} — filled square (debris, sparks)</li>
 * </ul>
 *
 * <p>Register this plugin once per scene via
 * {@link com.core.gfx.Renderer#registerPlugin(EntityRenderer)}:</p>
 * <pre>{@code
 * app.getRenderer().registerPlugin(new ParticleSystemRenderer());
 * }</pre>
 */
public class ParticleSystemRenderer implements EntityRenderer<ParticleSystem> {

    /** Reusable stroke for LINE-shaped particles (1 px width). */
    private static final BasicStroke RAIN_STROKE = new BasicStroke(1.2f);

    @Override
    public boolean supports(Entity<?> entity) {
        return entity instanceof ParticleSystem;
    }

    @Override
    public void render(Graphics2D g, ParticleSystem ps) {
        if (ps.particles.isEmpty()) return;

        Composite savedComposite = g.getComposite();
        Stroke savedStroke       = g.getStroke();

        for (Particle p : ps.particles) {
            if (!p.alive || p.color == null) continue;

            // Apply alpha
            float alpha = Math.min(1f, Math.max(0f, p.alpha));
            if (alpha < 0.01f) continue;

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(p.color);

            int px = Math.round(p.x);
            int py = Math.round(p.y);
            int sz = Math.max(1, Math.round(p.size));

            // Determine shape from the config of the first behavior that is a
            // ParticleEmitterBehavior (if any); default to CIRCLE otherwise.
            ParticleShape shape = resolveShape(ps);

            switch (shape) {
                case LINE -> {
                    g.setStroke(RAIN_STROKE);
                    // Rain streak: a line from the particle position downward
                    // proportional to its size (length ≈ 3× diameter).
                    int lineLen = sz * 3;
                    g.drawLine(px, py, px, py + lineLen);
                }
                case SQUARE -> g.fillRect(px - sz / 2, py - sz / 2, sz, sz);
                default    -> g.fillOval(px - sz / 2, py - sz / 2, sz, sz);
            }
        }

        // Restore graphics state
        g.setComposite(savedComposite);
        g.setStroke(savedStroke);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Resolves the particle shape by inspecting the first
     * {@code ParticleEmitterBehavior} attached to the system.
     */
    private static ParticleShape resolveShape(ParticleSystem ps) {
        for (var b : ps.behaviors) {
            if (b instanceof com.core.behavior.particle.ParticleEmitterBehavior peb) {
                return peb.getConfig().shape;
            }
        }
        return ParticleShape.CIRCLE;
    }
}
