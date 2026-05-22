package com.core.editor.tools;

/**
 * Base interface for editor tools.
 *
 * <p>Tools handle specific types of interactions in the editor viewport:
 * selection, dragging, placement, etc.</p>
 */
public interface EditorTool {

    /**
     * Returns the name of this tool (e.g., "SelectTool", "DragTool").
     */
    String getName();

    /**
     * Called when the tool is activated.
     */
    void onActivate();

    /**
     * Called when the tool is deactivated.
     */
    void onDeactivate();

    /**
     * Handle mouse press in the viewport.
     *
     * @param x world x coordinate
     * @param y world y coordinate
     */
    void onMousePressed(float x, float y);

    /**
     * Handle mouse release in the viewport.
     *
     * @param x world x coordinate
     * @param y world y coordinate
     */
    void onMouseReleased(float x, float y);

    /**
     * Handle mouse dragged in the viewport.
     *
     * @param x world x coordinate
     * @param y world y coordinate
     */
    void onMouseDragged(float x, float y);

    /**
     * Handle mouse moved (without pressing) in the viewport.
     *
     * @param x world x coordinate
     * @param y world y coordinate
     */
    void onMouseMoved(float x, float y);
}
