package com.core.gfx;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import com.core.App;
import com.core.behavior.VisualDebugBehavior;
import com.core.entity.Entity;
import com.core.scene.Scene;
import com.core.utils.TextAlign;

import java.awt.image.BufferStrategy;
import java.awt.geom.AffineTransform;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;

import static com.core.App.getI18n;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;

import com.core.entity.TextObject;
import com.core.gfx.plugin.DefaultEntityRenderer;
import com.core.gfx.plugin.EllipseRenderer;
import com.core.gfx.plugin.EntityRenderer;
import com.core.gfx.plugin.ImageRenderer;
import com.core.gfx.plugin.LineRenderer;
import com.core.gfx.plugin.PointRenderer;
import com.core.gfx.plugin.RectangleRenderer;
import com.core.gfx.plugin.TextObjectRenderer;

public class Renderer {
    private final App app;
    private JFrame window;

    /** Ordered list of renderer plugins; first match wins. */
    private final List<EntityRenderer<?>> plugins = new ArrayList<>();

    public Renderer(App app) {
        this.app = app;
        registerDefaultPlugins();
    }

    /**
     * Registers a renderer plugin with the highest priority (checked first).
     * Call this from your scene to override or extend the default renderers.
     *
     * @param plugin the plugin to add
     */
    public void registerPlugin(EntityRenderer<?> plugin) {
        plugins.add(0, plugin);
    }

    /**
     * Registers the built-in plugins in reverse priority order so that the
     * most specific ones (Point, Line …) end up at the front of the list.
     * {@link DefaultEntityRenderer} is always last (catch-all).
     */
    private void registerDefaultPlugins() {
        // Registered last → lowest priority (fallback)
        plugins.add(new DefaultEntityRenderer());
        // Registered in increasing specificity so they end up before the fallback
        plugins.add(0, new RectangleRenderer());
        plugins.add(0, new EllipseRenderer());
        plugins.add(0, new ImageRenderer());
        plugins.add(0, new LineRenderer());
        plugins.add(0, new PointRenderer());
        // TextObjectRenderer: highest priority — intercepts all TextObject instances
        // (hud=true ones are skipped silently; world-space ones are drawn immediately)
        plugins.add(0, new TextObjectRenderer());
    }

    public void createWindow(App app, Dimension winDim) {
        window = new JFrame(
                getI18n("app.main.name", "Application").formatted(
                        getI18n("app.main.version", "1.0.0")));
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setPreferredSize(winDim);
        window.pack();
        window.setVisible(true);
        window.createBufferStrategy(3);
    }

