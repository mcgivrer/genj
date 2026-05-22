package com.core.behavior;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.RoundRectangle2D;
import java.util.Comparator;

import com.core.App;
import com.core.entity.Entity;
import com.core.gfx.Camera;
import com.core.scene.Scene;

/**
 * Behavior that renders a floating debug panel beside each instrumented entity,
 * linked by a dashed arrow. Only the camera with the largest viewport area is
 * used.
 *
 * <ul>
 * <li>{@code debug > 2} — id, name, position, size</li>
 * <li>{@code debug > 3} — adds vx, vy, mass, material</li>
 * <li>{@code debug > 4} — highlights colliding sides in yellow (via
 * {@link #drawDebug})</li>
 * </ul>
 *
 * <p>
 * Call {@link #refreshPrimaryCamera(Scene)} once per frame (e.g. in
 * {@code BaseScene.update()} or in the Renderer) before entities are drawn.
 */
public class VisualDebugBehavior implements Behavior {

    // ── shared across all instances ────────────────────────────────────────────
    private static Camera primaryCamera = null;

    /**
     * Selects the active camera with the largest viewport area.
     * Must be called once per frame before any {@code draw()} call.
     */
    public static void refreshPrimaryCamera(Scene scene) {
        // A camera with viewport == null fills the whole window → treat as highest
        // area.
        primaryCamera = scene.getCameras().stream()
                .filter(Camera::isActive)
                .max(Comparator.comparingInt(c -> {
                    java.awt.Rectangle vp = c.getViewport();
                    return vp != null ? vp.width * vp.height : Integer.MAX_VALUE;
                }))
                .orElse(null);
    }

    // ── visual constants ───────────────────────────────────────────────────────
    private static final Color COLOR_TEXT       = new Color(0xFF, 0x8C, 0x00);
    private static final Color COLOR_BG          = new Color(2, 2, 14);
    private static final Color COLOR_BORDER      = new Color(0xFF, 0x8C, 0x00);
    private static final Color COLOR_SEPARATOR   = new Color(0x1E, 0x3A, 0x5F);
    private static final Color COLOR_COLLISION   = Color.YELLOW;
    /** Color of the velocity vector arrow. */
    private static final Color COLOR_VELOCITY    = new Color(0x00, 0xE5, 0xFF);

    /**
     * Scale: screen-pixels of arrow length per unit of speed (px/s).
     * Speed 100 px/s → 8 screen pixels of arrow.
     */
    private static final float VELOCITY_PX_PER_UNIT = 0.08f;
    /** Maximum arrow length, in screen pixels (prevents giant arrows at high speed). */
    private static final float VELOCITY_MAX_PX      = 80f;
    /** Minimum speed (px/s) below which the arrow is suppressed. */
    private static final float VELOCITY_MIN_SPEED   = 1f;

    /** Panel width in screen pixels (scaled by 1/zoom for world-space drawing). */
    private static final float PANEL_SCREEN_W = 148f;
    /** Horizontal gap between entity edge and panel edge, in screen pixels. */
    private static final float MARGIN_SCREEN = 20f;
    /** Vertical padding inside the panel, in screen pixels. */
    private static final float PADDING_SCREEN = 3f;
    /** Font size in screen pixels. */
    private static final float FONT_SIZE_SCREEN = 9f;
    /** Line height multiplier. */
    private static final float LINE_HEIGHT_MULT = 1.3f;
    /** Corner arc radius in screen pixels. */
    private static final float ARC_SCREEN = 3f;
    /** Lerp factor per frame for the elastic vertical tracking of the panel. */
    private static final float ELASTIC_FACTOR = 0.12f;

    // ── per-instance elastic state ─────────────────────────────────────────────
    /**
     * Smoothed entity-center Y used for elastic panel tracking. NaN = not yet
     * initialised.
     */
    private float smoothCy = Float.NaN;
    private int debugLevel = 4;

    public VisualDebugBehavior(int debugLevel) {
        this.debugLevel = debugLevel;
    }

    // ── Behavior ───────────────────────────────────────────────────────────────

    @Override
    public void update(Entity<?> entity, long elapsed) {
        // no-op: this behavior only renders
    }

