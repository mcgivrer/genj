package com.core.editor.tools;

/**
 * GridTool — manages grid state and snap-to-grid functionality.
 *
 * <p>Handles grid visibility, size cycling, and snapping coordinates to the nearest grid node.
 * Grid sizes cycle: 4 → 8 → 12 → 16 → 20 → 24 → 28 → 32 → 4…</p>
 */
public class GridTool {
    private static final int[] GRID_SIZES = {4, 8, 12, 16, 20, 24, 28, 32};

    private boolean gridActive = true;
    private int currentGridSizeIndex = 3; // Start at 16
    private int gridSize = GRID_SIZES[currentGridSizeIndex];

    public GridTool() {
        // Start with 16px grid and active
    }

    /**
     * Toggles the grid visibility on/off.
     */
    public void toggleGrid() {
        gridActive = !gridActive;
    }

    /**
     * Cycles to the next grid size in the predefined sequence.
     */
    public void cycleGridSize() {
        currentGridSizeIndex = (currentGridSizeIndex + 1) % GRID_SIZES.length;
        gridSize = GRID_SIZES[currentGridSizeIndex];
    }

    /**
     * Snaps a coordinate to the nearest grid node if grid is active.
     *
     * @param value the world coordinate (x or y)
     * @return snapped coordinate, or original if grid is not active
     */
    public float snapToGrid(float value) {
        if (!gridActive) {
            return value;
        }
        return Math.round(value / gridSize) * gridSize;
    }

    /**
     * Snaps a point (x, y) to the nearest grid node if grid is active.
     *
     * @param x world x coordinate
     * @param y world y coordinate
     * @return float[] {snappedX, snappedY}
     */
    public float[] snapToGrid(float x, float y) {
        return new float[]{snapToGrid(x), snapToGrid(y)};
    }

    /**
     * Returns the current grid size in pixels.
     */
    public int getGridSize() {
        return gridSize;
    }

    /**
     * Returns whether the grid is currently active (visible and snapping).
     */
    public boolean isGridActive() {
        return gridActive;
    }

    /**
     * Sets grid active state explicitly.
     */
    public void setGridActive(boolean active) {
        gridActive = active;
    }

    /**
     * Sets grid size explicitly (must be one of the predefined sizes).
     */
    public void setGridSize(int size) {
        for (int i = 0; i < GRID_SIZES.length; i++) {
            if (GRID_SIZES[i] == size) {
                currentGridSizeIndex = i;
                gridSize = size;
                return;
            }
        }
        // If size not found, keep current
    }

    /**
     * Finds the nearest grid point to the given coordinates.
     * Used for visual feedback (snap indicator).
     *
     * @param x world x coordinate
     * @param y world y coordinate
     * @return float[] {nearestX, nearestY}
     */
    public float[] getNearestGridPoint(float x, float y) {
        if (!gridActive) {
            return new float[]{x, y};
        }
        return snapToGrid(x, y);
    }
}
