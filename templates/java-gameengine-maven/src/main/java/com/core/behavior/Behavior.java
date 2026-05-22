package com.core.behavior;

import java.awt.Graphics2D;

import com.core.entity.Entity;

/**
 * A Behavior encapsulates a single autonomous update logic that can be
 * attached to any {@link Entity}. Behaviors are called once per frame,
 * before the entity's position integration, so they may freely modify
 * velocity or position fields.
 */
public interface Behavior {
    /**
     * Called once per frame for the owning entity.
     *
     * @param entity  the entity this behavior is attached to
     * @param elapsed elapsed time since the last frame, in milliseconds
     */
    void update(Entity<?> entity, long elapsed);

    /**
     * Optional visual artefacts drawn in world space each frame (e.g. path
     * preview, aim indicators). Called after all entities have been drawn.
     * Default implementation is a no-op.
     */
    default void draw(Graphics2D g, Entity<?> entity) {}

    /**
     * Debug overlay drawn in world space when
     * {@code DemoApp.debug >= getDebugLevel()}.
     * Should show internal state useful during development (trajectory,
     * bounds, waypoints, …). Default implementation is a no-op.
     */
    default void drawDebug(Graphics2D g, Entity<?> entity) {}

    /**
     * Minimum value of {@code DemoApp.debug} required for the Renderer to
     * call {@link #drawDebug} on this behavior.
     *
     * <p>Override to raise or lower the activation threshold per behavior:
     * <ul>
     *   <li>Return {@code 2} to show an overlay as soon as the debug bar is visible.</li>
     *   <li>Return {@code 5} to restrict it to very verbose debug sessions.</li>
     * </ul>
     *
     * @return debug level threshold (inclusive); default is {@code 4}
     */
    default int getDebugLevel() { return 4; }
}