    /**
     * Draws the debug panel. Active when {@code debug > 2}.
     * Called every frame by the Renderer in world space.
     */
    @Override
    public void draw(Graphics2D g, Entity<?> entity) {
        if (!App.isDebugGreaterThan(getDebugLevel()))
            return;
        if (primaryCamera == null)
            return;
        if (!App.matchesDebugFilter(entity.name))
            return;
        drawPanel(g, entity);
        drawVelocityVector(g, entity);
    }

    /**
     * Highlights the sides involved in a collision with a thick yellow stroke.
     * Active when {@code debug > 3} — consistent with when the Renderer calls this
     * method.
     */
    @Override
    public void drawDebug(Graphics2D g, Entity<?> entity) {
        if (!App.isDebugGreaterThan(getDebugLevel() + 1))
            return;
        if (!App.matchesDebugFilter(entity.name))
            return;
        drawCollisionSides(g, entity);
    }

    // ── panel rendering ────────────────────────────────────────────────────────

    private void drawPanel(Graphics2D g, Entity<?> entity) {
        Camera cam = primaryCamera;
        float zoom = cam.getZoom();

        // Convert screen-space constants to world-space units
        float pw = PANEL_SCREEN_W / zoom;
        float margin = MARGIN_SCREEN / zoom;
        float padding = PADDING_SCREEN / zoom;
        float arc = ARC_SCREEN / zoom;
        float fontSize = FONT_SIZE_SCREEN / zoom;
        float lineH = fontSize * LINE_HEIGHT_MULT;

        // World-space visible bounds from this camera
        java.awt.Rectangle vp = cam.getViewport();
        float vpW = (vp != null) ? vp.width : 10_000f;
        float vpH = (vp != null) ? vp.height : 10_000f;
        float worldLeft = cam.x;
        float worldTop = cam.y;
        float worldRight = cam.x + vpW / zoom;
        float worldBottom = cam.y + vpH / zoom;

        // If entity is fully outside the viewport, show a compact mini-panel instead
        boolean entityVisible = entity.x + entity.width > worldLeft
                && entity.x < worldRight
                && entity.y + entity.height > worldTop
                && entity.y < worldBottom;
        if (!entityVisible) {
            drawMiniPanel(g, entity, worldLeft, worldTop, worldRight, worldBottom,
                    zoom, pw, padding, arc, fontSize, lineH);
            return;
        }

        // Count content lines to size the panel height
        int lines = 4; // debug > 2: id, name, pos, size
        if (App.isDebugGreaterThan(getDebugLevel()))
            lines += 5; // + separator row + vx, vy, mass, mat
        float ph = padding * 2 + lines * lineH + (App.isDebugGreaterThan(getDebugLevel() + 1) ? lineH * 0.4f : 0);

        // Decide side: prefer right; fall back to left if it fits; else pick side with
        // more room
        boolean fitsRight = entity.x + entity.width + margin + pw <= worldRight;
        boolean fitsLeft = entity.x - margin - pw >= worldLeft;
        boolean onRight;
        if (fitsRight) {
            onRight = true;
        } else if (fitsLeft) {
            onRight = false;
        } else {
            float spaceRight = worldRight - (entity.x + entity.width);
            float spaceLeft = entity.x - worldLeft;
            onRight = spaceRight >= spaceLeft;
        }
        float panelX = onRight ? entity.x + entity.width + margin : entity.x - pw - margin;

        // ── elastic (lerp) vertical tracking ──
        float targetCy = entity.y + entity.height / 2f;
        if (Float.isNaN(smoothCy)) {
            smoothCy = targetCy;
        } else {
            smoothCy += (targetCy - smoothCy) * ELASTIC_FACTOR;
        }
        float panelY = Math.max(
                worldTop + margin,
                Math.min(smoothCy - ph / 2f, worldBottom - ph - margin));

        // ── background ──
        Composite savedComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.48f));
        g.setColor(COLOR_BG);
        g.fill(new RoundRectangle2D.Float(panelX, panelY, pw, ph, arc, arc));
        g.setComposite(savedComposite);

        // ── accent line on the entity-facing side only ──
        Stroke savedStroke = g.getStroke();
        float accentX = onRight ? panelX : panelX + pw;
        g.setColor(COLOR_BORDER);
        g.setStroke(new BasicStroke(2f / zoom, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Line2D.Float(accentX, panelY + arc, accentX, panelY + ph - arc));

        // ── dashed arrow: from entity center to panel mid-height (oblique if panel
        // moved) ──
        float entityMidY = entity.y + entity.height / 2f;
        float panelMidY = panelY + ph / 2f;
        float arrowStartX = onRight ? entity.x + entity.width : entity.x;
        float arrowEndX = onRight ? panelX : panelX + pw;
        g.setStroke(new BasicStroke(
                1f / zoom, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                new float[] { 4f / zoom, 3f / zoom }, 0f));
        g.draw(new java.awt.geom.Line2D.Float(arrowStartX, entityMidY, arrowEndX, panelMidY));

        // arrowhead at the panel end
        g.setStroke(savedStroke);
        float tipSize = 4f / zoom;
        int[] ax, ay;
        if (onRight) {
            ax = new int[] { (int) arrowEndX, (int) (arrowEndX - tipSize), (int) (arrowEndX - tipSize) };
            ay = new int[] { (int) panelMidY, (int) (panelMidY - tipSize / 2), (int) (panelMidY + tipSize / 2) };
        } else {
            ax = new int[] { (int) arrowEndX, (int) (arrowEndX + tipSize), (int) (arrowEndX + tipSize) };
            ay = new int[] { (int) panelMidY, (int) (panelMidY - tipSize / 2), (int) (panelMidY + tipSize / 2) };
        }
        g.fillPolygon(ax, ay, 3);

        // ── text ──
        Font savedFont = g.getFont();
        Object savedAA = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int fontPx = Math.max(1, (int) (fontSize + 0.5f));
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontPx));
        g.setColor(COLOR_TEXT);

        float tx = panelX + padding;
        float ty = panelY + padding + fontSize; // baseline of first line

        ty = drawLine(g, tx, ty, lineH, "id   : " + entity.id);
        ty = drawLine(g, tx, ty, lineH, "name : " + entity.name);
        ty = drawLine(g, tx, ty, lineH, "pos  : (%.1f, %.1f)".formatted(entity.x, entity.y));
        ty = drawLine(g, tx, ty, lineH, "size : %d x %d".formatted(entity.width, entity.height));

        if (App.isDebugGreaterThan(getDebugLevel() + 1)) {
            // separator
            ty += lineH * 0.4f;
            Stroke s = g.getStroke();
            g.setColor(COLOR_SEPARATOR);
            g.setStroke(new BasicStroke(1f / zoom));
            g.drawLine((int) (tx), (int) (ty - lineH * 0.6f),
                    (int) (panelX + pw - padding), (int) (ty - lineH * 0.6f));
            g.setStroke(s);
            g.setColor(COLOR_TEXT);

            ty = drawLine(g, tx, ty, lineH, "vx   : %+.2f".formatted(entity.vx));
            ty = drawLine(g, tx, ty, lineH, "vy   : %+.2f".formatted(entity.vy));
            ty = drawLine(g, tx, ty, lineH, "mass : %.2f".formatted(entity.mass));
            ty = drawLine(g, tx, ty, lineH, "mat  : " + entity.material.name);
            drawLine(g, tx, ty, lineH, "onGround : " + entity.onGround);
        }

        // restore font and text-antialiasing hint
        g.setFont(savedFont);
        if (savedAA != null) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, savedAA);
        }
    }

    /**
     * Draws a compact 2-line panel (id + name) stuck to the viewport edge that is
     * closest to the entity, when the entity is entirely outside the camera's
     * visible area.
     * <p>
     * The panel is anchored to the edge in the primary off-screen direction, and
     * the
     * accent line is placed on that same edge (vertical for left/right, horizontal
     * for
     * top/bottom) to suggest where to look for the entity.
     */
    private void drawMiniPanel(Graphics2D g, Entity<?> entity,
            float worldLeft, float worldTop,
            float worldRight, float worldBottom,
            float zoom, float pw, float padding, float arc,
            float fontSize, float lineH) {
        float margin = MARGIN_SCREEN / zoom;
        float miniW = pw * 0.72f;
        float miniH = padding * 2 + 2 * lineH;

        float entityCx = entity.x + entity.width / 2f;
        float entityCy = entity.y + entity.height / 2f;

        // Overflow distances per side (positive = how far outside)
        float overRight = entity.x - worldRight; // > 0 if entity past right edge
        float overLeft = worldLeft - (entity.x + entity.width); // > 0 if entity past left edge
        float overBottom = entity.y - worldBottom;
        float overTop = worldTop - (entity.y + entity.height);

        // Primary off-screen direction: the one with the largest overflow
        float maxOff = Math.max(Math.max(overRight, overLeft), Math.max(overBottom, overTop));
        boolean primaryRight = (overRight == maxOff);
        boolean primaryLeft = !primaryRight && (overLeft == maxOff);
        boolean primaryBottom = !primaryRight && !primaryLeft && (overBottom == maxOff);
        // primaryTop is the fallback

        float panelX, panelY;

        if (primaryRight) {
            // Stick to right edge, track entity vertically (clamped)
            panelX = worldRight - miniW - margin;
            panelY = Math.max(worldTop + margin,
                    Math.min(entityCy - miniH / 2f, worldBottom - miniH - margin));
        } else if (primaryLeft) {
            // Stick to left edge
            panelX = worldLeft + margin;
            panelY = Math.max(worldTop + margin,
                    Math.min(entityCy - miniH / 2f, worldBottom - miniH - margin));
        } else if (primaryBottom) {
            // Stick to bottom edge, track entity horizontally (clamped)
            panelY = worldBottom - miniH - margin;
            panelX = Math.max(worldLeft + margin,
                    Math.min(entityCx - miniW / 2f, worldRight - miniW - margin));
        } else {
            // Stick to top edge
            panelY = worldTop + margin;
            panelX = Math.max(worldLeft + margin,
                    Math.min(entityCx - miniW / 2f, worldRight - miniW - margin));
        }

        // ── background ──
        Composite savedComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.48f));
        g.setColor(COLOR_BG);
        g.fill(new RoundRectangle2D.Float(panelX, panelY, miniW, miniH, arc, arc));
        g.setComposite(savedComposite);

        // ── accent line on the edge-side facing the entity ──
        Stroke savedStroke = g.getStroke();
        g.setColor(COLOR_BORDER);
        g.setStroke(new BasicStroke(2f / zoom, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (primaryRight) {
            // entity is to the right → accent on right side of panel
            float ax = panelX + miniW;
            g.draw(new java.awt.geom.Line2D.Float(ax, panelY + arc, ax, panelY + miniH - arc));
        } else if (primaryLeft) {
            // entity is to the left → accent on left side
            g.draw(new java.awt.geom.Line2D.Float(panelX, panelY + arc, panelX, panelY + miniH - arc));
        } else if (primaryBottom) {
            // entity is below → accent on bottom edge of panel
            float ay = panelY + miniH;
            g.draw(new java.awt.geom.Line2D.Float(panelX + arc, ay, panelX + miniW - arc, ay));
        } else {
            // entity is above → accent on top edge
            g.draw(new java.awt.geom.Line2D.Float(panelX + arc, panelY, panelX + miniW - arc, panelY));
        }
        g.setStroke(savedStroke);

        // ── text (dimmed to signal the entity is off-screen) ──
        Font savedFont = g.getFont();
        Object savedAA = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int fontPx = Math.max(1, (int) (fontSize + 0.5f));
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontPx));
        g.setColor(COLOR_TEXT.darker());

        float tx = panelX + padding;
        float ty = panelY + padding + fontSize;
        ty = drawLine(g, tx, ty, lineH, "id   : " + entity.id);
        drawLine(g, tx, ty, lineH, "name : " + entity.name);

        g.setFont(savedFont);
        if (savedAA != null) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, savedAA);
        }
    }

    /** Draws a single line of text and returns the baseline Y for the next line. */
    private float drawLine(Graphics2D g, float x, float y, float lineH, String text) {
        g.drawString(text, x, y);
        return y + lineH;
    }

    // ── velocity vector rendering ──────────────────────────────────────────────

    /**
     * Draws a velocity vector arrow from the entity's centre, scaled and clamped to
     * a maximum screen length. The arrow is suppressed when the speed is below
     * {@link #VELOCITY_MIN_SPEED}.
     *
     * <ul>
     *   <li>Arrow length = {@code min(speed * VELOCITY_PX_PER_UNIT, VELOCITY_MAX_PX)}
     *       screen pixels, converted to world units via {@code zoom}.</li>
     *   <li>A small speed label is drawn beside the arrowhead.</li>
     * </ul>
     */
    private void drawVelocityVector(Graphics2D g, Entity<?> entity) {
        float speed = (float) Math.sqrt(entity.vx * entity.vx + entity.vy * entity.vy);
        if (speed < VELOCITY_MIN_SPEED) return;

        float zoom = primaryCamera.getZoom();

        // Arrow length in world units, proportional to speed and capped
        float screenLen = Math.min(speed * VELOCITY_PX_PER_UNIT, VELOCITY_MAX_PX);
        float worldLen  = screenLen / zoom;

        // Direction unit vector
        float dx = entity.vx / speed;
        float dy = entity.vy / speed;

        // Origin: entity centre
        float cx = entity.x + entity.width  * 0.5f;
        float cy = entity.y + entity.height * 0.5f;

        float ex = cx + dx * worldLen;
        float ey = cy + dy * worldLen;

        Stroke savedStroke = g.getStroke();
        g.setColor(COLOR_VELOCITY);
        g.setStroke(new BasicStroke(1.5f / zoom, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Line2D.Float(cx, cy, ex, ey));

        // Arrowhead (filled triangle at tip)
        float headSize = 6f / zoom;
        // Perpendicular unit vector (rotated 90°)
        float px = -dy;
        float py =  dx;
        int[] hx = {
            Math.round(ex),
            Math.round(ex - dx * headSize + px * headSize * 0.38f),
            Math.round(ex - dx * headSize - px * headSize * 0.38f)
        };
        int[] hy = {
            Math.round(ey),
            Math.round(ey - dy * headSize + py * headSize * 0.38f),
            Math.round(ey - dy * headSize - py * headSize * 0.38f)
        };
        g.setStroke(savedStroke);
        g.fillPolygon(hx, hy, 3);

        // Speed label (only when vx/vy text block is visible, i.e. debug > level+1)
        if (App.isDebugGreaterThan(getDebugLevel() + 1)) {
            float fontSize   = FONT_SIZE_SCREEN / zoom;
            Font savedFont   = g.getFont();
            Object savedAA   = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                               RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(1, (int)(fontSize + 0.5f))));
            // Offset label slightly to the side of the arrowhead
            float lx = ex + px * (4f / zoom);
            float ly = ey + py * (4f / zoom);
            g.drawString("%.0f".formatted(speed), lx, ly);
            g.setFont(savedFont);
            if (savedAA != null) {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, savedAA);
            }
        }
    }

    // ── collision sides rendering ──────────────────────────────────────────────

    private void drawCollisionSides(Graphics2D g, Entity<?> entity) {
        if (!entity.collisionTop && !entity.collisionBottom
                && !entity.collisionLeft && !entity.collisionRight) {
            return;
        }
        float zoom = (primaryCamera != null) ? primaryCamera.getZoom() : 1f;
        Stroke saved = g.getStroke();
        g.setColor(COLOR_COLLISION);
        g.setStroke(new BasicStroke(3f / zoom, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int ex = (int) entity.x, ey = (int) entity.y;
        int er = (int) (entity.x + entity.width);
        int eb = (int) (entity.y + entity.height);

        if (entity.collisionTop)
            g.drawLine(ex, ey, er, ey);
        if (entity.collisionBottom)
            g.drawLine(ex, eb, er, eb);
        if (entity.collisionLeft)
            g.drawLine(ex, ey, ex, eb);
        if (entity.collisionRight)
            g.drawLine(er, ey, er, eb);

        g.setStroke(saved);
    }

    @Override
    public int getDebugLevel() {
        return debugLevel;
    }

}
