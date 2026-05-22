package com.core.editor.viewport;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import com.core.editor.tools.GridTool;

/**
 * Renders editor overlay elements: grid, selection boxes, entity labels, etc.
 *
 * <p>All coordinates are drawn in world-space via the EditorCamera transform.</p>
 */
public class EditorOverlay {
    private GridTool gridTool;
    private Color gridColor = new Color(100, 100, 100, 64);
    private Color gridCenterColor = new Color(150, 150, 150, 128);
    private Color snapIndicatorColor = new Color(0, 255, 0, 128);

    public EditorOverlay() {
    }

    public void setGridTool(GridTool gridTool) {
        this.gridTool = gridTool;
    }

    /**
     * Toggles grid visibility.
     */
    public void toggleGrid() {
        if (gridTool != null) {
            gridTool.toggleGrid();
        }
    }

    public boolean isGridVisible() {
        return gridTool != null && gridTool.isGridActive();
    }

    /**
     * Renders the grid overlay.
     *
     * @param g       graphics context
     * @param camera  editor camera for coordinate transforms
     */
    public void renderGrid(Graphics2D g, EditorCamera camera) {
        if (gridTool == null || !gridTool.isGridActive()) return;

        int vw = camera.getViewportWidth();
        int vh = camera.getViewportHeight();
        int gridSize = gridTool.getGridSize();

        // Calculate world-space bounds visible in viewport
        float worldMinX = camera.screenToWorldX(0);
        float worldMinY = camera.screenToWorldY(0);
        float worldMaxX = camera.screenToWorldX(vw);
        float worldMaxY = camera.screenToWorldY(vh);

        // Align grid to multiples of gridSize
        int startGridX = (int) Math.floor(worldMinX / gridSize) * gridSize;
        int startGridY = (int) Math.floor(worldMinY / gridSize) * gridSize;

        g.setColor(gridColor);
        g.setStroke(new BasicStroke(1.0f));

        // Draw vertical grid lines
        for (int gx = startGridX; gx <= worldMaxX; gx += gridSize) {
            float sx = camera.worldToScreenX(gx);
            if (sx >= 0 && sx <= vw) {
                if (gx == 0) {
                    g.setColor(gridCenterColor);
                    g.setStroke(new BasicStroke(2.0f));
                } else {
                    g.setColor(gridColor);
                    g.setStroke(new BasicStroke(1.0f));
                }
                g.drawLine((int) sx, 0, (int) sx, vh);
            }
        }

        // Draw horizontal grid lines
        for (int gy = startGridY; gy <= worldMaxY; gy += gridSize) {
            float sy = camera.worldToScreenY(gy);
            if (sy >= 0 && sy <= vh) {
                if (gy == 0) {
                    g.setColor(gridCenterColor);
                    g.setStroke(new BasicStroke(2.0f));
                } else {
                    g.setColor(gridColor);
                    g.setStroke(new BasicStroke(1.0f));
                }
                g.drawLine(0, (int) sy, vw, (int) sy);
            }
        }
    }

    /**
     * Renders a selection box around an entity.
     *
     * @param g       graphics context
     * @param camera  editor camera
     * @param wx      world-space left edge
     * @param wy      world-space top edge
     * @param ww      world-space width
     * @param wh      world-space height
     */
    public void renderSelectionBox(Graphics2D g, EditorCamera camera, float wx, float wy, float ww, float wh) {
        float sx = camera.worldToScreenX(wx);
        float sy = camera.worldToScreenY(wy);
        float sw = ww * camera.getZoom();
        float sh = wh * camera.getZoom();

        g.setColor(new Color(0, 255, 0, 200));
        g.setStroke(new BasicStroke(2.0f));
        g.drawRect((int) sx, (int) sy, (int) sw, (int) sh);

        // Draw resize handles (small squares at corners)
        int handleSize = 8;
        Color handleColor = new Color(0, 255, 0, 255);
        drawHandle(g, sx, sy, handleSize, handleColor);
        drawHandle(g, sx + sw, sy, handleSize, handleColor);
        drawHandle(g, sx, sy + sh, handleSize, handleColor);
        drawHandle(g, sx + sw, sy + sh, handleSize, handleColor);
    }

    private void drawHandle(Graphics2D g, float x, float y, int size, Color color) {
        g.setColor(color);
        g.fillRect((int) (x - size / 2.0f), (int) (y - size / 2.0f), size, size);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.0f));
        g.drawRect((int) (x - size / 2.0f), (int) (y - size / 2.0f), size, size);
    }

    /**
     * Renders a label at world-space coordinates.
     *
     * @param g       graphics context
     * @param camera  editor camera
     * @param text    label text
     * @param wx      world-space x
     * @param wy      world-space y
     */
    public void renderLabel(Graphics2D g, EditorCamera camera, String text, float wx, float wy) {
        float sx = camera.worldToScreenX(wx);
        float sy = camera.worldToScreenY(wy);

        g.setColor(Color.BLACK);
        g.drawString(text, (int) sx + 2, (int) sy - 2);
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString(text, (int) sx, (int) sy - 4);
    }
}
