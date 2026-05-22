package com.core.gfx.plugin;

import java.awt.Graphics2D;

import com.core.entity.Entity;

/**
 * Strategy interface for drawing a single {@link Entity} onto a {@link Graphics2D} context.
 *
 * <p>Implementations are registered with {@link com.core.gfx.Renderer} via
 * {@link com.core.gfx.Renderer#registerPlugin(EntityRenderer)}.  The renderer
 * walks the plugin list in order and delegates to the first plugin whose
 * {@link #supports(Entity)} method returns {@code true}.</p>
 *
 * @param <T> the concrete entity type this plugin handles
 */
public interface EntityRenderer<T extends Entity<?>> {

    /**
     * Returns {@code true} if this plugin knows how to render the given entity.
     *
     * @param entity the entity about to be drawn
     * @return {@code true} if this plugin should handle the rendering
     */
    boolean supports(Entity<?> entity);

    /**
     * Draws {@code entity} into the graphics context.
     * The transform is already set to world space by the {@link com.core.gfx.Renderer}.
     *
     * @param g      the graphics context (world space)
     * @param entity the entity to draw
     */
    void render(Graphics2D g, T entity);
}
