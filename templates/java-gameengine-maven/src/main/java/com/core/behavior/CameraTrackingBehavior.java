package com.core.behavior;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.core.entity.Entity;
import com.core.entity.World;
import com.core.gfx.Camera;

/**
 * A {@link Behavior} that positions a {@link Camera} to follow a target entity,
 * with configurable anchor offsets and optional elastic smoothing.
 *
 * <p>Attach this behavior to a {@link Camera} instance instead of using
 * {@link Camera#setTarget(Entity)}:
 * <pre>{@code
 * camera.addBehavior(new CameraTrackingBehavior(player, world)
 *         .setTiltX(0.5f)
 *         .setTiltY(0.75f)
 *         .setElasticity(0.85f));
 * }</pre>
 *
 * <h3>Tilt factors</h3>
 * <ul>
 *   <li>{@link #tiltX} — horizontal fraction of the viewport at which the target
 *       center is locked.  {@code 0} = left edge, {@code 0.5} = center (default),
 *       {@code 1} = right edge.</li>
 *   <li>{@link #tiltY} — vertical fraction.  {@code 0} = top, {@code 0.75} =
 *       75 % from the top (default), {@code 1} = bottom.</li>
 * </ul>
 *
 * <h3>Elasticity</h3>
 * <p>{@link #elasticity} ∈ {@code [0, 1)}: {@code 0} = instant snap (preserves the
 * previous hard-coded behaviour), values closer to {@code 1} produce an increasingly
 * smooth/laggy follow using frame-rate-independent exponential decay:
 * <pre>
 *   stiffness = (1 − elasticity) × 10      [s⁻¹]
 *   blend     = 1 − exp(−stiffness × Δt)
 *   cam.x    += (desired − cam.x) × blend
 * </pre>
 * A value around {@code 0.85} gives a pleasant platformer-camera feel.
 */
public class CameraTrackingBehavior implements Behavior {

    /** Target entity to follow. */
    private final Entity<?> target;

    /**
     * World used for boundary clamping. May be {@code null} to disable clamping.
     */
    private World world;

    /**
     * Horizontal viewport anchor [0, 1].
     * The target's horizontal centre will be placed at {@code tiltX × viewportWidth}
     * from the viewport's left edge.  Default: {@code 0.5} (centred).
     */
    private float tiltX = 0.5f;

    /**
     * Vertical viewport anchor [0, 1].
     * The target's vertical centre will be placed at {@code tiltY × viewportHeight}
     * from the viewport's top edge.  Default: {@code 0.75} (75 % from top).
     */
    private float tiltY = 0.75f;

    /**
     * Elastic smoothing factor [0, 1).
     * <ul>
     *   <li>{@code 0} — instant snap, no lag (default, backward-compatible)</li>
     *   <li>{@code ≈0.85} — smooth platformer feel</li>
     *   <li>close to {@code 1} — very slow follow</li>
     * </ul>
     */
    private float elasticity = 0f;

    /**
     * Creates a tracking behavior for the given target with world-space clamping.
     *
     * @param target the entity the camera will follow (must not be {@code null})
     * @param world  world used for boundary clamping ({@code null} disables clamping)
     */
    public CameraTrackingBehavior(Entity<?> target, World world) {
        this.target = target;
        this.world  = world;
    }

    /**
     * Creates a tracking behavior without world clamping.
     *
     * @param target the entity the camera will follow
     */
    public CameraTrackingBehavior(Entity<?> target) {
        this.target = target;
    }

    // ── Fluent setters ────────────────────────────────────────────────────────

    /**
     * Sets the world used for boundary clamping.
     *
     * @param w world instance (may be {@code null} to disable clamping)
     * @return {@code this}
     */
    public CameraTrackingBehavior setWorld(World w) {
        this.world = w;
        return this;
    }

    /**
     * Sets the horizontal viewport anchor.
     *
     * @param tiltX fraction in [0, 1] ({@code 0.5} = centred)
     * @return {@code this}
     */
    public CameraTrackingBehavior setTiltX(float tiltX) {
        this.tiltX = tiltX;
        return this;
    }