    public void render(Scene scene, Map<String, Object> stats, long elapsed) {
        // prepare drawing graphics API
        BufferStrategy bf = window.getBufferStrategy();
        Graphics2D g = (Graphics2D) bf.getDrawGraphics();
        // set drawing configuration.
        g.setRenderingHints(
                Map.of(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON,
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
        // clear the full window once
        g.setBackground(Color.BLACK);
        g.clearRect(0, 0, getWindowWidth(), getWindowHeight());

        // Tracking for HUD: deduplicated visible entity IDs across all cameras
        Set<Long> visibleIds = new LinkedHashSet<>();
        int totalActive = (int) scene.getEntities().stream().filter(Entity::isActive).count();

        // collect active cameras
        List<Camera> activeCameras = scene.getCameras().stream()
                .filter(Camera::isActive)
                .toList();

        if (activeCameras.isEmpty()) {
            // Fallback: no cameras — direct world rendering without transform
            scene.getEntities().stream()
                    .filter(Entity::isActive)
                    .sorted((a, b) -> a.renderPriority - b.renderPriority)
                    .forEach(e -> {
                        visibleIds.add(e.id);
                        drawEntity(g, e);
                        for (var b : e.behaviors) {
                            b.draw(g, e);
                            if (App.debug >= b.getDebugLevel()) b.drawDebug(g, e);
                        }
                    });
        } else {
            // Any camera without an explicit viewport fills the whole window
            for (Camera cam : activeCameras) {
                if (cam.getViewport() == null) {
                    cam.setViewport(0, 0, getWindowWidth(), getWindowHeight());
                }
            }

            for (Camera cam : activeCameras) {
                Rectangle vp = cam.getViewport();

                // Update camera position: run camera behaviors (e.g. CameraTrackingBehavior)
                for (var b : cam.behaviors) {
                    b.update(cam, elapsed);
                }

                // Save current clip and transform
                Shape savedClip = g.getClip();
                AffineTransform savedTransform = g.getTransform();

                // Clip rendering to this viewport
                g.setClip(vp.x, vp.y, vp.width, vp.height);

                // Fill viewport background
                g.setColor(Color.BLACK);
                g.fillRect(vp.x, vp.y, vp.width, vp.height);

                // Apply camera-to-screen transform:
                //   screen.x = vp.x + (world.x - cam.x) * zoom
                //   screen.y = vp.y + (world.y - cam.y) * zoom
                float zoom = cam.getZoom();
                g.translate(vp.x - cam.x * zoom, vp.y - cam.y * zoom);
                g.scale(zoom, zoom);

                // Frustum culling: only draw entity shapes visible in this camera's world rect
                float worldW = vp.width  / zoom;
                float worldH = vp.height / zoom;

                List<Entity<?>> visible = scene.getVisibleEntities(cam.x, cam.y, worldW, worldH);
                Set<Long> visibleSet = new LinkedHashSet<>();
                visible.forEach(e -> { if (e.isActive()) visibleSet.add(e.id); });

                // Refresh the shared primary-camera reference for VisualDebugBehavior (once per frame)
                VisualDebugBehavior.refreshPrimaryCamera(scene);

                // Single render pass sorted by renderPriority.
                // Entity shape drawn only if inside the frustum; behaviors always run
                // (they may produce UI/debug overlays regardless of visibility).
                List<Entity<?>> allSorted = scene.getEntities().stream()
                        .filter(Entity::isActive)
                        .sorted((a, b) -> a.renderPriority - b.renderPriority)
                        .toList();

                for (Entity<?> e : allSorted) {
                    if (visibleSet.contains(e.id)) {
                        visibleIds.add(e.id);
                        drawEntity(g, e);
                    }
                    for (var b : e.behaviors) {
                        b.draw(g, e);
                        if (App.debug >= b.getDebugLevel()) {
                            b.drawDebug(g, e);
                        }
                    }
                }

                // Restore transform and clip
                g.setTransform(savedTransform);
                g.setClip(savedClip);

                // Draw viewport border (secondary cameras, e.g. minimap)
                if (cam.isDrawBorder()) {
                    g.setColor(Color.DARK_GRAY);
                    g.drawRect(vp.x, vp.y, vp.width - 1, vp.height - 1);
                }
            }
        }

        // Store visibility stats before drawing the HUD
        stats.put("visibleEntities", visibleIds.size());
        stats.put("totalEntities", totalActive);

        // HUD overlays — drawn in screen space, no camera transform
        drawHUD(g, stats, scene);

        // switch buffer
        g.dispose();
        bf.show();
    }

    private void drawHUD(Graphics2D g, Map<String, Object> stats, Scene scene) {
        // Render HUD TextObjects (screen-space, relX/relY resolved against window size)
        scene.getEntities().stream()
                .filter(e -> e instanceof TextObject to && to.hud && e.isActive())
                .map(e -> (TextObject) e)
                .sorted((a, b) -> a.renderPriority - b.renderPriority)
                .forEach(to -> {
                    if (to.text == null || to.text.isEmpty()) return;
                    int sx = to.relX >= 0f ? (int) (to.relX * getWindowWidth())  : (int) (to.x + 0.5f);
                    int sy = to.relY >= 0f ? (int) (to.relY * getWindowHeight()) : (int) (to.y + 0.5f);
                    drawText(g, to.text, sx, sy, to.align, to.fontSize, to.color, to.fontStyle);
                });

        if (App.debug > 0) {
            int barH = 15;
            int barY = getWindowHeight() - barH - 15;
            Composite savedComposite = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g.setColor(Color.BLACK);
            g.fillRect(0, barY, getWindowWidth(), barH);
            g.setComposite(savedComposite);
            drawText(
                    g,
                    "[ dbg:%d | obj:%04d / %04d | elapsed:%02d | time:%s | FPS:%03d | pause:%s ]".formatted(
                            App.debug,
                            stats.getOrDefault("visibleEntities", 0),
                            stats.getOrDefault("totalEntities", 0),
                            stats.get("elapsed"),
                            stats.get("gameTime"),
                            stats.get("fps"),
                            App.pause ? "ON" : "OFF"),
                    barH,
                    getWindowHeight() - barH,
                    TextAlign.LEFT,
                    11.0f,
                    Color.ORANGE,
                    Font.PLAIN);
        }
    }

    @SuppressWarnings("unchecked")
    private void drawEntity(Graphics2D g, Entity<?> e) {
        for (EntityRenderer<?> plugin : plugins) {
            if (plugin.supports(e)) {
                ((EntityRenderer<Entity<?>>) plugin).render(g, e);
                return;
            }
        }
        // Should not reach here because DefaultEntityRenderer is always present,
        // but kept as a safety net.
        e.draw(g);
    }

    /**
     * Returns the current drawable width of the window.
     */
    public int getWindowWidth() {
        return window.getWidth();
    }

    /**
     * Returns the current drawable height of the window.
     */
    public int getWindowHeight() {
        return window.getHeight();
    }

    /**
     * Draw text on the screen.
     *
     * @param g         the graphics context
     * @param text      the text to draw
     * @param x         the x coordinate of the text
     * @param y         the y coordinate of the text
     * @param align     the alignment of the text
     * @param fontSize  the font size of the text
     * @param c         the color of the text
     * @param fontStyle the font style of the text
     */
    public static void drawText(
            Graphics2D g,
            String text,
            int x,
            int y,
            TextAlign align,
            float fontSize,
            Color c,
            int fontStyle) {
        Font font = g.getFont().deriveFont(fontStyle, fontSize);
        drawText(g, text, x, y, align, font, c);
    }

    /**
     * Draw text on the screen.
     *
     * @param g     the graphics context
     * @param text  the text to draw
     * @param x     the x coordinate of the text
     * @param y     the y coordinate of the text
     * @param align the alignment of the text
     * @param font  the font of the text
     * @param c     the color of the text
     */
    public static void drawText(
            Graphics2D g,
            String text,
            int x,
            int y,
            TextAlign align,
            Font font,
            Color c) {
        g.setFont(font);
        g.setColor(c);
        int offsetX = align.equals(TextAlign.CENTER)
                ? (int) -(g.getFontMetrics().stringWidth(text) * 0.5)
                : align.equals(TextAlign.RIGHT)
                        ? -g.getFontMetrics().stringWidth(text)
                        : 0;
        g.drawString(text, x + offsetX, y);
    }

    /**
     * Gets the window of the App.
     * 
     * @return the JFrame window of the App
     */
    public JFrame getWindow() {
        return window;
    }

    /**
     * Disposes the Renderer resources.
     */
    public void dispose() {
        if (window != null) {
            window.dispose();
        }
    }
}
