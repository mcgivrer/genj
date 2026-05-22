package com.core.spatial;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;

/**
 * Draws a debug overlay of the {@link QuadTree} structure directly in world space.
 *
 * <p>Must be called <em>after</em> the camera-to-screen transform has been applied to
 * the {@link Graphics2D} context (i.e. inside the per-camera rendering loop, before
 * entity drawing so the grid appears behind all game objects).
 *
 * <p>Visual encoding:
 * <ul>
 *   <li>Cell borders are colour-coded by depth (slate → amber → cyan → purple).</li>
 *   <li>Non-empty leaf cells receive a semi-transparent emerald fill whose opacity
 *       grows with the entity count.</li>
 *   <li>The entity count is printed in the top-left corner of each non-empty leaf.</li>
 * </ul>
 *
 * @see com.core.gfx.Renderer
 */
public final class QuadTreeDebugOverlay {

    private QuadTreeDebugOverlay() {}

    // ── Depth colour palette (matches spatial-quadtree.svg illustration) ──────
    private static final Color[] BORDER_COLORS = {
        new Color(0x94, 0xa3, 0xb8,  60),  // depth 0 — root — barely visible
        new Color(0x94, 0xa3, 0xb8, 150),  // depth 1 — slate
        new Color(0xf5, 0x9e, 0x0b, 180),  // depth 2 — amber
        new Color(0x06, 0xb6, 0xd4, 200),  // depth 3 — cyan
        new Color(0xa7, 0x8b, 0xfa, 200),  // depth 4+ — violet
    };

    /** Emerald green used for leaf fills. */
    private static final Color LEAF_FILL = new Color(0x10, 0xb9, 0x81);

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Renders the QuadTree grid onto {@code g} (already in world-space coordinates).
     *
     * @param g    graphics context with the camera transform already applied
     * @param tree the tree to visualise
     * @param zoom current camera zoom (used to keep stroke and font size screen-stable)
     */
    public static void draw(Graphics2D g, QuadTree tree, float zoom) {
        // Pre-compute zoom-independent sizes
        float   sw        = Math.max(0.3f, 1f / zoom);
        Stroke  cellStroke = new BasicStroke(sw);
        int     fontSize  = Math.max(6, (int) (9f / zoom));
        Font    cellFont  = new Font("Monospaced", Font.PLAIN, fontSize);

        // Save graphics state
        Composite savedComposite = g.getComposite();
        Stroke    savedStroke    = g.getStroke();
        Font      savedFont      = g.getFont();
        Color     savedColor     = g.getColor();

        g.setStroke(cellStroke);
        g.setFont(cellFont);

        tree.visitNodes(info -> {
            int    ix     = (int) info.x();
            int    iy     = (int) info.y();
            int    iw     = (int) Math.ceil(info.w());
            int    ih     = (int) Math.ceil(info.h());
            Color  border = depthColor(info.depth());

            // ── Leaf fill (non-empty leaves only) ──────────────────────────
            if (info.isLeaf() && info.count() > 0) {
                int alpha = Math.min(18 + info.count() * 14, 95);
                g.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, alpha / 255f));
                g.setColor(LEAF_FILL);
                g.fillRect(ix, iy, iw, ih);
            }

            // ── Cell border ────────────────────────────────────────────────
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, border.getAlpha() / 255f));
            g.setColor(opaqueOf(border));
            g.drawRect(ix, iy, iw, ih);

            // ── Entity count label ─────────────────────────────────────────
            if (info.isLeaf() && info.count() > 0) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.90f));
                g.setColor(opaqueOf(border).brighter());
                g.drawString(
                        String.valueOf(info.count()),
                        ix + Math.max(1, (int)(2f / zoom)),
                        iy + fontSize + Math.max(1, (int)(1f / zoom)));
            }
        });

        // Restore graphics state
        g.setComposite(savedComposite);
        g.setStroke(savedStroke);
        g.setFont(savedFont);
        g.setColor(savedColor);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static Color depthColor(int depth) {
        return BORDER_COLORS[Math.min(depth, BORDER_COLORS.length - 1)];
    }

    /** Returns a fully-opaque version of {@code c} (alpha = 255). */
    private static Color opaqueOf(Color c) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue());
    }
}
