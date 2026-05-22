package com.core.editor.viewport;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.Consumer;

import javax.swing.JPanel;

import com.core.editor.tools.GridTool;
import com.core.entity.Entity;
import com.core.entity.World;
import com.core.scene.Scene;

/**
 * JPanel that renders a game scene with editor overlay.
 *
 * <p>Provides a viewport into the world with camera controls, grid overlay,
 * and selection rendering for editor functionality.</p>
 */
public class EditorViewport extends JPanel {
    private final EditorCamera camera;
    private final EditorOverlay overlay;
    private Scene scene;
    private GridTool gridTool;

    // Selection tracking
    private Entity<?> selectedEntity = null;
    private Consumer<Entity<?>> onSelectionChanged = null;

    public EditorViewport(Scene scene) {
        this.scene = scene;
        this.camera = new EditorCamera(getWidth(), getHeight());
        this.overlay = new EditorOverlay();
        setBackground(new Color(40, 40, 40));
    }

    public void setGridTool(GridTool gridTool) {
        this.gridTool = gridTool;
        if (overlay != null) {
            overlay.setGridTool(gridTool);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));

        // Update camera viewport size on resize
        if (camera.getViewportWidth() != getWidth() || camera.getViewportHeight() != getHeight()) {
            camera.setViewportSize(getWidth(), getHeight());
        }

        if (scene == null) {
            return;
        }

        // Render grid overlay
        overlay.renderGrid(g2d, camera);

        // Render world and entities in world-space
        World world = scene.getWorld();
        if (world != null) {
            renderEntity(g2d, world);
            for (Entity<?> child : world.children) {
                renderEntity(g2d, child);
            }
        }

        // Render other entities (non-world)
        for (Entity<?> entity : scene.getEntities()) {
            if (!(entity instanceof World)) {
                renderEntity(g2d, entity);
            }
        }

        // Render selection box if entity is selected
        if (selectedEntity != null && selectedEntity.width > 0 && selectedEntity.height > 0) {
            overlay.renderSelectionBox(g2d, camera, selectedEntity.x, selectedEntity.y, selectedEntity.width,
                    selectedEntity.height);
            // Render entity name label
            overlay.renderLabel(g2d, camera, selectedEntity.name, selectedEntity.x, selectedEntity.y - 10);
        }
    }

    /**
     * Renders an entity in world-space.
     */
    private void renderEntity(Graphics2D g, Entity<?> entity) {
        if (!entity.active || (entity.width == 0 && entity.height == 0)) {
            return;
        }

        float sx = camera.worldToScreenX(entity.x);
        float sy = camera.worldToScreenY(entity.y);
        float sw = entity.width * camera.getZoom();
        float sh = entity.height * camera.getZoom();

        // Draw entity as a filled rectangle
        g.setColor(entity.fillColor != null ? entity.fillColor : Color.BLUE);
        g.fillRect((int) sx, (int) sy, (int) sw, (int) sh);

        // Draw entity border
        g.setColor(entity.color != null ? entity.color : Color.BLACK);
        g.drawRect((int) sx, (int) sy, (int) sw, (int) sh);

        // Recursively render children
        for (Entity<?> child : entity.children) {
            renderEntity(g, child);
        }
    }

    public EditorCamera getCamera() {
        return camera;
    }

    public float screenToWorldX(int screenX) {
        return camera.screenToWorldX(screenX);
    }

    public float screenToWorldY(int screenY) {
        return camera.screenToWorldY(screenY);
    }

    public EditorOverlay getOverlay() {
        return overlay;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
        repaint();
    }

    public Scene getScene() {
        return scene;
    }

    public void setSelectedEntity(Entity<?> entity) {
        this.selectedEntity = entity;
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(entity);
        }
        repaint();
    }

    public Entity<?> getSelectedEntity() {
        return selectedEntity;
    }

    /**
     * Registers a listener to be notified when the selection changes.
     *
     * @param listener callback that receives the newly selected entity (may be {@code null})
     */
    public void setOnSelectionChangedListener(Consumer<Entity<?>> listener) {
        this.onSelectionChanged = listener;
    }

    /**
     * Selects an entity at screen coordinates (if any).
     * Returns the topmost entity at that position, or null if none found.
     */
    public Entity<?> selectEntityAtScreenCoords(int screenX, int screenY) {
        float worldX = camera.screenToWorldX(screenX);
        float worldY = camera.screenToWorldY(screenY);

        Entity<?> topmost = null;
        int highestPriority = Integer.MIN_VALUE;

        if (scene != null) {
            for (Entity<?> entity : scene.getEntities()) {
                if (checkEntityHit(entity, worldX, worldY)) {
                    if (entity.renderPriority > highestPriority) {
                        topmost = entity;
                        highestPriority = entity.renderPriority;
                    }
                }
            }
        }

        return topmost;
    }

    private boolean checkEntityHit(Entity<?> entity, float worldX, float worldY) {
        if (!entity.active || entity.width == 0 || entity.height == 0) {
            return false;
        }
        return worldX >= entity.x && worldX < entity.x + entity.width && worldY >= entity.y
                && worldY < entity.y + entity.height;
    }
}
