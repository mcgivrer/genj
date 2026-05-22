package com.core.editor.tools;

import com.core.editor.viewport.EditorViewport;
import com.core.entity.Entity;

/**
 * SelectTool — allows selecting entities by clicking on them in the viewport.
 *
 * <p>Left-click to select the topmost entity at the click position.
 * The selected entity is highlighted with a green box and handles in the viewport.</p>
 */
public class SelectTool implements EditorTool {
    private final EditorViewport viewport;

    public SelectTool(EditorViewport viewport) {
        this.viewport = viewport;
    }

    @Override
    public String getName() {
        return "SelectTool";
    }

    @Override
    public void onActivate() {
        // No special activation needed
    }

    @Override
    public void onDeactivate() {
        // Keep selection on deactivate
    }

    @Override
    public void onMousePressed(float x, float y) {
        // Select entity at this world coordinate
        Entity<?> entity = findEntityAt(x, y);
        viewport.setSelectedEntity(entity);
        viewport.repaint();
    }

    @Override
    public void onMouseReleased(float x, float y) {
        // No action on release
    }

    @Override
    public void onMouseDragged(float x, float y) {
        // No action while dragging (selection stays static)
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
