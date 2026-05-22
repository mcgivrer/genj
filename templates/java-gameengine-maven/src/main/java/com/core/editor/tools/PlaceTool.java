package com.core.editor.tools;

import com.core.editor.viewport.EditorViewport;
import com.core.entity.Entity;
import com.core.entity.GameObject;

/**
 * PlaceTool — allows creating new entities in the viewport with optional grid snapping.
 *
 * <p>Left-click to place a new entity at the click position. When grid snapping is active,
 * entities are created at grid-aligned positions.</p>
 */
public class PlaceTool implements EditorTool {
    private final EditorViewport viewport;
    private final GridTool gridTool;

    private int entityCounter = 0;

    public PlaceTool(EditorViewport viewport, GridTool gridTool) {
        this.viewport = viewport;
        this.gridTool = gridTool;
    }

    @Override
    public String getName() {
        return "PlaceTool";
    }

    @Override
    public void onActivate() {
        // No special activation needed
    }

    @Override
    public void onDeactivate() {
        // No cleanup needed
    }

    @Override
    public void onMousePressed(float x, float y) {
        // Snap to grid if active
        if (gridTool != null && gridTool.isGridActive()) {
            float[] snapped = gridTool.snapToGrid(x, y);
            x = snapped[0];
            y = snapped[1];
        }

        // Create a new entity at this position
        Entity<?> newEntity = createEntity(x, y);
        if (viewport.getScene() != null) {
            viewport.getScene().getEntities().add(newEntity);
        }
        viewport.setSelectedEntity(newEntity);
        viewport.repaint();
    }

    @Override
    public void onMouseReleased(float x, float y) {
        // No action on release
    }

    @Override
    public void onMouseDragged(float x, float y) {
        // No action while dragging
    }

    @Override
    public void onMouseMoved(float x, float y) {
        // No action while moving
    }

    private Entity<?> createEntity(float worldX, float worldY) {
        GameObject obj = new GameObject("object_" + (++entityCounter));
        obj.x = worldX;
        obj.y = worldY;
        obj.width = 50;
        obj.height = 50;
        obj.setActive(true);
        return obj;
    }
}
