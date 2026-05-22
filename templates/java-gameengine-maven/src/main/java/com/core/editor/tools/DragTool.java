package com.core.editor.tools;

import com.core.editor.viewport.EditorViewport;
import com.core.entity.Entity;

/**
 * DragTool — allows dragging entities to move them with optional grid snapping.
 *
 * <p>Left-click and drag to move an entity. When grid snapping is active, the entity
 * snaps to the grid during dragging.</p>
 */
public class DragTool implements EditorTool {
    private final EditorViewport viewport;
    private final GridTool gridTool;

    private Entity<?> draggingEntity = null;
    private float dragStartWorldX = 0.0f;
    private float dragStartWorldY = 0.0f;
    private float entityStartX = 0.0f;
    private float entityStartY = 0.0f;

    public DragTool(EditorViewport viewport, GridTool gridTool) {
        this.viewport = viewport;
        this.gridTool = gridTool;
    }

    @Override
    public String getName() {
        return "DragTool";
    }

    @Override
    public void onActivate() {
        // No special activation needed
    }

    @Override
    public void onDeactivate() {
        draggingEntity = null;
    }

    @Override
    public void onMousePressed(float x, float y) {
        // Start dragging the entity under the cursor
        draggingEntity = findEntityAt(x, y);
        if (draggingEntity != null) {
            dragStartWorldX = x;
            dragStartWorldY = y;
            entityStartX = draggingEntity.x;
            entityStartY = draggingEntity.y;
            viewport.setSelectedEntity(draggingEntity);
            viewport.repaint();
        }
    }

    @Override
    public void onMouseReleased(float x, float y) {
        draggingEntity = null;
    }

    @Override
    public void onMouseDragged(float x, float y) {
        if (draggingEntity == null) return;

        // Apply delta movement
        float deltaX = x - dragStartWorldX;
        float deltaY = y - dragStartWorldY;

        float newX = entityStartX + deltaX;
        float newY = entityStartY + deltaY;

        // Snap to grid if active
        if (gridTool != null && gridTool.isGridActive()) {
            float[] snapped = gridTool.snapToGrid(newX, newY);
            newX = snapped[0];
            newY = snapped[1];
        }

        draggingEntity.x = newX;
        draggingEntity.y = newY;

        viewport.repaint();
    }

    @Override
    public void onMouseMoved(float x, float y) {
        // No action while moving
    }

    private Entity<?> findEntityAt(float worldX, float worldY) {
        if (viewport.getScene() == null) return null;

        Entity<?> topmost = null;
        int highestPriority = Integer.MIN_VALUE;

        for (Entity<?> entity : viewport.getScene().getEntities()) {
            if (checkEntityHit(entity, worldX, worldY)) {
                if (entity.renderPriority > highestPriority) {
                    topmost = entity;
                    highestPriority = entity.renderPriority;
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
