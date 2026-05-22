package com.core.spatial;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.function.Supplier;

import com.core.behavior.Behavior;
import com.core.entity.Entity;

/**
 * Behavior that renders the {@link QuadTree} spatial-index grid as a debug
 * overlay when attached to an entity with a low {@code renderPriority} (e.g.
 * {@link com.core.entity.World}).
 *
 * <p>
 * Rendering is delegated to {@link QuadTreeDebugOverlay#draw} and only
 * happens via {@link #drawDebug}, so it is visible only when
 * {@code DemoApp.debug >= getDebugLevel()} (default: 4).
 *
 * <p>
 * The zoom factor is read directly from the {@link Graphics2D} transform so
 * stroke and font sizes remain visually stable regardless of camera zoom.
 *
 * <p>
 * Usage — attach once to the scene's World in
 * {@link com.core.scene.BaseScene#setWorld}:
 * 
 * <pre>{@code
 * world.addBehavior(new QuadTreeDebugBehavior(this::getQuadTree));
 * }</pre>
 */
public class QuadTreeDebugBehavior implements Behavior {

    private final Supplier<QuadTree> supplier;

    /**
     * @param supplier provides the live {@link QuadTree} each frame;
     *                 may return {@code null} before the first rebuild
     */
    public QuadTreeDebugBehavior(Supplier<QuadTree> supplier) {
        this.supplier = supplier;
    }

    /** No per-frame state to update. */
    @Override
    public void update(Entity<?> entity, long elapsed) {
    }

    /** No continuous visual output outside debug mode. */
    @Override
    public void draw(Graphics2D g, Entity<?> entity) {
    }

    /**
     * Draws the QuadTree cell grid in world space.
     * Called by the Renderer when {@code DemoApp.debug > 3}.
     */
    @Override
    public void drawDebug(Graphics2D g, Entity<?> entity) {
        QuadTree tree = supplier.get();
        if (tree == null)
            return;

        // Recover the effective zoom from the camera transform already applied to g.
        // AffineTransform.getScaleX() returns the X scale component, which equals
        // the camera zoom after g.scale(zoom, zoom).
        AffineTransform tx = g.getTransform();
        float zoom = (float) tx.getScaleX();
        if (zoom <= 0f)
            zoom = 1f;

        QuadTreeDebugOverlay.draw(g, tree, zoom);
    }

    @Override
    public int getDebugLevel() {
        return 3;
    }
}
