package com.core.gfx;

import java.awt.Rectangle;

import com.core.entity.Entity;
import com.core.entity.World;
import com.core.physics.PhysicsType;

/**
 * Camera is an Entity that defines a viewport on the game window and,
 * optionally, follows a target entity in world space.
 *
 * <p>A scene may hold any number of cameras; each active camera triggers a
 * separate rendering pass clipped to its viewport rectangle.
 *
 * <p>If {@code viewport} is {@code null}, the Renderer auto-sizes it to the
 * full window dimensions (only when it is the single active camera).
 *
 * <p>The world-to-screen transform for a camera is:
 * <pre>
 *   screen.x = viewport.x + (world.x - camera.x) * zoom
 *   screen.y = viewport.y + (world.y - camera.y) * zoom
 * </pre>
 */
public class Camera extends Entity<Camera> {

    /** Entity to track. When non-null, the camera centers on this target each frame. */
    private Entity<?> target;

    /**
     * Position and size of this camera's rendering area on the window.
     * {@code null} means "auto-size to the full window" (handled by Renderer).
     */
    private Rectangle viewport;

    /** Zoom factor. 1.0 = no zoom, 0.5 = zoom out, 2.0 = zoom in. */
    private float zoom = 1.0f;

    /** Whether to draw a border rectangle around the viewport edges. */
    private boolean drawBorder = false;

    public Camera(String name) {
        super(name);
        // Cameras are never processed by the physics engine
        this.physicsType = PhysicsType.NONE;
    }

    /**
     * Sets the entity this camera will follow.
     *
     * @param target entity to track (may be {@code null} to disable tracking)
     * @return {@code this} for fluent chaining
     */
    public Camera setTarget(Entity<?> target) {
        this.target = target;
        return this;
    }

    /**
     * Sets the viewport rectangle on the window.
     *
     * @param vx viewport left edge in window pixels
     * @param vy viewport top edge in window pixels
     * @param vw viewport width in pixels
     * @param vh viewport height in pixels
     * @return {@code this} for fluent chaining
     */
    public Camera setViewport(int vx, int vy, int vw, int vh) {
        this.viewport = new Rectangle(vx, vy, vw, vh);
        return this;
    }

    /**
     * Sets the zoom factor.
     *
     * @param zoom positive zoom factor (values &lt;1 zoom out, &gt;1 zoom in)
     * @return {@code this} for fluent chaining
     */
    public Camera setZoom(float zoom) {
        this.zoom = Math.max(0.01f, zoom);
        return this;
    }

    /**
     * Controls whether a border rectangle is drawn around the viewport.
     *
     * @param draw {@code true} to draw the border
     * @return {@code this} for fluent chaining
     */
    public Camera setDrawBorder(boolean draw) {
        this.drawBorder = draw;
        return this;
    }

    public Entity<?> getTarget() {
        return target;
    }

    public Rectangle getViewport() {
        return viewport;
    }

    public float getZoom() {
        return zoom;
    }

    public boolean isDrawBorder() {
        return drawBorder;
    }

    /**
     * No-op — camera tracking is now handled by
     * {@link com.core.behavior.CameraTrackingBehavior} attached to this camera.
     *
     * <p>Kept for API backward compatibility; callers in the Renderer no longer
     * need to call this method as the Renderer invokes camera behaviors directly.
     *
     * @deprecated Attach a {@link com.core.behavior.CameraTrackingBehavior} to
     *             the camera instead.
     */
    @Deprecated
    public void update(int viewportWidth, int viewportHeight, World world) {
        // logic moved to CameraTrackingBehavior
    }
}