    /**
     * Sets the vertical viewport anchor.
     *
     * @param tiltY fraction in [0, 1] ({@code 0.75} = 75 % from top)
     * @return {@code this}
     */
    public CameraTrackingBehavior setTiltY(float tiltY) {
        this.tiltY = tiltY;
        return this;
    }

    /**
     * Sets the elasticity (smoothing) factor.
     *
     * @param elasticity value clamped to [0, 0.999]; {@code 0} = instant snap
     * @return {@code this}
     */
    public CameraTrackingBehavior setElasticity(float elasticity) {
        this.elasticity = Math.max(0f, Math.min(0.999f, elasticity));
        return this;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Entity<?> getTarget()  { return target; }
    public float getTiltX()       { return tiltX; }
    public float getTiltY()       { return tiltY; }
    public float getElasticity()  { return elasticity; }

    // ── Behavior implementation ───────────────────────────────────────────────

    @Override
    public void update(Entity<?> entity, long elapsed) {
        if (!(entity instanceof Camera cam)) return;
        if (target == null) return;

        Rectangle vp = cam.getViewport();
        if (vp == null) return; // viewport not yet sized by Renderer — skip first frame

        float zoom    = cam.getZoom();
        float vpWorldW = vp.width  / zoom;
        float vpWorldH = vp.height / zoom;

        // Target centre placed at (tiltX, tiltY) of the visible world area
        float desiredX = target.x + target.width  * 0.5f - vpWorldW * tiltX;
        float desiredY = target.y + target.height * 0.5f - vpWorldH * tiltY;

        // Clamp desired position to world bounds
        if (world != null) {
            float maxCx = world.maxX() - vpWorldW;
            float maxCy = world.maxY() - vpWorldH;
            desiredX = Math.max(world.minX(), Math.min(desiredX, maxCx));
            desiredY = Math.max(world.minY(), Math.min(desiredY, maxCy));
        }

        // Apply position: instant snap or exponential-decay smoothing
        if (elasticity <= 0f || elapsed <= 0) {
            cam.x = desiredX;
            cam.y = desiredY;
        } else {
            // Frame-rate-independent exponential blend
            // stiffness ∈ (0, 10]: higher = stiffer (faster follow)
            float stiffness = (1f - elasticity) * 10f;
            float blend = 1f - (float) Math.exp(-stiffness * elapsed * 0.001f);
            cam.x += (desiredX - cam.x) * blend;
            cam.y += (desiredY - cam.y) * blend;
        }
    }

    /**
     * Debug overlay: draws a cross-hair at the anchor point and a line toward
     * the target centre (visible when {@code DemoApp.debug >= 4}).
     */
    @Override
    public void drawDebug(Graphics2D g, Entity<?> entity) {
        if (!(entity instanceof Camera cam)) return;
        if (target == null) return;

        Rectangle vp = cam.getViewport();
        if (vp == null) return;

        float zoom     = cam.getZoom();
        float vpWorldW = vp.width  / zoom;
        float vpWorldH = vp.height / zoom;

        // The anchor point in world space
        int ax = (int) (cam.x + vpWorldW * tiltX + 0.5f);
        int ay = (int) (cam.y + vpWorldH * tiltY + 0.5f);

        // Cross-hair at the anchor
        g.setColor(new Color(255, 200, 0, 200));
        g.drawLine(ax - 10, ay, ax + 10, ay);
        g.drawLine(ax, ay - 10, ax, ay + 10);
        g.drawOval(ax - 4, ay - 4, 8, 8);

        // Line from anchor to target centre
        int tx = (int) (target.x + target.width  * 0.5f + 0.5f);
        int ty = (int) (target.y + target.height * 0.5f + 0.5f);
        g.setColor(new Color(255, 200, 0, 100));
        g.drawLine(ax, ay, tx, ty);
    }

    @Override
    public int getDebugLevel() { return 4; }
}
